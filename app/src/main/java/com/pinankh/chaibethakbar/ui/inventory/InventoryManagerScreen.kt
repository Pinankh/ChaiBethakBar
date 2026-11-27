package com.pinankh.chaibethakbar.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pinankh.chaibethakbar.model.Category
import com.pinankh.chaibethakbar.model.InventoryItem
import com.pinankh.chaibethakbar.ui.theme.StatusAvailableGreen
import com.pinankh.chaibethakbar.ui.theme.StatusOccupiedAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryManagerScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val categories by viewModel.inventoryCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedItemType by viewModel.selectedItemType.collectAsStateWithLifecycle()
    val showActiveOnly by viewModel.showActiveOnly.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }

    var showDetailsDialog by remember { mutableStateOf<InventoryItem?>(null) }

    val filteredItems = items.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) || item.categoryName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || item.categoryName == selectedCategory
        val matchesType = selectedItemType == "All" || item.itemType == selectedItemType
        val matchesActive = !showActiveOnly || item.isActive
        matchesSearch && matchesCategory && matchesType && matchesActive
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            itemToEdit = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Inventory Item")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Messages
            if (errorMessage != null || successMessage != null) {
                val isError = errorMessage != null
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage ?: successMessage ?: "",
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("Search Items") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category:", style = MaterialTheme.typography.bodyMedium)
                    val catList = listOf("All") + categories.map { it.name }.distinct()
                    ScrollableDropdownMenu(
                        items = catList,
                        selectedItem = selectedCategory,
                        onItemSelected = { viewModel.setSelectedCategory(it) }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type:", style = MaterialTheme.typography.bodyMedium)
                    val typeList = listOf("All", "INGREDIENT", "RESALE_PRODUCT", "PACKAGING", "CONSUMABLE", "OTHER")
                    ScrollableDropdownMenu(
                        items = typeList,
                        selectedItem = selectedItemType,
                        onItemSelected = { viewModel.setSelectedItemType(it) }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = showActiveOnly,
                        onCheckedChange = { viewModel.setShowActiveOnly(it) }
                    )
                    Text("Active Only", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // List
            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No inventory items found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems) { item ->
                        InventoryItemCard(
                            item = item,
                            onEdit = {
                                itemToEdit = item
                                showAddEditDialog = true
                            },
                            onToggleActive = { viewModel.toggleInventoryItemActive(item.inventoryItemId, item.isActive) },
                            onShowDetails = { showDetailsDialog = item }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditInventoryItemDialog(
            item = itemToEdit,
            categories = categories,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedItem ->
                viewModel.saveInventoryItem(updatedItem, itemToEdit != null)
                showAddEditDialog = false
            }
        )
    }

    if (showDetailsDialog != null) {
        InventoryItemDetailsDialog(
            item = showDetailsDialog!!,
            onDismiss = { showDetailsDialog = null }
        )
    }
}

@Composable
fun ScrollableDropdownMenu(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedItem)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onShowDetails: () -> Unit
) {
    val isLowStock = item.currentStockBaseUnits <= item.minimumStockBaseUnits

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onShowDetails),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!item.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "INACTIVE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    if (isLowStock && item.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StatusOccupiedAmber
                        ) {
                            Text(
                                text = "LOW STOCK",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Text(
                    text = "${item.categoryName} • ${item.itemType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.currentStockBaseUnits} ${item.baseUnit}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Min: ${item.minimumStockBaseUnits} ${item.baseUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = item.isActive,
                    onCheckedChange = { onToggleActive() }
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    }
}

@Composable
fun AddEditInventoryItemDialog(
    item: InventoryItem?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(categories.find { it.categoryId == item?.categoryId } ?: categories.firstOrNull()) }
    var itemType by remember { mutableStateOf(item?.itemType ?: "INGREDIENT") }
    var baseUnit by remember { mutableStateOf(item?.baseUnit ?: "") }
    var purchaseUnit by remember { mutableStateOf(item?.purchaseUnit ?: "") }
    var conversionFactor by remember { mutableStateOf(item?.conversionFactor?.toString() ?: "1") }

    // Only editable for new items, although PRD states we should not silently overwrite.
    // So for edit, we might disable opening stock field or warn the user.
    var openingStockStr by remember { mutableStateOf(if(item == null) "" else "") }

    var minimumStockStr by remember { mutableStateOf(item?.minimumStockBaseUnits?.toString() ?: "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var isActive by remember { mutableStateOf(item?.isActive ?: true) }

    var showError by remember { mutableStateOf<String?>(null) }

    val isEdit = item != null

    val currentConversion = conversionFactor.toLongOrNull() ?: 1L
    val openingStockNum = openingStockStr.toLongOrNull() ?: 0L

    val baseQuantityCalculated = try {
        Math.multiplyExact(openingStockNum, currentConversion).toString()
    } catch (_: ArithmeticException) {
        "Too large (overflow)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Inventory Item" else "Add Inventory Item") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showError != null) {
                    Text(text = showError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                if (categories.isEmpty()) {
                    Text("No inventory categories found. Please create an INVENTORY category first.", color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category *", style = MaterialTheme.typography.labelMedium)
                        ScrollableDropdownMenu(
                            items = categories.map { it.name },
                            selectedItem = selectedCategory?.name ?: "",
                            onItemSelected = { catName -> selectedCategory = categories.find { it.name == catName } }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Item Type *", style = MaterialTheme.typography.labelMedium)
                        val types = listOf("INGREDIENT", "RESALE_PRODUCT", "PACKAGING", "CONSUMABLE", "OTHER")
                        ScrollableDropdownMenu(
                            items = types,
                            selectedItem = itemType,
                            onItemSelected = { itemType = it }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchaseUnit,
                        onValueChange = { purchaseUnit = it },
                        label = { Text("Purchase Unit * (e.g. KG, PACKET)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = baseUnit,
                        onValueChange = { baseUnit = it },
                        label = { Text("Base Unit * (e.g. GRAM, PIECE)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = conversionFactor,
                    onValueChange = { conversionFactor = it },
                    label = { Text("Conversion Factor *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (purchaseUnit.isNotBlank() && baseUnit.isNotBlank() && currentConversion > 0) {
                            Text("1 $purchaseUnit = $currentConversion $baseUnit")
                        } else {
                            Text("How many base units in one purchase unit? (Must be > 0)")
                        }
                    }
                )

                if (!isEdit) {
                    OutlinedTextField(
                        value = openingStockStr,
                        onValueChange = { openingStockStr = it },
                        label = { Text("Opening Stock (in $purchaseUnit) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            if (openingStockStr.isNotBlank() && baseUnit.isNotBlank() && currentConversion > 0) {
                                Text("Will be stored as $baseQuantityCalculated $baseUnit")
                            }
                        }
                    )
                }

                OutlinedTextField(
                    value = minimumStockStr,
                    onValueChange = { minimumStockStr = it },
                    label = { Text("Minimum Stock Threshold (in $baseUnit) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Active (Available for normal inventory operations)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cf = conversionFactor.toLongOrNull() ?: 0L
                    val minStock = minimumStockStr.toLongOrNull() ?: -1L
                    val openStockInput = openingStockStr.toLongOrNull() ?: -1L
                    
                    val openStock = if (isEdit) {
                        item!!.openingStockBaseUnits
                    } else {
                        try {
                            Math.multiplyExact(openStockInput, cf)
                        } catch (e: ArithmeticException) {
                            showError = "Opening stock value is too large and caused an overflow."
                            return@Button
                        }
                    }

                    if (name.isBlank()) {
                        showError = "Name is required."
                        return@Button
                    }
                    if (selectedCategory == null) {
                        showError = "Category is required."
                        return@Button
                    }
                    if (purchaseUnit.isBlank() || baseUnit.isBlank()) {
                        showError = "Purchase Unit and Base Unit are required."
                        return@Button
                    }
                    if (cf <= 0) {
                        showError = "Conversion factor must be a positive integer."
                        return@Button
                    }
                    if (minStock < 0) {
                        showError = "Minimum stock must be a non-negative integer."
                        return@Button
                    }
                    if (!isEdit && openingStockStr.isBlank()) {
                        showError = "Opening stock is required."
                        return@Button
                    }
                    if (!isEdit && (openingStockStr.toLongOrNull() ?: -1L) < 0) {
                        showError = "Opening stock cannot be negative."
                        return@Button
                    }

                    val updatedItem = InventoryItem(
                        inventoryItemId = item?.inventoryItemId ?: "",
                        name = name.trim(),
                        description = description.trim(),
                        categoryId = selectedCategory!!.categoryId,
                        categoryName = selectedCategory!!.name,
                        itemType = itemType,
                        baseUnit = baseUnit.trim(),
                        purchaseUnit = purchaseUnit.trim(),
                        conversionFactor = cf,
                        openingStockBaseUnits = openStock,
                        minimumStockBaseUnits = minStock,
                        currentStockBaseUnits = item?.currentStockBaseUnits ?: openStock,
                        isActive = isActive,
                        notes = notes.trim()
                    )
                    onSave(updatedItem)
                },
                enabled = categories.isNotEmpty()
            ) {
                Text(if (isEdit) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun InventoryItemDetailsDialog(
    item: InventoryItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.description.isNotBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                DetailRow("Category", item.categoryName)
                DetailRow("Type", item.itemType)
                DetailRow("Status", if (item.isActive) "ACTIVE" else "INACTIVE")

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Current Stock", "${item.currentStockBaseUnits} ${item.baseUnit}")
                DetailRow("Minimum Stock", "${item.minimumStockBaseUnits} ${item.baseUnit}")
                DetailRow("Opening Stock", "${item.openingStockBaseUnits} ${item.baseUnit}")

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Purchase Unit", item.purchaseUnit)
                DetailRow("Base Unit", item.baseUnit)
                DetailRow("Conversion", "1 ${item.purchaseUnit} = ${item.conversionFactor} ${item.baseUnit}")

                if (item.notes.isNotBlank()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Notes:", style = MaterialTheme.typography.labelMedium)
                    Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

