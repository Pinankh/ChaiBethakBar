package com.pinankh.chaibethakbar

import com.pinankh.chaibethakbar.model.OrderItem
import com.pinankh.chaibethakbar.util.TaxCalculator
import com.pinankh.chaibethakbar.util.toRupees
import org.junit.Assert.assertEquals
import org.junit.Test

class TaxCalculatorTest {

    @Test
    fun testNoneTax() {
        val items = listOf(
            OrderItem(menuItemId = "1", name = "Chai", unitPricePaise = 4000L, taxType = "NONE", taxPercent = 0, qty = 2)
        )
        val result = TaxCalculator.calculate(items)

        assertEquals(8000L, result.subtotalPaise)
        assertEquals(0L, result.cgstPaise)
        assertEquals(0L, result.sgstPaise)
        assertEquals(0L, result.vatPaise)
        assertEquals(0L, result.roundOffPaise)
        assertEquals(8000L, result.totalPaise)
    }

    @Test
    fun testGstCalculationAndCgstSgstSplit() {
        val items = listOf(
            OrderItem(menuItemId = "1", name = "Cold Coffee", unitPricePaise = 10000L, taxType = "GST", taxPercent = 5, qty = 2)
        )
        val result = TaxCalculator.calculate(items)

        assertEquals(20000L, result.subtotalPaise)
        assertEquals(500L, result.cgstPaise) // 5% of 20000 = 1000, split 500/500
        assertEquals(500L, result.sgstPaise)
        assertEquals(0L, result.vatPaise)
        assertEquals(21000L, result.totalPaise)
    }

    @Test
    fun testGstOddSplit() {
        // Line subtotal = 2100L. 5% GST = 105L.
        val items = listOf(
            OrderItem(menuItemId = "1", name = "Item", unitPricePaise = 2100L, taxType = "GST", taxPercent = 5, qty = 1)
        )
        val result = TaxCalculator.calculate(items)

        assertEquals(2100L, result.subtotalPaise)
        assertEquals(52L, result.cgstPaise) // 105 / 2 = 52
        assertEquals(53L, result.sgstPaise) // 105 - 52 = 53
        assertEquals(105L, result.cgstPaise + result.sgstPaise)
    }

    @Test
    fun testVatCalculation() {
        val items = listOf(
            OrderItem(menuItemId = "1", name = "Beverage", unitPricePaise = 10000L, taxType = "VAT", taxPercent = 12, qty = 1)
        )
        val result = TaxCalculator.calculate(items)

        assertEquals(10000L, result.subtotalPaise)
        assertEquals(0L, result.cgstPaise)
        assertEquals(0L, result.sgstPaise)
        assertEquals(1200L, result.vatPaise)
        assertEquals(11200L, result.totalPaise)
    }

    @Test
    fun testDiscounts() {
        // Base: 10000L (₹100). 5% GST on discounted price (8000L) = 400L GST (200 CGST, 200 SGST).
        val items = listOf(
            OrderItem(menuItemId = "1", name = "Combo", unitPricePaise = 10000L, taxType = "GST", taxPercent = 5, qty = 1)
        )
        val result = TaxCalculator.calculate(items, discountPaise = 2000L)

        assertEquals(10000L, result.subtotalPaise)
        assertEquals(2000L, result.discountPaise)
        assertEquals(200L, result.cgstPaise)
        assertEquals(200L, result.sgstPaise)
        assertEquals(8400L, result.totalPaise)
    }

    @Test
    fun testParcelPackagingCharge() {
        val items = listOf(
            OrderItem(menuItemId = "1", name = "Bun Maska", unitPricePaise = 5000L, taxType = "NONE", taxPercent = 0, qty = 1)
        )
        val result = TaxCalculator.calculate(items, parcelChargePaise = 1000L)

        assertEquals(5000L, result.subtotalPaise)
        assertEquals(1000L, result.parcelChargePaise)
        assertEquals(6000L, result.totalPaise)
    }

    @Test
    fun testNearestRupeeRoundingDown() {
        // Item subtotal = 1540L (₹15.40). Tax NONE. Raw total = 1540L.
        val items = listOf(
            OrderItem(menuItemId = "1", name = "Item", unitPricePaise = 1540L, taxType = "NONE", taxPercent = 0, qty = 1)
        )
        val result = TaxCalculator.calculate(items)

        assertEquals(1540L, result.rawTotalPaise)
        assertEquals(-40L, result.roundOffPaise)
        assertEquals(1500L, result.totalPaise)
    }

    @Test
    fun testNearestRupeeRoundingUp() {
        // Item subtotal = 1550L (₹15.50). Tax NONE. Raw total = 1550L.
        val items = listOf(
            OrderItem(menuItemId = "1", name = "Item", unitPricePaise = 1550L, taxType = "NONE", taxPercent = 0, qty = 1)
        )
        val result = TaxCalculator.calculate(items)

        assertEquals(1550L, result.rawTotalPaise)
        assertEquals(50L, result.roundOffPaise)
        assertEquals(1600L, result.totalPaise)
    }

    @Test
    fun testZeroSubtotalAndEdgeCases() {
        // Empty items list
        val emptyResult = TaxCalculator.calculate(emptyList())
        assertEquals(0L, emptyResult.subtotalPaise)
        assertEquals(0L, emptyResult.totalPaise)

        // Item with 0 qty
        val zeroQtyItem = listOf(
            OrderItem(menuItemId = "1", name = "Free Item", unitPricePaise = 5000L, taxType = "GST", taxPercent = 5, qty = 0)
        )
        val zeroQtyResult = TaxCalculator.calculate(zeroQtyItem)
        assertEquals(0L, zeroQtyResult.subtotalPaise)
        assertEquals(0L, zeroQtyResult.totalPaise)

        // Discount exceeds subtotal + parcel charge
        val item = listOf(
            OrderItem(menuItemId = "1", name = "Tea", unitPricePaise = 4000L, taxType = "NONE", taxPercent = 0, qty = 1)
        )
        val excessiveDiscountResult = TaxCalculator.calculate(item, parcelChargePaise = 500L, discountPaise = 10000L)
        assertEquals(4500L, excessiveDiscountResult.discountPaise) // clamped to 4000 + 500
        assertEquals(0L, excessiveDiscountResult.totalPaise)
    }

    @Test
    fun testToRupeesFormatting() {
        assertEquals("₹40.00", 4000L.toRupees())
        assertEquals("₹0.00", 0L.toRupees())
        assertEquals("₹0.50", 50L.toRupees())
        assertEquals("-₹0.40", (-40L).toRupees())
        assertEquals("₹1.05", 105L.toRupees())
    }
}
