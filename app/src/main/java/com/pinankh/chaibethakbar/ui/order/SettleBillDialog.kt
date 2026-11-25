package com.pinankh.chaibethakbar.ui.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.ui.theme.ChaiBethakBarTheme
import com.pinankh.chaibethakbar.ui.theme.StatusAvailableGreen
import com.pinankh.chaibethakbar.util.toRupees
import java.util.Locale

/**
 * Pure helper function for cash change calculation.
 * All monetary calculations use Long paise.
 */
fun calculateCashChangePaise(totalPaise: Long, tenderedPaise: Long): Long {
    return maxOf(0L, tenderedPaise - totalPaise)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleBillDialog(
    order: PosOrder,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirmSettle: (paymentMode: String, isHandedOver: Boolean) -> Unit,
    onClearError: () -> Unit = {}
) {
    var selectedPaymentMode by remember { mutableStateOf("ONLINE") } // ONLINE or CASH
    var selectedTenderChip by remember { mutableStateOf("Exact") } // Exact, 100, 200, 500, 1000
    var isHandedOver by remember { mutableStateOf(false) }

    // Calculate tendered amount in paise
    val tenderedPaise: Long = remember(selectedTenderChip, order.totalPaise) {
        when (selectedTenderChip) {
            "Exact" -> order.totalPaise
            "100" -> 10000L
            "200" -> 20000L
            "500" -> 50000L
            "1000" -> 100000L
            else -> order.totalPaise
        }
    }

    val changeToReturnPaise = remember(order.totalPaise, tenderedPaise) {
        calculateCashChangePaise(order.totalPaise, tenderedPaise)
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val headerLabel = if (order.orderType == "DINE_IN") {
                            order.tableLabel.ifBlank { "Dine-In Order" }
                        } else {
                            if (order.tokenNo > 0) String.format(Locale.US, "Parcel #%02d", order.tokenNo) else "Takeaway Order"
                        }
                        Text(
                            text = "Settle Payment — $headerLabel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select payment mode to finalize billing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error banner if write failed
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
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
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearError) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                // Grand Total Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total Payable",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = order.totalPaise.toRupees(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Payment Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Two Large Payment Selector Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. ONLINE CARD
                    val isOnlineSelected = selectedPaymentMode == "ONLINE"
                    Surface(
                        onClick = { selectedPaymentMode = "ONLINE" },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOnlineSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isOnlineSelected) 2.dp else 1.dp,
                            color = if (isOnlineSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = if (isOnlineSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ONLINE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnlineSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "UPI / Bank / Digital",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnlineSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. CASH CARD
                    val isCashSelected = selectedPaymentMode == "CASH"
                    Surface(
                        onClick = { selectedPaymentMode = "CASH" },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCashSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isCashSelected) 2.dp else 1.dp,
                            color = if (isCashSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isCashSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "CASH",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCashSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Physical Notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCashSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // CASH Quick Tender Section
                if (selectedPaymentMode == "CASH") {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Quick Cash Tendered",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Chips: Exact, ₹100, ₹200, ₹500, ₹1000
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val chips = listOf("Exact", "100", "200", "500", "1000")
                                chips.forEach { chipKey ->
                                    val chipLabel = if (chipKey == "Exact") "Exact" else "₹$chipKey"
                                    val isSelected = selectedTenderChip == chipKey
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTenderChip = chipKey
                                        },
                                        label = {
                                            Text(
                                                text = chipLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    )
                                }
                            }

                            // Dynamic Change to Return Display
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Change to Return:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = changeToReturnPaise.toRupees(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (changeToReturnPaise > 0L) StatusAvailableGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // TAKEAWAY ONLY: Handed Over Already Checkbox
                if (order.orderType == "TAKEAWAY") {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isHandedOver,
                                onCheckedChange = { isHandedOver = it },
                                modifier = Modifier.size(48.dp)
                            )
                            Column {
                                Text(
                                    text = "Handed Over Already",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isHandedOver) "Parcel will be completed and cleared from Home strip." else "Parcel will stay in Home Active Parcels strip as PAID — PACKING.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirmation Button
                val confirmButtonText = if (selectedPaymentMode == "ONLINE") {
                    "Confirm Paid (Online)"
                } else {
                    "Confirm Paid (Cash)"
                }

                Button(
                    onClick = {
                        onConfirmSettle(selectedPaymentMode, isHandedOver)
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Processing Settlement...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = confirmButtonText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettleBillDialogPreview() {
    ChaiBethakBarTheme {
        SettleBillDialog(
            order = PosOrder(
                id = "ord1",
                orderType = "DINE_IN",
                tableLabel = "Table 2",
                totalPaise = 15000L
            ),
            isSaving = false,
            errorMessage = null,
            onDismiss = {},
            onConfirmSettle = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettleBillDialogTakeawayPreview() {
    ChaiBethakBarTheme {
        SettleBillDialog(
            order = PosOrder(
                id = "ord2",
                orderType = "TAKEAWAY",
                tokenNo = 5,
                totalPaise = 25000L
            ),
            isSaving = false,
            errorMessage = null,
            onDismiss = {},
            onConfirmSettle = { _, _ -> }
        )
    }
}
