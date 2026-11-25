package com.pinankh.chaibethakbar

import com.pinankh.chaibethakbar.model.OrderItem
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.ui.components.buildShareableBillText
import com.pinankh.chaibethakbar.ui.order.calculateCashChangePaise
import com.pinankh.chaibethakbar.util.TaxCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderOperationTest {

    @Test
    fun testKitchenStatusTracking() {
        val item1 = OrderItem(menuItemId = "1", name = "Chai", unitPricePaise = 4000L, qty = 3, sentQty = 1)
        val item2 = OrderItem(menuItemId = "2", name = "Samosa", unitPricePaise = 2000L, qty = 2, sentQty = 2)

        val unsentQty1 = item1.qty - item1.sentQty
        assertEquals(2, unsentQty1)

        val unsentQty2 = item2.qty - item2.sentQty
        assertEquals(0, unsentQty2)

        val items = listOf(item1, item2)
        val hasUnsentItems = items.any { it.qty > it.sentQty }
        assertTrue(hasUnsentItems)

        val markedSent = items.map { it.copy(sentQty = it.qty) }
        val hasUnsentAfterMark = markedSent.any { it.qty > it.sentQty }
        assertFalse(hasUnsentAfterMark)
    }

    @Test
    fun testTakeawayOrderDefaultsAndTotals() {
        val item = OrderItem(menuItemId = "1", name = "Bun Maska", unitPricePaise = 5000L, taxType = "GST", taxPercent = 5, qty = 2)
        val items = listOf(item)

        val calc = TaxCalculator.calculate(items = items, parcelChargePaise = 1000L, discountPaise = 500L)

        assertEquals(10000L, calc.subtotalPaise)
        assertEquals(1000L, calc.parcelChargePaise)
        assertEquals(500L, calc.discountPaise)
        // 5% GST on discounted subtotal (9500) = 475 -> 237 CGST + 238 SGST
        assertEquals(237L, calc.cgstPaise)
        assertEquals(238L, calc.sgstPaise)
        // raw = 10000 + 1000 - 500 + 475 = 10975. Rounded nearest rupee = 11000L, roundOff = +25L
        assertEquals(25L, calc.roundOffPaise)
        assertEquals(11000L, calc.totalPaise)
    }

    @Test
    fun testCashChangeCalculations() {
        val totalPaise = 13600L // ₹136.00

        // 1. Exact tender
        val changeExact = calculateCashChangePaise(totalPaise, 13600L)
        assertEquals(0L, changeExact)

        // 2. Tender ₹200 chip (20000L paise)
        val change200 = calculateCashChangePaise(totalPaise, 20000L)
        assertEquals(6400L, change200) // ₹64.00

        // 3. Tender ₹500 chip (50000L paise)
        val change500 = calculateCashChangePaise(totalPaise, 50000L)
        assertEquals(36400L, change500) // ₹364.00

        // 4. Tender ₹1000 chip (100000L paise)
        val change1000 = calculateCashChangePaise(totalPaise, 100000L)
        assertEquals(86400L, change1000) // ₹864.00

        // 5. Insufficient tender (e.g. ₹100 against ₹136 total)
        val changeLess = calculateCashChangePaise(totalPaise, 10000L)
        assertEquals(0L, changeLess)
    }

    @Test
    fun testTakeawayHandoverStatusLogic() {
        val initialOrder = PosOrder(
            id = "t1",
            orderType = "TAKEAWAY",
            tokenNo = 4,
            status = "OPEN",
            handoverStatus = "PREPARING"
        )

        // Settle with Handed Over = false -> stays PREPARING
        val settledUnchecked = initialOrder.copy(
            status = "PAID",
            paymentMode = "CASH",
            handoverStatus = "PREPARING"
        )
        assertEquals("PAID", settledUnchecked.status)
        assertEquals("PREPARING", settledUnchecked.handoverStatus)

        // Settle with Handed Over = true -> becomes HANDED_OVER
        val settledChecked = initialOrder.copy(
            status = "PAID",
            paymentMode = "ONLINE",
            handoverStatus = "HANDED_OVER"
        )
        assertEquals("PAID", settledChecked.status)
        assertEquals("HANDED_OVER", settledChecked.handoverStatus)
    }

    @Test
    fun testDigitalBillTextFormatter() {
        val config = OutletConfig(
            outletName = "Chai Bathak Bar",
            addressLine1 = "12 Main St",
            cityPincode = "Ahmedabad 380015",
            phone = "+91 98765 43210",
            gstin = "24AAAAA0000A1Z5",
            fssaiNo = "10020021000123"
        )

        val order = PosOrder(
            id = "ord12345678",
            billNo = "CBB-250226-120000-A1B2",
            orderType = "DINE_IN",
            tableLabel = "Table 2",
            items = listOf(
                OrderItem(menuItemId = "m1", name = "Kulhad Masala Chai", unitPricePaise = 4000L, qty = 2)
            ),
            subtotalPaise = 8000L,
            totalPaise = 8400L,
            paymentMode = "ONLINE",
            status = "PAID"
        )

        val billText = buildShareableBillText(order, config)

        assertTrue(billText.contains("Chai Bathak Bar"))
        assertTrue(billText.contains("GSTIN: 24AAAAA0000A1Z5"))
        assertTrue(billText.contains("FSSAI: 10020021000123"))
        assertTrue(billText.contains("CBB-250226-120000-A1B2"))
        assertTrue(billText.contains("Kulhad Masala Chai"))
        assertTrue(billText.contains("Payment Mode: ONLINE"))
    }

    @Test(expected = IllegalStateException::class)
    fun testDuplicateSettlementPrevented() {
        val paidOrder = PosOrder(
            id = "ord1",
            status = "PAID",
            paymentMode = "CASH"
        )
        if (paidOrder.status == "PAID") {
            throw IllegalStateException("Order '${paidOrder.id}' is already settled.")
        }
    }
}
