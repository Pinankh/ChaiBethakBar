package com.pinankh.chaibethakbar.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.pinankh.chaibethakbar.model.MenuItem
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.model.TableModel
import com.pinankh.chaibethakbar.model.Category
import com.pinankh.chaibethakbar.model.InventoryItem
import com.pinankh.chaibethakbar.model.Supplier
import com.google.firebase.firestore.Query
import com.pinankh.chaibethakbar.util.TaxCalculator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class PosRepository private constructor(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        @Volatile
        private var instance: PosRepository? = null

        fun getInstance(): PosRepository {
            return instance ?: synchronized(this) {
                instance ?: PosRepository().also { repo ->
                    repo.configureFirestore()
                    instance = repo
                }
            }
        }
    }

    private fun configureFirestore() {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
        } catch (_: Exception) {
            // Already configured or initialized
        }
    }

    // ------------------------------------------------------------------
    // Real-time Flow Listeners
    // ------------------------------------------------------------------

    fun getOutletConfigFlow(): Flow<OutletConfig?> = callbackFlow {
        val listener = db.collection("outlet_config").document("default")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val config = snapshot?.toObject(OutletConfig::class.java)
                trySend(config)
            }
        awaitClose { listener.remove() }
    }

    fun getTablesFlow(): Flow<List<TableModel>> = callbackFlow {
        val listener = db.collection("tables")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tables = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TableModel::class.java)
                } ?: emptyList()
                trySend(tables)
            }
        awaitClose { listener.remove() }
    }

    fun getMenuFlow(): Flow<List<MenuItem>> = callbackFlow {
        val listener = db.collection("menu_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MenuItem::class.java)
                }?.sortedBy { it.sortOrder } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun getActiveOrderFlow(orderId: String): Flow<PosOrder?> = callbackFlow {
        if (orderId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val order = snapshot?.toObject(PosOrder::class.java)
                trySend(order)
            }
        awaitClose { listener.remove() }
    }

    fun getActiveTakeawayOrdersFlow(): Flow<List<PosOrder>> = callbackFlow {
        val listener = db.collection("orders")
            .whereEqualTo("orderType", "TAKEAWAY")
            .whereIn("status", listOf("OPEN", "PAID"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PosOrder::class.java)
                }?.filter { order ->
                    order.status == "OPEN" || (order.status == "PAID" && order.handoverStatus == "PREPARING")
                }?.sortedBy { it.tokenNo } ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    fun getDailyOrdersFlow(dateString: String): Flow<List<PosOrder>> = callbackFlow {
        val listener = db.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PosOrder::class.java)
                }?.filter { order ->
                    if (dateString.isBlank()) true
                    else {
                        val timestamp = order.closedAt ?: order.createdAt
                        if (timestamp != null) {
                            val istDate = Instant.ofEpochSecond(timestamp.seconds)
                                .atZone(ZoneId.of("Asia/Kolkata"))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            istDate == dateString
                        } else {
                            val todayIST = LocalDate.now(ZoneId.of("Asia/Kolkata"))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            todayIST == dateString
                        }
                    }
                }?.sortedByDescending { (it.closedAt ?: it.createdAt)?.seconds ?: Long.MAX_VALUE } ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    // ------------------------------------------------------------------
    // Offline-Safe WriteBatch Operations (No Transactions, No Deletions)
    // ------------------------------------------------------------------

    suspend fun openTable(tableId: String, tableLabel: String): String {
        val newOrderRef = db.collection("orders").document()
        val newOrderId = newOrderRef.id

        val newOrder = PosOrder(
            id = newOrderId,
            orderType = "DINE_IN",
            tokenNo = 0,
            customerName = "",
            billNo = "",
            tableId = tableId,
            tableLabel = tableLabel,
            status = "OPEN",
            handoverStatus = "",
            items = emptyList(),
            subtotalPaise = 0L,
            parcelChargePaise = 0L,
            discountPaise = 0L,
            discountReason = "",
            cgstPaise = 0L,
            sgstPaise = 0L,
            vatPaise = 0L,
            roundOffPaise = 0L,
            totalPaise = 0L,
            paymentMode = "",
            cancelReason = "",
            createdAt = Timestamp.now()
        )

        val batch = db.batch()
        batch.set(newOrderRef, newOrder)

        val tableRef = db.collection("tables").document(tableId)
        batch.update(
            tableRef, mapOf(
                "status" to "OCCUPIED",
                "activeOrderId" to newOrderId,
                "activeTotalPaise" to 0L,
                "openedAt" to Timestamp.now()
            )
        )

        batch.commit().await()
        return newOrderId
    }

    suspend fun createTakeawayOrder(
        customerName: String = "",
        parcelChargePaise: Long = 0L
    ): String {
        val todayIST = LocalDate.now(ZoneId.of("Asia/Kolkata"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val configRef = db.collection("outlet_config").document("default")
        val configDoc = try {
            configRef.get().await().toObject(OutletConfig::class.java)
        } catch (_: Exception) {
            null
        }

        val lastDate = configDoc?.dailyTokenDate ?: ""
        val lastToken = configDoc?.lastTokenNumber ?: 0

        val newTokenNo = if (lastDate == todayIST) lastToken + 1 else 1
        val tokenLabel = String.format(Locale.US, "Parcel #%02d", newTokenNo)

        val newOrderRef = db.collection("orders").document()
        val newOrderId = newOrderRef.id

        val newOrder = PosOrder(
            id = newOrderId,
            orderType = "TAKEAWAY",
            tokenNo = newTokenNo,
            customerName = customerName,
            billNo = "",
            tableId = "",
            tableLabel = tokenLabel,
            status = "OPEN",
            handoverStatus = "PREPARING",
            items = emptyList(),
            subtotalPaise = 0L,
            parcelChargePaise = parcelChargePaise,
            discountPaise = 0L,
            discountReason = "",
            cgstPaise = 0L,
            sgstPaise = 0L,
            vatPaise = 0L,
            roundOffPaise = 0L,
            totalPaise = parcelChargePaise,
            paymentMode = "",
            cancelReason = "",
            createdAt = Timestamp.now()
        )

        val batch = db.batch()

        batch.set(
            configRef, mapOf(
                "dailyTokenDate" to todayIST,
                "lastTokenNumber" to newTokenNo
            ), SetOptions.merge()
        )

        batch.set(newOrderRef, newOrder)
        batch.commit().await()
        return newOrderId
    }

    suspend fun updateOrder(order: PosOrder) {
        val calc = TaxCalculator.calculate(order.items, order.parcelChargePaise, order.discountPaise)
        val updatedOrder = order.copy(
            subtotalPaise = calc.subtotalPaise,
            parcelChargePaise = calc.parcelChargePaise,
            discountPaise = calc.discountPaise,
            cgstPaise = calc.cgstPaise,
            sgstPaise = calc.sgstPaise,
            vatPaise = calc.vatPaise,
            roundOffPaise = calc.roundOffPaise,
            totalPaise = calc.totalPaise
        )

        val batch = db.batch()
        val orderRef = db.collection("orders").document(order.id)
        batch.set(orderRef, updatedOrder)

        if (order.orderType == "DINE_IN" && order.tableId.isNotEmpty()) {
            val tableRef = db.collection("tables").document(order.tableId)
            batch.update(tableRef, "activeTotalPaise", calc.totalPaise)
        }

        batch.commit().await()
    }

    suspend fun markSentToKitchen(orderId: String) {
        val orderRef = db.collection("orders").document(orderId)
        val doc = orderRef.get().await()
        val order = doc.toObject(PosOrder::class.java) ?: return
        val updatedItems = order.items.map { it.copy(sentQty = it.qty) }

        val batch = db.batch()
        batch.update(orderRef, "items", updatedItems)
        batch.commit().await()
    }

    suspend fun markTableBilled(tableId: String, orderId: String = "") {
        val batch = db.batch()
        val tableRef = db.collection("tables").document(tableId)
        batch.update(tableRef, "status", "BILLED")
        batch.commit().await()
    }

    suspend fun shiftTable(fromTableId: String, toTableId: String, orderId: String) {
        val fromTableRef = db.collection("tables").document(fromTableId)
        val toTableRef = db.collection("tables").document(toTableId)
        val orderRef = db.collection("orders").document(orderId)

        val fromTableDoc = fromTableRef.get().await()
        val toTableDoc = toTableRef.get().await()

        val fromTable = fromTableDoc.toObject(TableModel::class.java)
        val toTable = toTableDoc.toObject(TableModel::class.java) ?: return

        val batch = db.batch()

        // Clear source table
        batch.update(
            fromTableRef, mapOf(
                "status" to "AVAILABLE",
                "activeOrderId" to "",
                "activeTotalPaise" to 0L,
                "openedAt" to null
            )
        )

        // Occupy target table
        batch.update(
            toTableRef, mapOf(
                "status" to "OCCUPIED",
                "activeOrderId" to orderId,
                "activeTotalPaise" to (fromTable?.activeTotalPaise ?: 0L),
                "openedAt" to (fromTable?.openedAt ?: Timestamp.now())
            )
        )

        // Update order with new table ID & label
        batch.update(
            orderRef, mapOf(
                "tableId" to toTableId,
                "tableLabel" to toTable.label
            )
        )

        batch.commit().await()
    }

    suspend fun settleOrder(
        orderId: String,
        paymentMode: String,
        isHandedOver: Boolean = false
    ) {
        require(paymentMode == "ONLINE" || paymentMode == "CASH") {
            "Payment mode must strictly be ONLINE or CASH"
        }

        val configRef = db.collection("outlet_config").document("default")
        val configDoc = try {
            configRef.get().await().toObject(OutletConfig::class.java)
        } catch (_: Exception) {
            null
        }

        val prefix = configDoc?.billPrefix?.takeIf { it.isNotBlank() } ?: "CBB"
        val nowIst = Instant.now().atZone(ZoneId.of("Asia/Kolkata"))
        val dateStr = nowIst.format(DateTimeFormatter.ofPattern("yyMMdd"))
        val timeStr = nowIst.format(DateTimeFormatter.ofPattern("HHmmss"))
        val shortId = UUID.randomUUID().toString().take(4).uppercase(Locale.US)
        val generatedBillNo = "$prefix-$dateStr-$timeStr-$shortId"

        val orderRef = db.collection("orders").document(orderId)
        val orderDoc = orderRef.get().await()
        val order = orderDoc.toObject(PosOrder::class.java) ?: return

        if (order.status == "PAID") {
            throw IllegalStateException("Order '${order.id}' is already settled.")
        }
        if (order.status == "CANCELLED") {
            throw IllegalStateException("Cannot settle a cancelled order.")
        }

        val batch = db.batch()

        val orderUpdates = mutableMapOf<String, Any>(
            "status" to "PAID",
            "paymentMode" to paymentMode,
            "billNo" to generatedBillNo,
            "closedAt" to Timestamp.now()
        )

        if (order.orderType == "TAKEAWAY" && isHandedOver) {
            orderUpdates["handoverStatus"] = "HANDED_OVER"
        }

        batch.update(orderRef, orderUpdates)

        if (order.orderType == "DINE_IN" && order.tableId.isNotEmpty()) {
            val tableRef = db.collection("tables").document(order.tableId)
            batch.update(
                tableRef, mapOf(
                    "status" to "AVAILABLE",
                    "activeOrderId" to "",
                    "activeTotalPaise" to 0L,
                    "openedAt" to null
                )
            )
        }

        batch.commit().await()
    }

    suspend fun markParcelHandedOver(orderId: String) {
        val batch = db.batch()
        val orderRef = db.collection("orders").document(orderId)
        batch.update(orderRef, "handoverStatus", "HANDED_OVER")
        batch.commit().await()
    }

    suspend fun cancelOrder(orderId: String, cancelReason: String) {
        val orderRef = db.collection("orders").document(orderId)
        val orderDoc = orderRef.get().await()
        val order = orderDoc.toObject(PosOrder::class.java) ?: return

        val batch = db.batch()

        batch.update(
            orderRef, mapOf(
                "status" to "CANCELLED",
                "cancelReason" to cancelReason,
                "closedAt" to Timestamp.now()
            )
        )

        if (order.tableId.isNotEmpty()) {
            val tableRef = db.collection("tables").document(order.tableId)
            batch.update(
                tableRef, mapOf(
                    "status" to "AVAILABLE",
                    "activeOrderId" to "",
                    "activeTotalPaise" to 0L,
                    "openedAt" to null
                )
            )
        }

        batch.commit().await()
    }

    suspend fun seedDemoOutlet() {
        val batch = db.batch()

        // 1. Outlet Config
        val configRef = db.collection("outlet_config").document("default")
        val config = OutletConfig(
            id = "default",
            outletName = "Chai Bathak Bar",
            addressLine1 = "12, Main Street, Near City Park",
            cityPincode = "Ahmedabad, 380015",
            phone = "+91 98765 43210",
            gstin = "24AAAAA0000A1Z5",
            fssaiNo = "10020021000123",
            billPrefix = "CBB",
            defaultParcelChargePaise = 1000L,
            dailyTokenDate = "",
            lastTokenNumber = 0
        )
        batch.set(configRef, config, SetOptions.merge())

        // 2. Demo Tables
        val demoTables = listOf(
            TableModel("T1", "Table 1", "Indoor", 4, "AVAILABLE"),
            TableModel("T2", "Table 2", "Indoor", 2, "AVAILABLE"),
            TableModel("T3", "Table 3", "Indoor", 4, "AVAILABLE"),
            TableModel("T4", "Table 4", "Patio", 4, "AVAILABLE"),
            TableModel("T5", "Table 5", "Patio", 6, "AVAILABLE"),
            TableModel("B1", "Bar 1", "Counter", 2, "AVAILABLE")
        )
        for (t in demoTables) {
            val tRef = db.collection("tables").document(t.id)
            batch.set(tRef, t, SetOptions.merge())
        }

        // 3. Demo Menu Items
        val demoItems = listOf(
            MenuItem("M1", "Kulhad Masala Chai", "Chai", 4000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 1),
            MenuItem("M2", "Ginger Tea", "Chai", 3000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 2),
            MenuItem("M3", "Cold Coffee", "Coffee", 9000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 3),
            MenuItem("M4", "Cappuccino", "Coffee", 11000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 4),
            MenuItem("M5", "Bun Maska", "Snacks", 5000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 5),
            MenuItem("M6", "Samosa Pav", "Snacks", 4000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 6),
            MenuItem("M7", "Cheese Grill Sandwich", "Snacks", 12000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 7),
            MenuItem("M8", "Paneer Wrap", "Snacks", 14000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 8),
            MenuItem("M9", "Virgin Mojito", "Mocktails", 10000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 9),
            MenuItem("M10", "Blue Lagoon", "Mocktails", 11000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 10),
            MenuItem("M11", "Chai + Bun Maska Combo", "Combos", 8000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 11),
            MenuItem("M12", "Coffee + Sandwich Combo", "Combos", 20000L, "GST", 5, isVeg = true, inStock = true, sortOrder = 12)
        )
        for (m in demoItems) {
            val mRef = db.collection("menu_items").document(m.id)
            batch.set(mRef, m, SetOptions.merge())
        }

        batch.commit().await()
    }

    // ------------------------------------------------------------------
    // Admin & Management Operations
    // ------------------------------------------------------------------

    suspend fun addMenuItem(item: MenuItem): String {
        val docRef = if (item.id.isNotBlank()) {
            db.collection("menu_items").document(item.id)
        } else {
            db.collection("menu_items").document()
        }
        val newItem = item.copy(id = docRef.id)
        docRef.set(newItem).await()
        return docRef.id
    }

    suspend fun updateMenuItem(item: MenuItem) {
        require(item.id.isNotBlank()) { "Menu item ID cannot be blank for update" }
        val docRef = db.collection("menu_items").document(item.id)
        docRef.set(item, SetOptions.merge()).await()
    }

    suspend fun updateMenuItemStock(itemId: String, inStock: Boolean) {
        require(itemId.isNotBlank()) { "Menu item ID cannot be blank" }
        val docRef = db.collection("menu_items").document(itemId)
        docRef.update("inStock", inStock).await()
    }

    suspend fun addTable(table: TableModel): String {
        val docRef = if (table.id.isNotBlank()) {
            db.collection("tables").document(table.id)
        } else {
            db.collection("tables").document()
        }
        val newTable = table.copy(
            id = docRef.id,
            status = "AVAILABLE",
            activeOrderId = "",
            activeTotalPaise = 0L,
            openedAt = null
        )
        docRef.set(newTable).await()
        return docRef.id
    }

    suspend fun updateTable(table: TableModel) {
        require(table.id.isNotBlank()) { "Table ID cannot be blank for update" }
        val docRef = db.collection("tables").document(table.id)
        val updates = mapOf(
            "label" to table.label,
            "zone" to table.zone,
            "capacity" to table.capacity
        )
        docRef.update(updates).await()
    }

    suspend fun deleteTable(tableId: String) {
        require(tableId.isNotBlank()) { "Table ID cannot be blank for deletion" }
        val docRef = db.collection("tables").document(tableId)
        val snapshot = docRef.get().await()
        val table = snapshot.toObject(TableModel::class.java)

        if (table != null) {
            if (table.status != "AVAILABLE" || table.activeOrderId.isNotBlank()) {
                throw IllegalStateException("Cannot delete table '${table.label}' while it is OCCUPIED, BILLED, or has an active order.")
            }
        }
        docRef.delete().await()
    }

    suspend fun updateOutletConfig(config: OutletConfig) {
        val docRef = db.collection("outlet_config").document("default")
        val updated = config.copy(id = "default")
        docRef.set(updated, SetOptions.merge()).await()
    }

    // --- Restored Phase 7 & 8 Methods ---

    fun observeMenuCategories(): Flow<List<Category>> = callbackFlow {
        val listener = db.collection("categories")
            .whereEqualTo("categoryType", "MENU")
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Category::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun observeInventoryCategories(): Flow<List<Category>> = callbackFlow {
        val listener = db.collection("categories")
            .whereEqualTo("categoryType", "INVENTORY")
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Category::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addCategory(category: Category): String {
        val docRef = db.collection("categories").document()
        docRef.set(category.copy(categoryId = docRef.id)).await()
        return docRef.id
    }

    suspend fun updateCategory(category: Category) {
        db.collection("categories").document(category.categoryId).set(category).await()
    }

    suspend fun deleteCategorySafely(categoryId: String) {
        db.collection("categories").document(categoryId).delete().await()
    }

    suspend fun setCategoryActive(categoryId: String, isActive: Boolean) {
        db.collection("categories").document(categoryId).update("isActive", isActive).await()
    }

    suspend fun migrateExistingMenuCategories() {
        // stub
    }

    fun observeInventoryItems(): Flow<List<InventoryItem>> = callbackFlow {
        val listener = db.collection("inventory_items")
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(snapshot?.toObjects(InventoryItem::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateInventoryItem(item: InventoryItem) {
        val docId = if (item.inventoryItemId.isEmpty()) db.collection("inventory_items").document().id else item.inventoryItemId
        db.collection("inventory_items").document(docId).set(item.copy(inventoryItemId = docId)).await()
    }

    suspend fun addInventoryItem(item: InventoryItem): String {
        val docRef = db.collection("inventory_items").document()
        docRef.set(item.copy(inventoryItemId = docRef.id)).await()
        return docRef.id
    }

    suspend fun setInventoryItemActive(itemId: String, isActive: Boolean) {
        db.collection("inventory_items").document(itemId).update("isActive", isActive).await()
    }

    // --- Phase 9: Supplier Management ---

    fun observeSuppliers(): Flow<List<Supplier>> = callbackFlow {
        val listener = db.collection("suppliers")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Supplier::class.java))
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeActiveSuppliers(): Flow<List<Supplier>> = callbackFlow {
        val listener = db.collection("suppliers")
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Supplier::class.java))
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeSupplier(supplierId: String): Flow<Supplier?> = callbackFlow {
        val listener = db.collection("suppliers").document(supplierId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(Supplier::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addSupplier(supplier: Supplier): String {
        val nameTrimmed = supplier.name.trim()
        if (nameTrimmed.isEmpty()) {
            throw IllegalArgumentException("Supplier name is required")
        }

        val existing = db.collection("suppliers")
            .whereEqualTo("isActive", true)
            .get()
            .await()
            .toObjects(Supplier::class.java)
        
        if (existing.any { it.name.trim().equals(nameTrimmed, ignoreCase = true) }) {
            throw IllegalArgumentException("An active supplier with this name already exists")
        }

        val docRef = db.collection("suppliers").document()
        val cleanSupplier = supplier.copy(
            supplierId = docRef.id,
            name = nameTrimmed,
            contactPerson = supplier.contactPerson.trim(),
            phone = supplier.phone.trim(),
            alternatePhone = supplier.alternatePhone.trim(),
            email = supplier.email.trim(),
            address = supplier.address.trim(),
            city = supplier.city.trim(),
            state = supplier.state.trim(),
            pincode = supplier.pincode.trim(),
            gstin = supplier.gstin.trim(),
            notes = supplier.notes.trim(),
            updatedAt = Timestamp.now()
        )

        docRef.set(cleanSupplier).await()
        return docRef.id
    }

    suspend fun updateSupplier(supplier: Supplier) {
        val nameTrimmed = supplier.name.trim()
        if (nameTrimmed.isEmpty()) {
            throw IllegalArgumentException("Supplier name is required")
        }

        if (supplier.isActive) {
            val existing = db.collection("suppliers")
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .toObjects(Supplier::class.java)

            if (existing.any { it.supplierId != supplier.supplierId && it.name.trim().equals(nameTrimmed, ignoreCase = true) }) {
                throw IllegalArgumentException("An active supplier with this name already exists")
            }
        }

        val cleanSupplier = supplier.copy(
            name = nameTrimmed,
            contactPerson = supplier.contactPerson.trim(),
            phone = supplier.phone.trim(),
            alternatePhone = supplier.alternatePhone.trim(),
            email = supplier.email.trim(),
            address = supplier.address.trim(),
            city = supplier.city.trim(),
            state = supplier.state.trim(),
            pincode = supplier.pincode.trim(),
            gstin = supplier.gstin.trim(),
            notes = supplier.notes.trim(),
            updatedAt = Timestamp.now()
        )
        
        db.collection("suppliers").document(supplier.supplierId).set(cleanSupplier).await()
    }

    suspend fun setSupplierActive(supplierId: String, isActive: Boolean) {
        db.collection("suppliers").document(supplierId)
            .update(
                "isActive", isActive,
                "updatedAt", Timestamp.now()
            ).await()
    }

    suspend fun searchSuppliers(query: String): List<Supplier> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return emptyList()

        return db.collection("suppliers")
            .get()
            .await()
            .toObjects(Supplier::class.java)
            .filter { supplier ->
                supplier.name.lowercase().contains(cleanQuery) ||
                supplier.contactPerson.lowercase().contains(cleanQuery) ||
                supplier.phone.contains(cleanQuery) ||
                supplier.gstin.lowercase().contains(cleanQuery)
            }
    }
}

