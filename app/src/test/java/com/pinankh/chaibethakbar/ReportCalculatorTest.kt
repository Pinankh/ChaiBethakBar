package com.pinankh.chaibethakbar

import com.google.firebase.Timestamp
import com.pinankh.chaibethakbar.model.OrderItem
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.util.ReportCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReportCalculatorTest {

    @Test
    fun testEmptyReportData() {
        val summary = ReportCalculator.calculateDailyReport(emptyList())

        assertEquals(0L, summary.totalSalesPaise)
        assertEquals(0, summary.totalOrdersCount)
        assertEquals(0L, summary.dineInSalesPaise)
        assertEquals(0, summary.dineInOrdersCount)
        assertEquals(0L, summary.takeawaySalesPaise)
        assertEquals(0, summary.takeawayOrdersCount)
        assertEquals(0L, summary.cashCollectedPaise)
        assertEquals(0, summary.cashBillsCount)
        assertEquals(0L, summary.onlineCollectedPaise)
        assertEquals(0, summary.onlineBillsCount)
        assertEquals(0L, summary.totalTaxPaise)
        assertEquals(0L, summary.totalDiscountPaise)
        assertEquals(0, summary.cancelledOrdersCount)
        assertEquals(0L, summary.cancelledOrdersTotalPaise)
        assertTrue(summary.topSellingItems.isEmpty())
    }

    @Test
    fun testZeroSalesDayWithOnlyOpenOrCancelledOrders() {
        val openOrder = PosOrder(id = "1", status = "OPEN", totalPaise = 15000L)
        val cancelledOrder = PosOrder(id = "2", status = "CANCELLED", totalPaise = 20000L)

        val summary = ReportCalculator.calculateDailyReport(listOf(openOrder, cancelledOrder))

        assertEquals(0L, summary.totalSalesPaise)
        assertEquals(0, summary.totalOrdersCount)
        assertEquals(0L, summary.cashCollectedPaise)
        assertEquals(0L, summary.onlineCollectedPaise)
        assertEquals(1, summary.cancelledOrdersCount)
        assertEquals(20000L, summary.cancelledOrdersTotalPaise)
    }

    @Test
    fun testDailyCashCollection() {
        val cashOrder1 = PosOrder(id = "1", status = "PAID", paymentMode = "CASH", totalPaise = 4000L)
        val cashOrder2 = PosOrder(id = "2", status = "PAID", paymentMode = "CASH", totalPaise = 6000L)
        val onlineOrder = PosOrder(id = "3", status = "PAID", paymentMode = "ONLINE", totalPaise = 10000L)

        val summary = ReportCalculator.calculateDailyReport(listOf(cashOrder1, cashOrder2, onlineOrder))

        assertEquals(10000L, summary.cashCollectedPaise)
        assertEquals(2, summary.cashBillsCount)
        assertEquals(20000L, summary.totalSalesPaise)
    }

    @Test
    fun testDailyOnlineCollection() {
        val online1 = PosOrder(id = "1", status = "PAID", paymentMode = "ONLINE", totalPaise = 12000L)
        val online2 = PosOrder(id = "2", status = "PAID", paymentMode = "ONLINE", totalPaise = 8000L)

        val summary = ReportCalculator.calculateDailyReport(listOf(online1, online2))

        assertEquals(20000L, summary.onlineCollectedPaise)
        assertEquals(2, summary.onlineBillsCount)
        assertEquals(0L, summary.cashCollectedPaise)
        assertEquals(0, summary.cashBillsCount)
        assertEquals(20000L, summary.totalSalesPaise)
    }

    @Test
    fun testDineInVsTakeawaySales() {
        val dineIn1 = PosOrder(id = "1", orderType = "DINE_IN", status = "PAID", paymentMode = "CASH", totalPaise = 15000L)
        val dineIn2 = PosOrder(id = "2", orderType = "DINE_IN", status = "PAID", paymentMode = "ONLINE", totalPaise = 25000L)
        val takeaway = PosOrder(id = "3", orderType = "TAKEAWAY", status = "PAID", paymentMode = "CASH", totalPaise = 10000L)

        val summary = ReportCalculator.calculateDailyReport(listOf(dineIn1, dineIn2, takeaway))

        assertEquals(40000L, summary.dineInSalesPaise)
        assertEquals(2, summary.dineInOrdersCount)
        assertEquals(10000L, summary.takeawaySalesPaise)
        assertEquals(1, summary.takeawayOrdersCount)
        assertEquals(50000L, summary.totalSalesPaise)
    }

    @Test
    fun testCancelledOrderExclusion() {
        val paid = PosOrder(id = "1", status = "PAID", paymentMode = "CASH", totalPaise = 5000L)
        val cancelled = PosOrder(id = "2", status = "CANCELLED", paymentMode = "CASH", totalPaise = 15000L, cancelReason = "Customer left")

        val summary = ReportCalculator.calculateDailyReport(listOf(paid, cancelled))

        assertEquals(5000L, summary.totalSalesPaise)
        assertEquals(1, summary.totalOrdersCount)
        assertEquals(1, summary.cancelledOrdersCount)
        assertEquals(15000L, summary.cancelledOrdersTotalPaise)
    }

    @Test
    fun testUnpaidOrderExclusion() {
        val paid = PosOrder(id = "1", status = "PAID", paymentMode = "ONLINE", totalPaise = 8000L)
        val open = PosOrder(id = "2", status = "OPEN", totalPaise = 12000L)

        val summary = ReportCalculator.calculateDailyReport(listOf(paid, open))

        assertEquals(8000L, summary.totalSalesPaise)
        assertEquals(1, summary.totalOrdersCount)
    }

    @Test
    fun testTaxAndDiscountReporting() {
        val order1 = PosOrder(
            id = "1",
            status = "PAID",
            paymentMode = "CASH",
            subtotalPaise = 10000L,
            discountPaise = 1000L,
            cgstPaise = 225L,
            sgstPaise = 225L,
            vatPaise = 0L,
            totalPaise = 9450L
        )

        val order2 = PosOrder(
            id = "2",
            status = "PAID",
            paymentMode = "ONLINE",
            subtotalPaise = 20000L,
            discountPaise = 2000L,
            cgstPaise = 0L,
            sgstPaise = 0L,
            vatPaise = 2400L,
            totalPaise = 20400L
        )

        val summary = ReportCalculator.calculateDailyReport(listOf(order1, order2))

        assertEquals(3000L, summary.totalDiscountPaise)
        assertEquals(2850L, summary.totalTaxPaise) // 225 + 225 + 2400 = 2850
        assertEquals(29850L, summary.totalSalesPaise)
    }

    @Test
    fun testTopSellingItemCalculations() {
        val itemChai = OrderItem(menuItemId = "m1", name = "Kulhad Masala Chai", unitPricePaise = 4000L, qty = 3)
        val itemSamosa = OrderItem(menuItemId = "m2", name = "Samosa", unitPricePaise = 2000L, qty = 5)

        val order1 = PosOrder(id = "1", status = "PAID", paymentMode = "CASH", items = listOf(itemChai, itemSamosa))
        val order2 = PosOrder(id = "2", status = "PAID", paymentMode = "ONLINE", items = listOf(itemChai))

        val summary = ReportCalculator.calculateDailyReport(listOf(order1, order2))

        assertEquals(2, summary.topSellingItems.size)

        // Chai total qty = 3 + 3 = 6. Samosa total qty = 5.
        val top1 = summary.topSellingItems[0]
        assertEquals("Kulhad Masala Chai", top1.name)
        assertEquals(6, top1.quantity)
        assertEquals(24000L, top1.revenuePaise) // 6 * 4000L

        val top2 = summary.topSellingItems[1]
        assertEquals("Samosa", top2.name)
        assertEquals(5, top2.quantity)
        assertEquals(10000L, top2.revenuePaise) // 5 * 2000L
    }

    @Test
    fun testHistoricalOrderDateFiltering() {
        val istZone = ZoneId.of("Asia/Kolkata")
        val targetDate = "2025-02-26"

        // Create timestamp for 2025-02-26 14:30:00 IST
        val targetZdt = ZonedDateTime.of(2025, 2, 26, 14, 30, 0, 0, istZone)
        val targetTimestamp = Timestamp(targetZdt.toEpochSecond(), 0)

        // Create timestamp for 2025-02-25 23:30:00 IST
        val prevZdt = ZonedDateTime.of(2025, 2, 25, 23, 30, 0, 0, istZone)
        val prevTimestamp = Timestamp(prevZdt.toEpochSecond(), 0)

        val targetOrder = PosOrder(id = "1", status = "PAID", createdAt = targetTimestamp)
        val prevOrder = PosOrder(id = "2", status = "PAID", createdAt = prevTimestamp)

        val filtered = ReportCalculator.filterOrdersByDate(listOf(targetOrder, prevOrder), targetDate)

        assertEquals(1, filtered.size)
        assertEquals("1", filtered[0].id)
    }
}
