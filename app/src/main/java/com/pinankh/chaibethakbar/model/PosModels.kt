package com.pinankh.chaibethakbar.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class OutletConfig(
    @DocumentId val id: String = "default",
    val outletName: String = "Chai Bathak Bar",
    val addressLine1: String = "",
    val cityPincode: String = "",
    val phone: String = "",
    val gstin: String = "",
    val fssaiNo: String = "",
    val billPrefix: String = "CBB",
    val defaultParcelChargePaise: Long = 0L,
    val dailyTokenDate: String = "",
    val lastTokenNumber: Int = 0
)

data class TableModel(
    @DocumentId val id: String = "",
    val label: String = "",
    val zone: String = "",
    val capacity: Int = 0,
    val status: String = "AVAILABLE", // AVAILABLE, OCCUPIED, BILLED
    val activeOrderId: String = "",
    val activeTotalPaise: Long = 0L,
    val openedAt: Timestamp? = null
)

data class MenuItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "", // Chai, Coffee, Snacks, Mocktails, Combos
    val pricePaise: Long = 0L,
    val taxType: String = "NONE", // NONE, GST, VAT
    val taxPercent: Int = 0,
    val isVeg: Boolean = true,
    val inStock: Boolean = true,
    val sortOrder: Int = 0
)

data class OrderItem(
    val menuItemId: String = "",
    val name: String = "",
    val unitPricePaise: Long = 0L,
    val taxType: String = "NONE", // NONE, GST, VAT
    val taxPercent: Int = 0,
    val qty: Int = 0,
    val sentQty: Int = 0, // On-screen kitchen tracking
    val notes: String = ""
)

data class PosOrder(
    @DocumentId val id: String = "",
    val orderType: String = "DINE_IN", // DINE_IN or TAKEAWAY
    val tokenNo: Int = 0, // 0 for DINE_IN, 1, 2, 3... for TAKEAWAY
    val customerName: String = "",
    val billNo: String = "",
    val tableId: String = "",
    val tableLabel: String = "",
    val status: String = "OPEN", // OPEN, PAID, CANCELLED
    val handoverStatus: String = "", // PREPARING or HANDED_OVER for TAKEAWAY
    val items: List<OrderItem> = emptyList(),
    val subtotalPaise: Long = 0L,
    val parcelChargePaise: Long = 0L,
    val discountPaise: Long = 0L,
    val discountReason: String = "",
    val cgstPaise: Long = 0L,
    val sgstPaise: Long = 0L,
    val vatPaise: Long = 0L,
    val roundOffPaise: Long = 0L,
    val totalPaise: Long = 0L,
    val paymentMode: String = "", // Strictly ONLINE or CASH
    val cancelReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val closedAt: Timestamp? = null
)

data class Category(
    @DocumentId val categoryId: String = "",
    val name: String = "",
    val description: String = "",
    val categoryType: String = "MENU", // MENU or INVENTORY
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class InventoryItem(
    @DocumentId val inventoryItemId: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val itemType: String = "OTHER", // INGREDIENT, RESALE_PRODUCT, PACKAGING, CONSUMABLE, OTHER
    val baseUnit: String = "",
    val purchaseUnit: String = "",
    val conversionFactor: Long = 1L,
    val currentStockBaseUnits: Long = 0L,
    val minimumStockBaseUnits: Long = 0L,
    val openingStockBaseUnits: Long = 0L,
    val defaultPurchaseRatePaise: Long = 0L,
    val isActive: Boolean = true,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Supplier(
    @DocumentId val supplierId: String = "",
    val name: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val alternatePhone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val gstin: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
