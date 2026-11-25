package com.pinankh.chaibethakbar.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pinankh.chaibethakbar.model.MenuItem
import com.pinankh.chaibethakbar.model.TableModel
import com.pinankh.chaibethakbar.ui.theme.*
import com.pinankh.chaibethakbar.util.toRupees
import java.util.Locale

fun parseRupeesToPaise(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    return try {
        val amount = trimmed.toDouble()
        if (amount < 0.0) null
        else Math.round(amount * 100.0)
    } catch (_: Exception) {
        null
    }
}

fun Long.toRupeesInputString(): String {
    val rupees = this / 100
    val paise = this % 100
    return if (paise == 0L) "$rupees" else String.format(Locale.US, "%d.%02d", rupees, paise)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuAndTablesScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    MenuAndTablesScreenContent(
        menuItems = menuItems,
        tables = tables,
        searchQuery = searchQuery,
        selectedCategory = selectedCategory,
        isLoading = isLoading,
        errorMessage = errorMessage,
        successMessage = successMessage,
        onBack = onBack,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onCategorySelect = { viewModel.setSelectedCategory(it) },
        onToggleStock = { id, stock -> viewModel.toggleStock(id, stock) },
        onSaveMenuItem = { item, isEdit -> viewModel.saveMenuItem(item, isEdit) },
        onSaveTable = { table, isEdit -> viewModel.saveTable(table, isEdit) },
        onDeleteTable = { table -> viewModel.deleteTable(table) },
        onClearMessages = { viewModel.clearMessages() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuAndTablesScreenContent(
    menuItems: List<MenuItem>,
    tables: List<TableModel>,
    searchQuery: String,
    selectedCategory: String,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onToggleStock: (String, Boolean) -> Unit,
    onSaveMenuItem: (MenuItem, Boolean) -> Unit,
    onSaveTable: (TableModel, Boolean) -> Unit,
    onDeleteTable: (TableModel) -> Unit,
    onClearMessages: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu & Tables Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Status messages banner
            if (errorMessage != null || successMessage != null) {
                val isError = errorMessage != null
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage ?: successMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF166534)
                        )
                        IconButton(onClick = onClearMessages) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // Tab Navigation Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                            Text("Menu Manager (${menuItems.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TableBar, contentDescription = null)
                            Text("Table Manager (${tables.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> MenuManagerTab(
                        menuItems = menuItems,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        isLoading = isLoading,
                        onSearchQueryChange = onSearchQueryChange,
                        onCategorySelect = onCategorySelect,
                        onToggleStock = onToggleStock,
                        onSaveMenuItem = onSaveMenuItem
                    )
                    1 -> TableManagerTab(
                        tables = tables,
                        isLoading = isLoading,
                        onSaveTable = onSaveTable,
                        onDeleteTable = onDeleteTable
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// TAB A — MENU MANAGER
// ------------------------------------------------------------------

@Composable
fun MenuManagerTab(
    menuItems: List<MenuItem>,
    searchQuery: String,
    selectedCategory: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onToggleStock: (String, Boolean) -> Unit,
    onSaveMenuItem: (MenuItem, Boolean) -> Unit
) {
    var showItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MenuItem?>(null) }

    val defaultCategories = listOf("Chai", "Coffee", "Snacks", "Mocktails", "Combos")
    val allCategories = listOf("All") + (defaultCategories + menuItems.map { it.category }).distinct().filter { it.isNotBlank() }

    val filteredItems = menuItems.filter { item ->
        val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    val groupedItems = filteredItems.groupBy { it.category }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search menu items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    itemToEdit = null
                    showItemDialog = true
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Menu Item")
            }
        }

        // Category Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            allCategories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category) }
                )
            }
        }

        // Menu Items List grouped by Category
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No menu items match '$searchQuery'" else "No menu items found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedItems.forEach { (category, itemsInCategory) ->
                    item {
                        Text(
                            text = category.ifBlank { "Uncategorized" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(itemsInCategory, key = { it.id }) { item ->
                        MenuItemAdminCard(
                            item = item,
                            onToggleStock = { onToggleStock(item.id, item.inStock) },
                            onEdit = {
                                itemToEdit = item
                                showItemDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showItemDialog) {
        MenuItemDialog(
            item = itemToEdit,
            onDismiss = { showItemDialog = false },
            onSave = { updatedItem ->
                showItemDialog = false
                onSaveMenuItem(updatedItem, itemToEdit != null)
            }
        )
    }
}

@Composable
fun MenuItemAdminCard(
    item: MenuItem,
    onToggleStock: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Veg / Non-Veg Indicator Dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (item.isVeg) Color(0xFF16A34A) else Color(0xFFDC2626))
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.pricePaise.toRupees(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when (item.taxType.uppercase()) {
                                "GST" -> "GST ${item.taxPercent}%"
                                "VAT" -> "VAT ${item.taxPercent}%"
                                else -> "No Tax"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Sort: ${item.sortOrder}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Stock Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (item.inStock) "In Stock" else "Out of Stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.inStock) Color(0xFF16A34A) else Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = item.inStock,
                        onCheckedChange = { onToggleStock() }
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItemDialog(
    item: MenuItem?,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    val isEdit = item != null

    var nameInput by remember { mutableStateOf(item?.name ?: "") }
    var categoryInput by remember { mutableStateOf(item?.category ?: "Chai") }
    var priceInput by remember { mutableStateOf(item?.pricePaise?.toRupeesInputString() ?: "") }
    var taxTypeInput by remember { mutableStateOf(item?.taxType ?: "GST") }
    var taxPercentInput by remember { mutableStateOf(item?.taxPercent?.toString() ?: "5") }
    var isVegInput by remember { mutableStateOf(item?.isVeg ?: true) }
    var inStockInput by remember { mutableStateOf(item?.inStock ?: true) }
    var sortOrderInput by remember { mutableStateOf(item?.sortOrder?.toString() ?: "0") }

    var validationError by remember { mutableStateOf<String?>(null) }

    val categoryOptions = listOf("Chai", "Coffee", "Snacks", "Mocktails", "Combos")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Menu Item" else "Add Menu Item") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection
                Text("Category *", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    categoryOptions.forEach { cat ->
                        FilterChip(
                            selected = categoryInput.equals(cat, ignoreCase = true),
                            onClick = { categoryInput = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Price (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sortOrderInput,
                        onValueChange = { sortOrderInput = it },
                        label = { Text("Sort Order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Tax Type Selection
                Text("Tax Type *", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NONE", "GST", "VAT").forEach { type ->
                        FilterChip(
                            selected = taxTypeInput.equals(type, ignoreCase = true),
                            onClick = {
                                taxTypeInput = type
                                if (type == "NONE") taxPercentInput = "0"
                            },
                            label = { Text(type) }
                        )
                    }
                }

                if (taxTypeInput != "NONE") {
                    OutlinedTextField(
                        value = taxPercentInput,
                        onValueChange = { taxPercentInput = it },
                        label = { Text("Tax Percentage (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Type:")
                        FilterChip(
                            selected = isVegInput,
                            onClick = { isVegInput = true },
                            label = { Text("Veg") }
                        )
                        FilterChip(
                            selected = !isVegInput,
                            onClick = { isVegInput = false },
                            label = { Text("Non-Veg") }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("In Stock:")
                        Switch(
                            checked = inStockInput,
                            onCheckedChange = { inStockInput = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = nameInput.trim()
                    if (name.isBlank()) {
                        validationError = "Name cannot be blank"
                        return@Button
                    }
                    val pricePaise = parseRupeesToPaise(priceInput)
                    if (pricePaise == null) {
                        validationError = "Please enter a valid price in ₹"
                        return@Button
                    }
                    val taxPercent = taxPercentInput.toIntOrNull()
                    if (taxPercent == null || taxPercent < 0) {
                        validationError = "Please enter a valid tax percentage"
                        return@Button
                    }
                    val sortOrder = sortOrderInput.toIntOrNull() ?: 0

                    val resultItem = MenuItem(
                        id = item?.id ?: "",
                        name = name,
                        category = categoryInput.trim(),
                        pricePaise = pricePaise,
                        taxType = taxTypeInput,
                        taxPercent = taxPercent,
                        isVeg = isVegInput,
                        inStock = inStockInput,
                        sortOrder = sortOrder
                    )
                    onSave(resultItem)
                }
            ) {
                Text(if (isEdit) "Save Changes" else "Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ------------------------------------------------------------------
// TAB B — TABLE MANAGER
// ------------------------------------------------------------------

@Composable
fun TableManagerTab(
    tables: List<TableModel>,
    isLoading: Boolean,
    onSaveTable: (TableModel, Boolean) -> Unit,
    onDeleteTable: (TableModel) -> Unit
) {
    var showTableDialog by remember { mutableStateOf(false) }
    var tableToEdit by remember { mutableStateOf<TableModel?>(null) }
    var tableToDelete by remember { mutableStateOf<TableModel?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dine-In Floor Tables",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    tableToEdit = null
                    showTableDialog = true
                },
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Table")
            }
        }

        if (tables.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No tables configured. Tap '+ Add Table' to create one.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(tables, key = { it.id }) { table ->
                    TableAdminCard(
                        table = table,
                        onEdit = {
                            tableToEdit = table
                            showTableDialog = true
                        },
                        onDelete = {
                            tableToDelete = table
                        }
                    )
                }
            }
        }
    }

    if (showTableDialog) {
        TableDialog(
            table = tableToEdit,
            onDismiss = { showTableDialog = false },
            onSave = { resultTable ->
                showTableDialog = false
                onSaveTable(resultTable, tableToEdit != null)
            }
        )
    }

    if (tableToDelete != null) {
        AlertDialog(
            onDismissRequest = { tableToDelete = null },
            title = { Text("Delete Table") },
            text = { Text("Are you sure you want to delete '${tableToDelete?.label}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val t = tableToDelete!!
                        tableToDelete = null
                        onDeleteTable(t)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Table")
                }
            },
            dismissButton = {
                TextButton(onClick = { tableToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TableAdminCard(
    table: TableModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val canDelete = table.status == "AVAILABLE" && table.activeOrderId.isBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = table.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (table.status) {
                        "OCCUPIED" -> StatusOccupiedContainer
                        "BILLED" -> StatusBilledContainer
                        else -> StatusAvailableContainer
                    }
                ) {
                    Text(
                        text = table.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (table.status) {
                            "OCCUPIED" -> StatusOnOccupiedContainer
                            "BILLED" -> StatusOnBilledContainer
                            else -> StatusOnAvailableContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = table.zone,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${table.capacity} seats",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Table")
                }

                IconButton(
                    onClick = onDelete,
                    enabled = canDelete
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Table",
                        tint = if (canDelete) MaterialTheme.colorScheme.error else Color.Gray.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun TableDialog(
    table: TableModel?,
    onDismiss: () -> Unit,
    onSave: (TableModel) -> Unit
) {
    val isEdit = table != null

    var labelInput by remember { mutableStateOf(table?.label ?: "") }
    var zoneInput by remember { mutableStateOf(table?.zone ?: "Indoor") }
    var capacityInput by remember { mutableStateOf(table?.capacity?.toString() ?: "4") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val zoneOptions = listOf("Indoor", "Patio", "Counter")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Table" else "Add New Table") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Table Label * (e.g., Table 7)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Zone *", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    zoneOptions.forEach { zone ->
                        FilterChip(
                            selected = zoneInput.equals(zone, ignoreCase = true),
                            onClick = { zoneInput = zone },
                            label = { Text(zone) }
                        )
                    }
                }

                OutlinedTextField(
                    value = capacityInput,
                    onValueChange = { capacityInput = it },
                    label = { Text("Seat Capacity *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val label = labelInput.trim()
                    if (label.isBlank()) {
                        validationError = "Table label cannot be blank"
                        return@Button
                    }
                    val capacity = capacityInput.toIntOrNull()
                    if (capacity == null || capacity <= 0) {
                        validationError = "Please enter a valid seat capacity (> 0)"
                        return@Button
                    }

                    val resultTable = TableModel(
                        id = table?.id ?: "",
                        label = label,
                        zone = zoneInput.trim(),
                        capacity = capacity,
                        status = table?.status ?: "AVAILABLE",
                        activeOrderId = table?.activeOrderId ?: "",
                        activeTotalPaise = table?.activeTotalPaise ?: 0L,
                        openedAt = table?.openedAt
                    )
                    onSave(resultTable)
                }
            ) {
                Text(if (isEdit) "Save Table" else "Add Table")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MenuAndTablesScreenPreview() {
    ChaiBethakBarTheme {
        MenuAndTablesScreenContent(
            menuItems = listOf(
                MenuItem(id = "1", name = "Kulhad Masala Chai", category = "Chai", pricePaise = 4000L, inStock = true),
                MenuItem(id = "2", name = "Cold Coffee", category = "Coffee", pricePaise = 9000L, inStock = false)
            ),
            tables = listOf(
                TableModel(id = "t1", label = "Table 1", zone = "Indoor", capacity = 4, status = "AVAILABLE")
            ),
            searchQuery = "",
            selectedCategory = "All",
            isLoading = false,
            errorMessage = null,
            successMessage = null,
            onBack = {},
            onSearchQueryChange = {},
            onCategorySelect = {},
            onToggleStock = { _, _ -> },
            onSaveMenuItem = { _, _ -> },
            onSaveTable = { _, _ -> },
            onDeleteTable = {},
            onClearMessages = {}
        )
    }
}
