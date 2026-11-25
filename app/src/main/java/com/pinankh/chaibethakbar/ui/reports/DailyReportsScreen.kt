package com.pinankh.chaibethakbar.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.ui.components.DigitalBillDialog
import com.pinankh.chaibethakbar.ui.theme.ChaiBethakBarTheme
import com.pinankh.chaibethakbar.util.DailyReportSummary
import com.pinankh.chaibethakbar.util.TopSellingItem
import com.pinankh.chaibethakbar.util.toRupees
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val reportSummary by viewModel.reportSummary.collectAsStateWithLifecycle()
    val filteredHistoryOrders by viewModel.filteredHistoryOrders.collectAsStateWithLifecycle()
    val outletConfig by viewModel.outletConfig.collectAsStateWithLifecycle()
    val orderTypeFilter by viewModel.orderTypeFilter.collectAsStateWithLifecycle()
    val paymentModeFilter by viewModel.paymentModeFilter.collectAsStateWithLifecycle()

    DailyReportsScreenContent(
        selectedDate = selectedDate,
        summary = reportSummary,
        historyOrders = filteredHistoryOrders,
        outletConfig = outletConfig,
        orderTypeFilter = orderTypeFilter,
        paymentModeFilter = paymentModeFilter,
        onBack = onBack,
        onDateSelected = { dateStr -> viewModel.setSelectedDate(dateStr) },
        onOrderTypeFilterChange = { filter -> viewModel.setOrderTypeFilter(filter) },
        onPaymentModeFilterChange = { filter -> viewModel.setPaymentModeFilter(filter) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportsScreenContent(
    selectedDate: String,
    summary: DailyReportSummary,
    historyOrders: List<PosOrder>,
    outletConfig: OutletConfig?,
    orderTypeFilter: String,
    paymentModeFilter: String,
    onBack: () -> Unit,
    onDateSelected: (String) -> Unit,
    onOrderTypeFilterChange: (String) -> Unit,
    onPaymentModeFilterChange: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedOrderForBillDialog by remember { mutableStateOf<PosOrder?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val todayIST = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val isToday = selectedDate == todayIST

    val displayDateLabel = if (isToday) "Today ($selectedDate)" else selectedDate

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Sales Reports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Date Selector Button
                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayDateLabel, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. KPI Summary Cards Grid
            item {
                Text(
                    text = "Key Performance Indicators ($displayDateLabel)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                KpiGridSection(summary = summary)
            }

            // 2. Payment Tally Card (Highlighted Cash vs Online Collection Summary)
            item {
                PaymentTallyCard(summary = summary)
            }

            // 3. Top Selling Items Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Top Selling Items (Historical Snapshots)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (summary.topSellingItems.isEmpty()) {
                            Text(
                                text = "No completed item sales recorded for this date.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            summary.topSellingItems.take(5).forEachIndexed { index, item ->
                                TopSellingItemRow(rank = index + 1, item = item)
                            }
                        }
                    }
                }
            }

            // 4. Bill History List Section
            item {
                Text(
                    text = "Historical Bill Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Filters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Order Type Filter
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Text("Type:", style = MaterialTheme.typography.labelSmall)
                        FilterChip(
                            selected = orderTypeFilter == "ALL",
                            onClick = { onOrderTypeFilterChange("ALL") },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = orderTypeFilter == "DINE_IN",
                            onClick = { onOrderTypeFilterChange("DINE_IN") },
                            label = { Text("Dine-In") }
                        )
                        FilterChip(
                            selected = orderTypeFilter == "TAKEAWAY",
                            onClick = { onOrderTypeFilterChange("TAKEAWAY") },
                            label = { Text("Takeaway") }
                        )
                    }

                    // Payment Mode Filter
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Text("Payment:", style = MaterialTheme.typography.labelSmall)
                        FilterChip(
                            selected = paymentModeFilter == "ALL",
                            onClick = { onPaymentModeFilterChange("ALL") },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = paymentModeFilter == "CASH",
                            onClick = { onPaymentModeFilterChange("CASH") },
                            label = { Text("Cash") }
                        )
                        FilterChip(
                            selected = paymentModeFilter == "ONLINE",
                            onClick = { onPaymentModeFilterChange("ONLINE") },
                            label = { Text("Online") }
                        )
                    }
                }
            }

            // Bill History Items
            if (historyOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No bills matching filters found for $displayDateLabel.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(historyOrders, key = { it.id }) { order ->
                    BillHistoryRowCard(
                        order = order,
                        onClick = {
                            if (order.status == "PAID") {
                                selectedOrderForBillDialog = order
                            }
                        }
                    )
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val selectedDateStr = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            onDateSelected(selectedDateStr)
                        }
                    }
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Digital Bill Dialog Preview for historical orders
    if (selectedOrderForBillDialog != null) {
        DigitalBillDialog(
            order = selectedOrderForBillDialog!!,
            outletConfig = outletConfig,
            onDismiss = { selectedOrderForBillDialog = null }
        )
    }
}

@Composable
fun KpiGridSection(summary: DailyReportSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KpiCard(
                title = "Total Sales",
                value = summary.totalSalesPaise.toRupees(),
                subtitle = "${summary.totalOrdersCount} Completed Bills",
                colorContainer = MaterialTheme.colorScheme.primaryContainer,
                colorOnContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )

            KpiCard(
                title = "Dine-In Sales",
                value = summary.dineInSalesPaise.toRupees(),
                subtitle = "${summary.dineInOrdersCount} Orders",
                colorContainer = MaterialTheme.colorScheme.secondaryContainer,
                colorOnContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )

            KpiCard(
                title = "Takeaway Sales",
                value = summary.takeawaySalesPaise.toRupees(),
                subtitle = "${summary.takeawayOrdersCount} Parcels",
                colorContainer = MaterialTheme.colorScheme.tertiaryContainer,
                colorOnContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KpiCard(
                title = "Total Tax (GST/VAT)",
                value = summary.totalTaxPaise.toRupees(),
                subtitle = "Collected Tax",
                colorContainer = MaterialTheme.colorScheme.surfaceVariant,
                colorOnContainer = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            KpiCard(
                title = "Total Discounts",
                value = summary.totalDiscountPaise.toRupees(),
                subtitle = "Applied Discounts",
                colorContainer = MaterialTheme.colorScheme.surfaceVariant,
                colorOnContainer = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            KpiCard(
                title = "Cancelled Orders",
                value = "${summary.cancelledOrdersCount}",
                subtitle = "Loss: ${summary.cancelledOrdersTotalPaise.toRupees()}",
                colorContainer = MaterialTheme.colorScheme.errorContainer,
                colorOnContainer = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    colorContainer: Color,
    colorOnContainer: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colorOnContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorOnContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorOnContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PaymentTallyCard(summary: DailyReportSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Cash vs Online Payment Tally",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF334155)
                ) {
                    Text(
                        text = "PAID Orders Only",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CASH Tally Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF064E3B)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                            Text("CASH COLLECTION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
                        }
                        Text(
                            text = summary.cashCollectedPaise.toRupees(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${summary.cashBillsCount} Cash Bills",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6EE7B7)
                        )
                    }
                }

                // ONLINE Tally Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E3A8A)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                            Text("ONLINE COLLECTION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFBFDBFE))
                        }
                        Text(
                            text = summary.onlineCollectedPaise.toRupees(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${summary.onlineBillsCount} Online Bills",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF93C5FD)
                        )
                    }
                }
            }

            Text(
                text = "Tally Verification: Cash (${summary.cashCollectedPaise.toRupees()}) + Online (${summary.onlineCollectedPaise.toRupees()}) = Total Collected (${summary.totalSalesPaise.toRupees()})",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun TopSellingItemRow(rank: Int, item: TopSellingItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sold: x${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = item.revenuePaise.toRupees(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BillHistoryRowCard(
    order: PosOrder,
    onClick: () -> Unit
) {
    val isPaid = order.status == "PAID"
    val isCancelled = order.status == "CANCELLED"

    val timeFormatted = formatBillTimestamp(order.closedAt ?: order.createdAt)

    val labelStr = if (order.billNo.isNotBlank()) {
        order.billNo
    } else if (order.orderType == "TAKEAWAY" && order.tokenNo > 0) {
        String.format(Locale.US, "Parcel #%02d", order.tokenNo)
    } else {
        order.tableLabel.ifBlank { "Bill #${order.id.take(6).uppercase(Locale.US)}" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isPaid, onClick = onClick),
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = labelStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (order.orderType == "DINE_IN") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = if (order.orderType == "DINE_IN") "Dine-In" else "Takeaway",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (order.tableLabel.isNotBlank() && order.orderType == "DINE_IN") {
                        Text(
                            text = "(${order.tableLabel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (order.customerName.isNotBlank()) {
                        Text(
                            text = "Cust: ${order.customerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (order.items.isNotEmpty()) {
                        Text(
                            text = "${order.items.sumOf { it.qty }} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isCancelled && order.cancelReason.isNotBlank()) {
                    Text(
                        text = "Reason: ${order.cancelReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = order.totalPaise.toRupees(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when {
                            isCancelled -> MaterialTheme.colorScheme.errorContainer
                            isPaid -> Color(0xFFDCFCE7)
                            else -> Color(0xFFFEF3C7)
                        }
                    ) {
                        Text(
                            text = when {
                                isCancelled -> "CANCELLED"
                                isPaid -> "PAID"
                                else -> "OPEN"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isCancelled -> MaterialTheme.colorScheme.onErrorContainer
                                isPaid -> Color(0xFF166534)
                                else -> Color(0xFF92400E)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isPaid && order.paymentMode.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = order.paymentMode,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBillTimestamp(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "-"
    val instant = Instant.ofEpochSecond(timestamp.seconds)
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
        .withZone(ZoneId.of("Asia/Kolkata"))
    return formatter.format(instant)
}

@Preview(showBackground = true)
@Composable
fun DailyReportsScreenPreview() {
    ChaiBethakBarTheme {
        DailyReportsScreenContent(
            selectedDate = "2025-02-26",
            summary = DailyReportSummary(
                totalSalesPaise = 250000L,
                totalOrdersCount = 15,
                dineInSalesPaise = 180000L,
                dineInOrdersCount = 10,
                takeawaySalesPaise = 70000L,
                takeawayOrdersCount = 5,
                cashCollectedPaise = 100000L,
                cashBillsCount = 6,
                onlineCollectedPaise = 150000L,
                onlineBillsCount = 9,
                totalTaxPaise = 12500L,
                totalDiscountPaise = 2000L,
                cancelledOrdersCount = 1,
                cancelledOrdersTotalPaise = 4000L,
                topSellingItems = listOf(
                    TopSellingItem("1", "Kulhad Masala Chai", 25, 100000L),
                    TopSellingItem("2", "Cheese Grill Sandwich", 10, 120000L)
                )
            ),
            historyOrders = listOf(
                PosOrder(
                    id = "o1",
                    billNo = "CBB-250226-101500-A1B2",
                    orderType = "DINE_IN",
                    tableLabel = "Table 2",
                    totalPaise = 24000L,
                    status = "PAID",
                    paymentMode = "ONLINE"
                )
            ),
            outletConfig = OutletConfig(),
            orderTypeFilter = "ALL",
            paymentModeFilter = "ALL",
            onBack = {},
            onDateSelected = {},
            onOrderTypeFilterChange = {},
            onPaymentModeFilterChange = {}
        )
    }
}
