package com.pinankh.chaibethakbar.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pinankh.chaibethakbar.model.MenuItem
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.model.TableModel
import com.pinankh.chaibethakbar.ui.theme.*
import com.pinankh.chaibethakbar.util.toRupees
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosHomeScreen(
    viewModel: HomeViewModel,
    onOpenOrder: (String) -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateMenuTables: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val outletConfig by viewModel.outletConfig.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val menuItems by viewModel.menuItems.collectAsStateWithLifecycle()
    val activeTakeawayOrders by viewModel.activeTakeawayOrders.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedZone by viewModel.selectedZone.collectAsStateWithLifecycle()

    PosHomeScreen(
        outletConfig = outletConfig,
        tables = tables,
        menuItems = menuItems,
        activeTakeawayOrders = activeTakeawayOrders,
        isOnline = isOnline,
        isLoading = isLoading,
        errorMessage = errorMessage,
        selectedZone = selectedZone,
        onOpenOrder = onOpenOrder,
        onNavigateReports = onNavigateReports,
        onNavigateMenuTables = onNavigateMenuTables,
        onNavigateSettings = onNavigateSettings,
        onClearError = { viewModel.clearError() },
        onSeedDemoOutlet = { viewModel.seedDemoOutlet() },
        onMarkParcelHandedOver = { viewModel.markParcelHandedOver(it) },
        onSetSelectedZone = { viewModel.setSelectedZone(it) },
        onOpenTable = { table ->
            if (table.status == "AVAILABLE") {
                viewModel.openTable(table.id, table.label) { orderId ->
                    onOpenOrder(orderId)
                }
            } else {
                if (table.activeOrderId.isNotBlank()) {
                    onOpenOrder(table.activeOrderId)
                }
            }
        },
        onCreateTakeawayOrder = { name ->
            viewModel.createTakeawayOrder(name) { orderId ->
                onOpenOrder(orderId)
            }
        },
        onShiftTable = { source, targetTable ->
            viewModel.shiftTable(source.id, targetTable.id, source.activeOrderId)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosHomeScreen(
    outletConfig: OutletConfig?,
    tables: List<TableModel>,
    menuItems: List<MenuItem>,
    activeTakeawayOrders: List<PosOrder>,
    isOnline: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    selectedZone: String,
    onOpenOrder: (String) -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateMenuTables: () -> Unit,
    onNavigateSettings: () -> Unit,
    onClearError: () -> Unit,
    onSeedDemoOutlet: () -> Unit,
    onMarkParcelHandedOver: (String) -> Unit,
    onSetSelectedZone: (String) -> Unit,
    onOpenTable: (TableModel) -> Unit,
    onCreateTakeawayOrder: (String) -> Unit,
    onShiftTable: (TableModel, TableModel) -> Unit
) {
    var showNewParcelDialog by remember { mutableStateOf(false) }
    var customerNameInput by remember { mutableStateOf("") }
    var tableToShift by remember { mutableStateOf<TableModel?>(null) }

    val isFirstRunSetupNeeded = tables.isEmpty() && menuItems.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                text = outletConfig?.outletName ?: "Chai Bathak Bar",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = outletConfig?.addressLine1?.takeIf { it.isNotBlank() } ?: "POS Management Hub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Sync Indicator Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isOnline) StatusAvailableContainer else StatusOccupiedContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) StatusAvailableGreen else StatusOccupiedAmber)
                                )
                                Text(
                                    text = if (isOnline) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnline) StatusOnAvailableContainer else StatusOnOccupiedContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Prominent New Parcel Button
                    Button(
                        onClick = { showNewParcelDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "+ New Parcel", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(onClick = onNavigateReports) {
                        Icon(Icons.Default.BarChart, contentDescription = "Reports")
                    }
                    IconButton(onClick = onNavigateMenuTables) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = "Menu & Tables")
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Error banner if any
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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

            // First Run Setup Banner
            if (isFirstRunSetupNeeded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome to Chai Bathak Bar POS!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Setup initial demo tables, menu items, and outlet configurations with a single click.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = onSeedDemoOutlet,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Setup Chai Bathak Bar Demo Data")
                            }
                        }
                    }
                }
            }

            // Active Takeaway / Parcel Strip
            if (activeTakeawayOrders.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Takeaway Parcels",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeTakeawayOrders) { order ->
                            TakeawayParcelCard(
                                order = order,
                                onClick = { onOpenOrder(order.id) },
                                onDone = { onMarkParcelHandedOver(order.id) }
                            )
                        }
                    }
                }
            }

            // Dine-In Table Floor Map Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header & Zone Filter Chips + Status Counts
                    val zones = listOf("All") + tables.map { it.zone }.distinct().filter { it.isNotBlank() }
                    val freeCount = tables.count { it.status == "AVAILABLE" }
                    val occupiedCount = tables.count { it.status == "OCCUPIED" }
                    val billedCount = tables.count { it.status == "BILLED" }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            zones.forEach { zone ->
                                FilterChip(
                                    selected = selectedZone == zone,
                                    onClick = { onSetSelectedZone(zone) },
                                    label = { Text(zone) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatusCountBadge(label = "Free", count = freeCount, color = StatusAvailableGreen)
                            StatusCountBadge(label = "Occupied", count = occupiedCount, color = StatusOccupiedAmber)
                            StatusCountBadge(label = "Billed", count = billedCount, color = StatusBilledBlue)
                        }
                    }

                    // Filtered Tables
                    val displayedTables = if (selectedZone == "All") tables else tables.filter { it.zone == selectedZone }

                    if (displayedTables.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFirstRunSetupNeeded) "Setup demo data above to view tables." else "No tables found in this zone.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 185.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(displayedTables, key = { it.id }) { table ->
                                TableCard(
                                    table = table,
                                    onClick = { onOpenTable(table) },
                                    onLongClick = {
                                        if (table.status == "OCCUPIED") {
                                            tableToShift = table
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // New Parcel Dialog
    if (showNewParcelDialog) {
        AlertDialog(
            onDismissRequest = { showNewParcelDialog = false },
            title = { Text("Create New Takeaway Parcel") },
            text = {
                OutlinedTextField(
                    value = customerNameInput,
                    onValueChange = { customerNameInput = it },
                    label = { Text("Customer Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customerNameInput.trim()
                        showNewParcelDialog = false
                        customerNameInput = ""
                        onCreateTakeawayOrder(name)
                    }
                ) {
                    Text("Create & Open Parcel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewParcelDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Shift Table Dialog
    if (tableToShift != null) {
        val availableTables = tables.filter { it.status == "AVAILABLE" }
        AlertDialog(
            onDismissRequest = { tableToShift = null },
            title = { Text("Shift Table (${tableToShift?.label})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select an available table to shift this active order:")
                    if (availableTables.isEmpty()) {
                        Text("No available tables found.", color = MaterialTheme.colorScheme.error)
                    } else {
                        availableTables.forEach { targetTable ->
                            OutlinedButton(
                                onClick = {
                                    val source = tableToShift!!
                                    tableToShift = null
                                    onShiftTable(source, targetTable)
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
                TextButton(onClick = { tableToShift = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusCountBadge(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TakeawayParcelCard(
    order: PosOrder,
    onClick: () -> Unit,
    onDone: () -> Unit
) {
    val tokenLabel = if (order.tokenNo > 0) String.format(java.util.Locale.US, "Parcel #%02d", order.tokenNo) else "Parcel"
    val itemCount = order.items.sumOf { it.qty }
    val isPaid = order.status == "PAID"
    val elapsedTime = formatElapsedTime(order.createdAt)

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(130.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) StatusAvailableContainer.copy(alpha = 0.6f) else StatusOccupiedContainer.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tokenLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = elapsedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (order.customerName.isNotBlank()) {
                Text(
                    text = order.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$itemCount items • ${order.totalPaise.toRupees()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isPaid) "PAID — PACKING" else "UNPAID",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaid) StatusAvailableGreen else StatusOccupiedAmber,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isPaid) {
                    Button(
                        onClick = onDone,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreen)
                    ) {
                        Text("Done ✓", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableCard(
    table: TableModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (containerColor, contentColor, statusText) = when (table.status) {
        "OCCUPIED" -> Triple(StatusOccupiedContainer, StatusOnOccupiedContainer, formatElapsedTime(table.openedAt))
        "BILLED" -> Triple(StatusBilledContainer, StatusOnBilledContainer, "BILL READY")
        else -> Triple(StatusAvailableContainer, StatusOnAvailableContainer, "FREE")
    }

    Card(
        modifier = Modifier
            .height(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = table.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = table.zone,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (table.status == "AVAILABLE") "${table.capacity} seats" else table.activeTotalPaise.toRupees(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

private fun formatElapsedTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "0m"
    val diffSecs = Instant.now().epochSecond - timestamp.seconds
    val mins = maxOf(0L, diffSecs / 60)
    if (mins < 60) return "${mins}m"
    val hours = mins / 60
    val remMins = mins % 60
    return "${hours}h ${remMins}m"
}

@Preview(showBackground = true)
@Composable
fun PosHomeScreenPreview() {
    ChaiBethakBarTheme {
        PosHomeScreen(
            outletConfig = OutletConfig(outletName = "Chai Bathak Bar", addressLine1 = "Connaught Place, New Delhi"),
            tables = listOf(
                TableModel(id = "t1", label = "Table 1", zone = "Indoor", capacity = 4, status = "AVAILABLE"),
                TableModel(id = "t2", label = "Table 2", zone = "Indoor", capacity = 2, status = "OCCUPIED", activeTotalPaise = 45000L),
                TableModel(id = "t3", label = "Table 3", zone = "Outdoor", capacity = 6, status = "BILLED", activeTotalPaise = 120000L)
            ),
            menuItems = listOf(
                MenuItem(id = "m1", name = "Adrak Chai", category = "Chai", pricePaise = 3000L)
            ),
            activeTakeawayOrders = listOf(
                PosOrder(id = "o1", orderType = "TAKEAWAY", tokenNo = 1, customerName = "Rahul", totalPaise = 15000L, status = "OPEN")
            ),
            isOnline = true,
            isLoading = false,
            errorMessage = null,
            selectedZone = "All",
            onOpenOrder = {},
            onNavigateReports = {},
            onNavigateMenuTables = {},
            onNavigateSettings = {},
            onClearError = {},
            onSeedDemoOutlet = {},
            onMarkParcelHandedOver = {},
            onSetSelectedZone = {},
            onOpenTable = {},
            onCreateTakeawayOrder = {},
            onShiftTable = { _, _ -> }
        )
    }
}
