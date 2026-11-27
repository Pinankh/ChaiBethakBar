package com.pinankh.chaibethakbar.ui.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pinankh.chaibethakbar.model.MenuItem
import com.pinankh.chaibethakbar.model.OrderItem
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.model.TableModel
import com.pinankh.chaibethakbar.ui.components.DigitalBillDialog
import com.pinankh.chaibethakbar.ui.theme.*
import com.pinankh.chaibethakbar.util.toRupees
import java.time.Instant

@Composable
fun OrderWorkspaceScreen(
    orderId: String,
    onBackToHome: () -> Unit,
    viewModel: OrderViewModel = viewModel()
) {
    LaunchedEffect(orderId) {
        viewModel.setOrderId(orderId)
    }

    val activeOrder by viewModel.activeOrder.collectAsStateWithLifecycle()
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val menuCategories by viewModel.menuCategories.collectAsStateWithLifecycle()
    val outletConfig by viewModel.outletConfig.collectAsStateWithLifecycle()
    val availableTables by viewModel.availableTables.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isVegOnly by viewModel.isVegOnly.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    OrderWorkspaceScreen(
        activeOrder = activeOrder,
        menuItems = menuItems,
        menuCategories = menuCategories,
        outletConfig = outletConfig,
        availableTables = availableTables,
        selectedCategory = selectedCategory,
        searchQuery = searchQuery,
        isVegOnly = isVegOnly,
        isSaving = isSaving,
        errorMessage = errorMessage,
        onBackToHome = onBackToHome,
        onSelectCategory = { viewModel.setSelectedCategory(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onToggleVegOnly = { viewModel.setVegOnly(it) },
        onAddItem = { menuItem, note -> viewModel.addOrIncrementItem(menuItem, note) },
        onIncrementQty = { viewModel.incrementItemQuantity(it) },
        onDecrementQty = { viewModel.decrementItemQuantity(it) },
        onRemoveItem = { viewModel.removeItem(it) },
        onUpdateItemNote = { index, note -> viewModel.updateItemNote(index, note) },
        onUpdateCustomerName = { viewModel.updateCustomerName(it) },
        onToggleParcelCharge = { viewModel.toggleParcelCharge(it) },
        onSetParcelCharge = { viewModel.setParcelCharge(it) },
        onApplyDiscount = { paise, reason -> viewModel.applyDiscount(paise, reason) },
        onMarkSentToKitchen = { viewModel.markSentToKitchen() },
        onMarkTableBilled = { viewModel.markTableBilled() },
        onShiftTable = { viewModel.shiftTable(it) },
        onCancelOrder = { reason, callback -> viewModel.cancelOrder(reason, callback) },
        onConfirmSettle = { paymentMode, isHandedOver ->
            viewModel.settleOrder(paymentMode, isHandedOver) {
                onBackToHome()
            }
        },
        onClearError = { viewModel.clearError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderWorkspaceScreen(
    activeOrder: PosOrder?,
    menuItems: List<MenuItem>,
    menuCategories: List<com.pinankh.chaibethakbar.model.Category>,
    outletConfig: OutletConfig?,
    availableTables: List<TableModel>,
    selectedCategory: String,
    searchQuery: String,
    isVegOnly: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onBackToHome: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleVegOnly: (Boolean) -> Unit,
    onAddItem: (MenuItem, String?) -> Unit,
    onIncrementQty: (Int) -> Unit,
    onDecrementQty: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onUpdateItemNote: (Int, String) -> Unit,
    onUpdateCustomerName: (String) -> Unit,
    onToggleParcelCharge: (Boolean) -> Unit,
    onSetParcelCharge: (Long) -> Unit,
    onApplyDiscount: (Long, String) -> Unit,
    onMarkSentToKitchen: () -> Unit,
    onMarkTableBilled: () -> Unit,
    onShiftTable: (String) -> Unit,
    onCancelOrder: (String, () -> Unit) -> Unit,
    onConfirmSettle: (String, Boolean) -> Unit,
    onClearError: () -> Unit
) {
    // Dialog States
    var cookingNoteTarget by remember { mutableStateOf<Pair<MenuItem?, Int?>?>(null) } // MenuItem for new item, Int for existing cart index
    var initialNoteText by remember { mutableStateOf("") }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showShiftTableDialog by remember { mutableStateOf(false) }
    var showCancelOrderDialog by remember { mutableStateOf(false) }
    var showAdjustParcelChargeDialog by remember { mutableStateOf(false) }
    var showViewBillDialog by remember { mutableStateOf(false) }
    var showSettleBillDialog by remember { mutableStateOf(false) }

    val categories = remember(menuItems, menuCategories) {
        val activeCategories = menuCategories.filter { it.isActive }.map { it.name }.distinct()
        val baseCategories = listOf("All") + (activeCategories.ifEmpty { listOf("Chai", "Coffee", "Snacks", "Mocktails", "Combos") })
        val dynamicCategories = menuItems.map { it.category }.distinct().filter { it.isNotBlank() }
        (baseCategories + dynamicCategories).distinct()
    }

    if (activeOrder == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading Order Workspace...", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onBackToHome) {
                    Text("Return to Home")
                }
            }
        }
        return
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error banner if any
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = onClearError) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Main 3-Pane Tablet Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // ------------------------------------------------------------------
                // LEFT PANE: 18% Width — Categories & Home Navigation
                // ------------------------------------------------------------------
                Surface(
                    modifier = Modifier
                        .weight(0.18f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Home button that preserves current open order
                        OutlinedButton(
                            onClick = onBackToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Home", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(categories) { _, category ->
                                val isSelected = selectedCategory.equals(category, ignoreCase = true)
                                Surface(
                                    onClick = { onSelectCategory(category) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                VerticalDivider()

                // ------------------------------------------------------------------
                // CENTRE PANE: 47% Width — Search, Veg Filter, & Menu Grid
                // ------------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(0.47f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search + Veg Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search menu...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        FilterChip(
                            selected = isVegOnly,
                            onClick = { onToggleVegOnly(!isVegOnly) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    VegNonVegIndicator(isVeg = true)
                                    Text("Veg Only", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }

                    // Filtered Menu Items Grid
                    val filteredMenu = menuItems.filter { item ->
                        val matchesCategory = selectedCategory.equals("All", ignoreCase = true) ||
                                item.category.equals(selectedCategory, ignoreCase = true)
                        val matchesVeg = !isVegOnly || item.isVeg
                        val matchesSearch = searchQuery.isBlank() ||
                                item.name.contains(searchQuery, ignoreCase = true) ||
                                item.category.contains(searchQuery, ignoreCase = true)
                        matchesCategory && matchesVeg && matchesSearch
                    }.sortedBy { it.sortOrder }

                    if (filteredMenu.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No menu items found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(filteredMenu, key = { it.id }) { menuItem ->
                                MenuItemGridTile(
                                    item = menuItem,
                                    onClick = { onAddItem(menuItem, null) },
                                    onLongClick = {
                                        cookingNoteTarget = Pair(menuItem, null)
                                        val existingNote = activeOrder.items.find { it.menuItemId == menuItem.id }?.notes ?: ""
                                        initialNoteText = existingNote
                                    }
                                )
                            }
                        }
                    }
                }

                VerticalDivider()

                // ------------------------------------------------------------------
                // RIGHT PANE: 35% Width — Running Cart, Totals, & Pinned Actions
                // ------------------------------------------------------------------
                Surface(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Header Section: DINE_IN vs TAKEAWAY
                        if (activeOrder.orderType == "DINE_IN") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = activeOrder.tableLabel.ifBlank { "Dine-In Order" },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (activeOrder.status == "BILLED") {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = StatusBilledContainer
                                            ) {
                                                Text(
                                                    text = "BILLED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StatusOnBilledContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Dining time: ${formatElapsedTime(activeOrder.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OverflowMenuButton(
                                    onApplyDiscount = { showDiscountDialog = true },
                                    onShiftTable = { showShiftTableDialog = true },
                                    onCancelOrder = { showCancelOrderDialog = true },
                                    isDineIn = true
                                )
                            }
                        } else {
                            // TAKEAWAY Header
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = if (activeOrder.tokenNo > 0) String.format(java.util.Locale.US, "Parcel #%02d", activeOrder.tokenNo) else "Takeaway Parcel",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    OverflowMenuButton(
                                        onApplyDiscount = { showDiscountDialog = true },
                                        onShiftTable = {},
                                        onCancelOrder = { showCancelOrderDialog = true },
                                        isDineIn = false
                                    )
                                }

                                OutlinedTextField(
                                    value = activeOrder.customerName,
                                    onValueChange = onUpdateCustomerName,
                                    placeholder = { Text("Customer Name (Optional)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                // Parcel Packaging Charge Row
                                val defaultCharge = outletConfig?.defaultParcelChargePaise ?: 1000L
                                val isParcelChargeActive = activeOrder.parcelChargePaise > 0L

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Switch(
                                            checked = isParcelChargeActive,
                                            onCheckedChange = { onToggleParcelCharge(it) },
                                            modifier = Modifier.height(32.dp)
                                        )
                                        Text(
                                            text = "Parcel Packaging",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    TextButton(
                                        onClick = { showAdjustParcelChargeDialog = true },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (isParcelChargeActive) activeOrder.parcelChargePaise.toRupees() else "+${(if (defaultCharge > 0L) defaultCharge else 1000L).toRupees()}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(Icons.Default.Edit, contentDescription = "Edit charge", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Cart Items List
                        if (activeOrder.items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cart is empty.\nTap menu items to add.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(activeOrder.items) { index, item ->
                                    CartItemRow(
                                        item = item,
                                        onIncrement = { onIncrementQty(index) },
                                        onDecrement = { onDecrementQty(index) },
                                        onEditNote = {
                                            cookingNoteTarget = Pair(null, index)
                                            initialNoteText = item.notes
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Live Totals Breakdown
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TotalDetailRow(label = "Subtotal", amount = activeOrder.subtotalPaise.toRupees())

                            if (activeOrder.parcelChargePaise > 0L) {
                                TotalDetailRow(label = "Parcel Packaging", amount = "+${activeOrder.parcelChargePaise.toRupees()}")
                            }

                            if (activeOrder.discountPaise > 0L) {
                                val reasonText = if (activeOrder.discountReason.isNotBlank()) " (${activeOrder.discountReason})" else ""
                                TotalDetailRow(
                                    label = "Discount$reasonText",
                                    amount = "-${activeOrder.discountPaise.toRupees()}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (activeOrder.cgstPaise > 0L) {
                                TotalDetailRow(label = "CGST", amount = "+${activeOrder.cgstPaise.toRupees()}")
                            }
                            if (activeOrder.sgstPaise > 0L) {
                                TotalDetailRow(label = "SGST", amount = "+${activeOrder.sgstPaise.toRupees()}")
                            }
                            if (activeOrder.vatPaise > 0L) {
                                TotalDetailRow(label = "VAT", amount = "+${activeOrder.vatPaise.toRupees()}")
                            }

                            if (activeOrder.roundOffPaise != 0L) {
                                TotalDetailRow(label = "Round-Off", amount = activeOrder.roundOffPaise.toRupees())
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grand Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeOrder.totalPaise.toRupees(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pinned Bottom Action Buttons
                        val hasUnsentItems = activeOrder.items.any { it.qty > it.sentQty }

                        if (activeOrder.orderType == "DINE_IN") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onMarkSentToKitchen,
                                        enabled = hasUnsentItems && !isSaving,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Sent to Kitchen", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onMarkTableBilled()
                                            showViewBillDialog = true
                                        },
                                        enabled = !isSaving,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("View Bill", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Button(
                                    onClick = { showSettleBillDialog = true },
                                    enabled = !isSaving && activeOrder.items.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Settle Bill (${activeOrder.totalPaise.toRupees()})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // TAKEAWAY Pinned Actions
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onMarkSentToKitchen,
                                        enabled = hasUnsentItems && !isSaving,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Sent to Kitchen", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = onBackToHome,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Save Unpaid", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Button(
                                    onClick = { showSettleBillDialog = true },
                                    enabled = !isSaving && activeOrder.items.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Settle Parcel (${activeOrder.totalPaise.toRupees()})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // DIALOGS
    // ------------------------------------------------------------------

    // 1. Cooking Note Dialog
    if (cookingNoteTarget != null) {
        val targetPair = cookingNoteTarget!!
        val menuItem = targetPair.first
        val cartIndex = targetPair.second
        val titleText = menuItem?.name ?: activeOrder.items.getOrNull(cartIndex ?: -1)?.name ?: "Item"

        CookingNoteDialog(
            itemName = titleText,
            initialNote = initialNoteText,
            onDismiss = { cookingNoteTarget = null },
            onSave = { note ->
                cookingNoteTarget = null
                if (menuItem != null) {
                    onAddItem(menuItem, note)
                } else if (cartIndex != null) {
                    onUpdateItemNote(cartIndex, note)
                }
            }
        )
    }

    // 2. Apply Discount Dialog
    if (showDiscountDialog) {
        ApplyDiscountDialog(
            subtotalPaise = activeOrder.subtotalPaise + activeOrder.parcelChargePaise,
            currentDiscountPaise = activeOrder.discountPaise,
            currentReason = activeOrder.discountReason,
            onDismiss = { showDiscountDialog = false },
            onApply = { paise, reason ->
                showDiscountDialog = false
                onApplyDiscount(paise, reason)
            }
        )
    }

    // 3. Shift Table Dialog
    if (showShiftTableDialog) {
        AlertDialog(
            onDismissRequest = { showShiftTableDialog = false },
            title = { Text("Shift Table (${activeOrder.tableLabel})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select target available table:")
                    if (availableTables.isEmpty()) {
                        Text("No available tables found.", color = MaterialTheme.colorScheme.error)
                    } else {
                        availableTables.forEach { targetTable ->
                            OutlinedButton(
                                onClick = {
                                    showShiftTableDialog = false
                                    onShiftTable(targetTable.id)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${targetTable.label} (${targetTable.zone} • ${targetTable.capacity} seats)")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShiftTableDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Cancel Order / Parcel Dialog
    if (showCancelOrderDialog) {
        var cancelReasonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCancelOrderDialog = false },
            title = { Text(if (activeOrder.orderType == "DINE_IN") "Cancel Dine-In Order" else "Cancel Takeaway Parcel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide a reason for cancelling this order:")
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Cancellation Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = cancelReasonInput.trim().ifBlank { "Cancelled by owner" }
                        showCancelOrderDialog = false
                        onCancelOrder(reason, onBackToHome)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelOrderDialog = false }) {
                    Text("Keep Order")
                }
            }
        )
    }

    // 5. Adjust Parcel Charge Dialog
    if (showAdjustParcelChargeDialog) {
        val currentRupees = activeOrder.parcelChargePaise / 100
        var chargeRupeesInput by remember { mutableStateOf(currentRupees.toString()) }
        AlertDialog(
            onDismissRequest = { showAdjustParcelChargeDialog = false },
            title = { Text("Adjust Parcel Packaging Charge") },
            text = {
                OutlinedTextField(
                    value = chargeRupeesInput,
                    onValueChange = { chargeRupeesInput = it },
                    label = { Text("Packaging Charge (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rupees = chargeRupeesInput.toLongOrNull() ?: 0L
                        showAdjustParcelChargeDialog = false
                        onSetParcelCharge(rupees * 100L)
                    }
                ) {
                    Text("Save Charge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustParcelChargeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. View Digital Bill Dialog
    if (showViewBillDialog) {
        DigitalBillDialog(
            order = activeOrder,
            outletConfig = outletConfig,
            onDismiss = { showViewBillDialog = false }
        )
    }

    // 7. Settle Payment Dialog
    if (showSettleBillDialog) {
        SettleBillDialog(
            order = activeOrder,
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = { showSettleBillDialog = false },
            onConfirmSettle = { paymentMode, isHandedOver ->
                onConfirmSettle(paymentMode, isHandedOver)
            },
            onClearError = onClearError
        )
    }
}

// ------------------------------------------------------------------
// HELPER COMPOSABLES
// ------------------------------------------------------------------

private fun formatElapsedTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "0m"
    val diffSecs = Instant.now().epochSecond - timestamp.seconds
    val mins = maxOf(0L, diffSecs / 60)
    if (mins < 60) return "${mins}m"
    val hours = mins / 60
    val remMins = mins % 60
    return "${hours}h ${remMins}m"
}

@Composable
fun VegNonVegIndicator(isVeg: Boolean) {
    val color = if (isVeg) Color(0xFF16A34A) else Color(0xFFDC2626)
    Box(
        modifier = Modifier
            .size(16.dp)
            .border(1.5.dp, color, RoundedCornerShape(3.dp))
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuItemGridTile(
    item: MenuItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VegNonVegIndicator(isVeg = item.isVeg)
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = item.pricePaise.toRupees(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: OrderItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditNote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // On-screen Kitchen Status Badge
                    val unsentQty = item.qty - item.sentQty
                    if (unsentQty > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusOccupiedContainer
                        ) {
                            Text(
                                text = "+$unsentQty new",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusOnOccupiedContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (item.sentQty > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusAvailableContainer
                        ) {
                            Text(
                                text = "In kitchen",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusOnAvailableContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (item.notes.isNotBlank()) {
                    Surface(
                        onClick = onEditNote,
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "Note: ${item.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    TextButton(
                        onClick = onEditNote,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("+ Add Note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "${item.unitPricePaise.toRupees()} × ${item.qty} = ${(item.unitPricePaise * item.qty).toRupees()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantity controls (- qty +)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedIconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.qty == 1) Icons.Default.Delete else Icons.Default.Remove,
                        contentDescription = "Decrement",
                        modifier = Modifier.size(16.dp),
                        tint = if (item.qty == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = item.qty.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                OutlinedIconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun TotalDetailRow(label: String, amount: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun OverflowMenuButton(
    onApplyDiscount: () -> Unit,
    onShiftTable: () -> Unit,
    onCancelOrder: () -> Unit,
    isDineIn: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Apply Discount") },
                onClick = {
                    expanded = false
                    onApplyDiscount()
                },
                leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) }
            )

            if (isDineIn) {
                DropdownMenuItem(
                    text = { Text("Shift Table") },
                    onClick = {
                        expanded = false
                        onShiftTable()
                    },
                    leadingIcon = { Icon(Icons.Default.MoveUp, contentDescription = null) }
                )
            }

            DropdownMenuItem(
                text = { Text(if (isDineIn) "Cancel Order" else "Cancel Parcel") },
                onClick = {
                    expanded = false
                    onCancelOrder()
                },
                leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

// ------------------------------------------------------------------
// DIALOG COMPOSABLES
// ------------------------------------------------------------------

@Composable
fun CookingNoteDialog(
    itemName: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember { mutableStateOf(initialNote) }
    val quickChips = listOf("Less Sugar", "Extra Adrak", "No Ice", "Less Spicy", "Separate Chutney", "Extra Hot")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cooking Note ($itemName)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quick Note Chips:", style = MaterialTheme.typography.labelMedium)

                // Quick Chips Grid / Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val rows = quickChips.chunked(3)
                    rows.forEach { rowChips ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowChips.forEach { chip ->
                                FilterChip(
                                    selected = noteText.contains(chip, ignoreCase = true),
                                    onClick = {
                                        if (noteText.contains(chip, ignoreCase = true)) {
                                            // Remove chip text
                                            noteText = noteText.split(",")
                                                .map { it.trim() }
                                                .filter { !it.equals(chip, ignoreCase = true) }
                                                .joinToString(", ")
                                        } else {
                                            // Append chip text
                                            noteText = if (noteText.isBlank()) chip else "$noteText, $chip"
                                        }
                                    },
                                    label = { Text(chip, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.heightIn(min = 36.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Custom Cooking Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(noteText.trim()) }) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ApplyDiscountDialog(
    subtotalPaise: Long,
    currentDiscountPaise: Long,
    currentReason: String,
    onDismiss: () -> Unit,
    onApply: (Long, String) -> Unit
) {
    var isPercentage by remember { mutableStateOf(false) }
    var valueInput by remember { mutableStateOf(if (currentDiscountPaise > 0L) (currentDiscountPaise / 100).toString() else "") }
    var reasonInput by remember { mutableStateOf(currentReason) }

    val discountCalculatedPaise = remember(valueInput, isPercentage, subtotalPaise) {
        val num = valueInput.toDoubleOrNull() ?: 0.0
        if (num <= 0.0) 0L
        else if (isPercentage) {
            Math.round(subtotalPaise.toDouble() * num / 100.0)
        } else {
            Math.round(num * 100.0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply Order Discount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isPercentage,
                        onClick = { isPercentage = false },
                        label = { Text("Flat ₹ Amount") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isPercentage,
                        onClick = { isPercentage = true },
                        label = { Text("Percentage %") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    label = { Text(if (isPercentage) "Discount Percentage (%)" else "Discount Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Reason (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Discount to apply: ${discountCalculatedPaise.toRupees()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentDiscountPaise > 0L) {
                    OutlinedButton(
                        onClick = { onApply(0L, "") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove Discount")
                    }
                }
                Button(onClick = { onApply(discountCalculatedPaise, reasonInput.trim()) }) {
                    Text("Apply Discount")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun OrderWorkspaceScreenPreview() {
    ChaiBethakBarTheme {
        OrderWorkspaceScreen(
            activeOrder = PosOrder(
                id = "ord1",
                orderType = "DINE_IN",
                tableLabel = "Table 2",
                items = listOf(
                    OrderItem(menuItemId = "m1", name = "Kulhad Masala Chai", unitPricePaise = 4000L, qty = 2, sentQty = 1, notes = "Less Sugar"),
                    OrderItem(menuItemId = "m5", name = "Bun Maska", unitPricePaise = 5000L, qty = 1, sentQty = 1)
                ),
                subtotalPaise = 13000L,
                cgstPaise = 325L,
                sgstPaise = 325L,
                totalPaise = 13650L
            ),
            menuItems = listOf(
                MenuItem("m1", "Kulhad Masala Chai", "Chai", 4000L, isVeg = true),
                MenuItem("m2", "Ginger Tea", "Chai", 3000L, isVeg = true),
                MenuItem("m3", "Cold Coffee", "Coffee", 9000L, isVeg = true)
            ),
            menuCategories = emptyList(),
            outletConfig = OutletConfig(),
            availableTables = listOf(TableModel("t1", "Table 1", "Indoor", 4, "AVAILABLE")),
            selectedCategory = "All",
            searchQuery = "",
            isVegOnly = false,
            isSaving = false,
            errorMessage = null,
            onBackToHome = {},
            onSelectCategory = {},
            onSearchQueryChange = {},
            onToggleVegOnly = {},
            onAddItem = { _: MenuItem, _: String? -> },
            onIncrementQty = {},
            onDecrementQty = {},
            onRemoveItem = {},
            onUpdateItemNote = { _, _ -> },
            onUpdateCustomerName = {},
            onToggleParcelCharge = {},
            onSetParcelCharge = {},
            onApplyDiscount = { _, _ -> },
            onMarkSentToKitchen = {},
            onMarkTableBilled = {},
            onShiftTable = {},
            onCancelOrder = { _, _ -> },
            onConfirmSettle = { _, _ -> },
            onClearError = {}
        )
    }
}
