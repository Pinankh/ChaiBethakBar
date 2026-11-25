package com.pinankh.chaibethakbar.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.ui.theme.ChaiBethakBarTheme
import com.pinankh.chaibethakbar.util.toRupees
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatBillTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "-"
    val instant = Instant.ofEpochSecond(timestamp.seconds)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US)
        .withZone(ZoneId.of("Asia/Kolkata"))
    return formatter.format(instant)
}

fun buildShareableBillText(order: PosOrder, outletConfig: OutletConfig?): String {
    val sb = StringBuilder()
    val outletName = outletConfig?.outletName?.takeIf { it.isNotBlank() } ?: "Chai Bathak Bar"
    sb.appendLine("================================")
    sb.appendLine(outletName)
    if (!outletConfig?.addressLine1.isNullOrBlank()) {
        sb.appendLine(outletConfig.addressLine1)
    }
    if (!outletConfig?.cityPincode.isNullOrBlank()) {
        sb.appendLine(outletConfig.cityPincode)
    }
    if (!outletConfig?.phone.isNullOrBlank()) {
        sb.appendLine("Phone: ${outletConfig.phone}")
    }
    if (!outletConfig?.gstin.isNullOrBlank()) {
        sb.appendLine("GSTIN: ${outletConfig.gstin}")
    }
    if (!outletConfig?.fssaiNo.isNullOrBlank()) {
        sb.appendLine("FSSAI: ${outletConfig.fssaiNo}")
    }
    sb.appendLine("================================")
    if (order.billNo.isNotBlank()) {
        sb.appendLine("Bill No: ${order.billNo}")
    }
    val timestamp = order.closedAt ?: order.createdAt
    sb.appendLine("Date: ${formatBillTimestamp(timestamp)}")
    val label = if (order.orderType == "DINE_IN") order.tableLabel else if (order.tokenNo > 0) "Parcel #${String.format(Locale.US, "%02d", order.tokenNo)}" else "Takeaway Parcel"
    sb.appendLine("Type: $label")
    if (order.customerName.isNotBlank()) {
        sb.appendLine("Customer: ${order.customerName}")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("ITEMS:")
    for (item in order.items) {
        val lineTotal = (item.unitPricePaise * item.qty).toRupees()
        val noteStr = if (item.notes.isNotBlank()) " (${item.notes})" else ""
        sb.appendLine("• ${item.name}$noteStr x${item.qty} = $lineTotal")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("Subtotal: ${order.subtotalPaise.toRupees()}")
    if (order.parcelChargePaise > 0L) {
        sb.appendLine("Parcel Charge: +${order.parcelChargePaise.toRupees()}")
    }
    if (order.discountPaise > 0L) {
        val reason = if (order.discountReason.isNotBlank()) " (${order.discountReason})" else ""
        sb.appendLine("Discount$reason: -${order.discountPaise.toRupees()}")
    }
    if (order.cgstPaise > 0L) {
        sb.appendLine("CGST: +${order.cgstPaise.toRupees()}")
    }
    if (order.sgstPaise > 0L) {
        sb.appendLine("SGST: +${order.sgstPaise.toRupees()}")
    }
    if (order.vatPaise > 0L) {
        sb.appendLine("VAT: +${order.vatPaise.toRupees()}")
    }
    if (order.roundOffPaise != 0L) {
        val sign = if (order.roundOffPaise > 0) "+" else ""
        sb.appendLine("Round-Off: $sign${order.roundOffPaise.toRupees()}")
    }
    sb.appendLine("================================")
    sb.appendLine("GRAND TOTAL: ${order.totalPaise.toRupees()}")
    val modeText = if (order.paymentMode.isNotBlank()) order.paymentMode else if (order.status == "PAID") "PAID" else "UNPAID"
    sb.appendLine("Payment Mode: $modeText")
    sb.appendLine("================================")
    sb.appendLine("Thank you! Visit Again.")
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalBillDialog(
    order: PosOrder,
    outletConfig: OutletConfig?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 540.dp)
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
            ) {
                // Header with Close Button
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
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Digital Receipt",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Professional Receipt Canvas (White Paper Style)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            // Outlet Header
                            Text(
                                text = outletConfig?.outletName?.takeIf { it.isNotBlank() } ?: "Chai Bathak Bar",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                textAlign = TextAlign.Center
                            )
                            val config = outletConfig
                            if (config != null && config.addressLine1.isNotBlank()) {
                                Text(
                                    text = config.addressLine1,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4B5563),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (config != null && config.cityPincode.isNotBlank()) {
                                Text(
                                    text = config.cityPincode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4B5563),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (config != null && config.phone.isNotBlank()) {
                                Text(
                                    text = "Ph: ${config.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4B5563),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (config != null && config.gstin.isNotBlank()) {
                                Text(
                                    text = "GSTIN: ${config.gstin}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4B5563),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (config != null && config.fssaiNo.isNotBlank()) {
                                Text(
                                    text = "FSSAI: ${config.fssaiNo}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4B5563),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Order Info Block
                            val orderLabel = if (order.orderType == "DINE_IN") {
                                order.tableLabel.ifBlank { "Dine-In" }
                            } else {
                                if (order.tokenNo > 0) String.format(Locale.US, "Parcel #%02d", order.tokenNo) else "Takeaway"
                            }

                            ReceiptInfoRow(
                                label = if (order.billNo.isNotBlank()) "Bill No:" else "Order Ref:",
                                value = if (order.billNo.isNotBlank()) order.billNo else order.id.take(8).uppercase(Locale.US)
                            )
                            ReceiptInfoRow(
                                label = "Date & Time:",
                                value = formatBillTimestamp(order.closedAt ?: order.createdAt)
                            )
                            ReceiptInfoRow(
                                label = "Table / Token:",
                                value = orderLabel
                            )
                            if (order.customerName.isNotBlank()) {
                                ReceiptInfoRow(
                                    label = "Customer Name:",
                                    value = order.customerName
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Table Header for Items
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ITEM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF374151), modifier = Modifier.weight(2f))
                                Text("QTY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF374151), textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                                Text("PRICE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF374151), textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                Text("AMOUNT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF374151), textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Item rows
                        items(order.items) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827)
                                    )
                                    if (item.notes.isNotBlank()) {
                                        Text(
                                            text = item.notes,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }
                                Text(
                                    text = item.qty.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF111827),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = item.unitPricePaise.toRupees(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF111827),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = (item.unitPricePaise * item.qty).toRupees(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF111827),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Monetary breakdown
                            ReceiptAmountRow(label = "Subtotal", value = order.subtotalPaise.toRupees())

                            if (order.parcelChargePaise > 0L) {
                                ReceiptAmountRow(label = "Parcel Packaging", value = "+${order.parcelChargePaise.toRupees()}")
                            }

                            if (order.discountPaise > 0L) {
                                val reason = if (order.discountReason.isNotBlank()) " (${order.discountReason})" else ""
                                ReceiptAmountRow(label = "Discount$reason", value = "-${order.discountPaise.toRupees()}", isNegative = true)
                            }

                            if (order.cgstPaise > 0L) {
                                ReceiptAmountRow(label = "CGST", value = "+${order.cgstPaise.toRupees()}")
                            }
                            if (order.sgstPaise > 0L) {
                                ReceiptAmountRow(label = "SGST", value = "+${order.sgstPaise.toRupees()}")
                            }
                            if (order.vatPaise > 0L) {
                                ReceiptAmountRow(label = "VAT", value = "+${order.vatPaise.toRupees()}")
                            }
                            if (order.roundOffPaise != 0L) {
                                val sign = if (order.roundOffPaise > 0) "+" else ""
                                ReceiptAmountRow(label = "Round-Off", value = "$sign${order.roundOffPaise.toRupees()}")
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(6.dp))

                            // Grand Total & Payment Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GRAND TOTAL",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = order.totalPaise.toRupees(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val paymentLabel = when {
                                order.paymentMode.isNotBlank() -> "PAID VIA ${order.paymentMode}"
                                order.status == "PAID" -> "PAID"
                                else -> "PAYMENT PENDING (UNPAID)"
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (order.status == "PAID" || order.paymentMode.isNotBlank()) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = paymentLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (order.status == "PAID" || order.paymentMode.isNotBlank()) Color(0xFF166534) else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Share & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val shareText = buildShareableBillText(order, outletConfig)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val chooser = Intent.createChooser(sendIntent, "Share Digital Bill")
                            context.startActivity(chooser)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Bill (WhatsApp / Text)")
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DashedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF9CA3AF),
        maxLines = 1,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ReceiptInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
    }
}

@Composable
fun ReceiptAmountRow(label: String, value: String, isNegative: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isNegative) Color(0xFFDC2626) else Color(0xFF111827)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DigitalBillDialogPreview() {
    ChaiBethakBarTheme {
        DigitalBillDialog(
            order = PosOrder(
                id = "ord1",
                billNo = "CBB-250226-120000-A1B2",
                orderType = "DINE_IN",
                tableLabel = "Table 2",
                customerName = "Amit",
                status = "PAID",
                paymentMode = "ONLINE",
                items = listOf(
                    com.pinankh.chaibethakbar.model.OrderItem(menuItemId = "1", name = "Kulhad Masala Chai", unitPricePaise = 4000L, qty = 2),
                    com.pinankh.chaibethakbar.model.OrderItem(menuItemId = "2", name = "Bun Maska", unitPricePaise = 5000L, qty = 1)
                ),
                subtotalPaise = 13000L,
                cgstPaise = 325L,
                sgstPaise = 325L,
                totalPaise = 13650L
            ),
            outletConfig = OutletConfig(
                outletName = "Chai Bathak Bar",
                addressLine1 = "12 Main Street",
                cityPincode = "Ahmedabad 380015",
                phone = "+91 98765 43210",
                gstin = "24AAAAA0000A1Z5",
                fssaiNo = "10020021000123"
            ),
            onDismiss = {}
        )
    }
}
