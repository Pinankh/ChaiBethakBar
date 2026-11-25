package com.pinankh.chaibethakbar.util

import com.pinankh.chaibethakbar.model.PosOrder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TopSellingItem(
    val menuItemId: String,
    val name: String,
    val quantity: Int,
    val revenuePaise: Long
)

data class DailyReportSummary(
    val totalSalesPaise: Long = 0L,
    val totalOrdersCount: Int = 0,
    val dineInSalesPaise: Long = 0L,
    val dineInOrdersCount: Int = 0,
    val takeawaySalesPaise: Long = 0L,
    val takeawayOrdersCount: Int = 0,
    val totalTaxPaise: Long = 0L,
    val totalDiscountPaise: Long = 0L,
    val cancelledOrdersCount: Int = 0,
    val cancelledOrdersTotalPaise: Long = 0L,
    val cashCollectedPaise: Long = 0L,
    val cashBillsCount: Int = 0,
    val onlineCollectedPaise: Long = 0L,
    val onlineBillsCount: Int = 0,
    val topSellingItems: List<TopSellingItem> = emptyList()
)

object ReportCalculator {

    /**
     * Filters a list of orders by date string ("yyyy-MM-dd") in IST.
     */
    fun filterOrdersByDate(orders: List<PosOrder>, dateString: String): List<PosOrder> {
        if (dateString.isBlank()) return orders
        val zoneId = ZoneId.of("Asia/Kolkata")
        return orders.filter { order ->
            val timestamp = order.closedAt ?: order.createdAt
            if (timestamp != null) {
                val istDate = Instant.ofEpochSecond(timestamp.seconds)
                    .atZone(zoneId)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                istDate == dateString
            } else {
                val todayIST = LocalDate.now(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                todayIST == dateString
            }
        }
    }

    /**
     * Calculates the daily report summary strictly following PRD rules:
     * - Only PAID orders count toward collected sales, taxes, discounts, and top selling items.
     * - CASH and ONLINE collections are calculated separately from PAID orders.
     * - DINE_IN and TAKEAWAY sales are calculated separately from PAID orders.
     * - Cancelled orders are excluded from completed sales and reported separately.
     * - Unpaid/OPEN orders are excluded from completed sales.
     * - Top selling items use item snapshots from historical orders.
     */
    fun calculateDailyReport(orders: List<PosOrder>): DailyReportSummary {
        val paidOrders = orders.filter { it.status == "PAID" }
        val cancelledOrders = orders.filter { it.status == "CANCELLED" }

        val cashOrders = paidOrders.filter { it.paymentMode.equals("CASH", ignoreCase = true) }
        val onlineOrders = paidOrders.filter { it.paymentMode.equals("ONLINE", ignoreCase = true) }

        val dineInOrders = paidOrders.filter { it.orderType.equals("DINE_IN", ignoreCase = true) }
        val takeawayOrders = paidOrders.filter { it.orderType.equals("TAKEAWAY", ignoreCase = true) }

        val cashCollected = cashOrders.sumOf { it.totalPaise }
        val onlineCollected = onlineOrders.sumOf { it.totalPaise }
        val totalSales = cashCollected + onlineCollected

        val dineInSales = dineInOrders.sumOf { it.totalPaise }
        val takeawaySales = takeawayOrders.sumOf { it.totalPaise }

        val totalTax = paidOrders.sumOf { it.cgstPaise + it.sgstPaise + it.vatPaise }
        val totalDiscount = paidOrders.sumOf { it.discountPaise }

        val cancelledCount = cancelledOrders.size
        val cancelledTotal = cancelledOrders.sumOf { it.totalPaise }

        // Top selling items aggregation from historical order item snapshots
        val itemStatsMap = mutableMapOf<String, MutableItemStat>()
        for (order in paidOrders) {
            for (item in order.items) {
                if (item.qty <= 0) continue
                val key = if (item.menuItemId.isNotBlank()) item.menuItemId else item.name
                val stat = itemStatsMap.getOrPut(key) { MutableItemStat(item.name) }
                stat.quantity += item.qty
                stat.revenuePaise += (item.unitPricePaise * item.qty.toLong())
            }
        }

        val topSellingItems = itemStatsMap.entries.map { (id, stat) ->
            TopSellingItem(
                menuItemId = id,
                name = stat.name,
                quantity = stat.quantity,
                revenuePaise = stat.revenuePaise
            )
        }.sortedWith(compareByDescending<TopSellingItem> { it.quantity }.thenByDescending { it.revenuePaise })

        return DailyReportSummary(
            totalSalesPaise = totalSales,
            totalOrdersCount = paidOrders.size,
            dineInSalesPaise = dineInSales,
            dineInOrdersCount = dineInOrders.size,
            takeawaySalesPaise = takeawaySales,
            takeawayOrdersCount = takeawayOrders.size,
            totalTaxPaise = totalTax,
            totalDiscountPaise = totalDiscount,
            cancelledOrdersCount = cancelledCount,
            cancelledOrdersTotalPaise = cancelledTotal,
            cashCollectedPaise = cashCollected,
            cashBillsCount = cashOrders.size,
            onlineCollectedPaise = onlineCollected,
            onlineBillsCount = onlineOrders.size,
            topSellingItems = topSellingItems
        )
    }

    private class MutableItemStat(
        val name: String,
        var quantity: Int = 0,
        var revenuePaise: Long = 0L
    )
}
