package com.pinankh.chaibethakbar.util

import com.pinankh.chaibethakbar.model.OrderItem
import java.util.Locale

/**
 * Formats monetary value in paise (Long) to Rupees string.
 * Example: 4000L -> "₹40.00", 0L -> "₹0.00", 50L -> "₹0.50", -40L -> "-₹0.40"
 */
fun Long.toRupees(): String {
    val isNegative = this < 0
    val absVal = kotlin.math.abs(this)
    val rupees = absVal / 100
    val paise = absVal % 100
    val formatted = String.format(Locale.US, "₹%d.%02d", rupees, paise)
    return if (isNegative) "-$formatted" else formatted
}

data class TaxCalculationResult(
    val subtotalPaise: Long = 0L,
    val parcelChargePaise: Long = 0L,
    val discountPaise: Long = 0L,
    val cgstPaise: Long = 0L,
    val sgstPaise: Long = 0L,
    val vatPaise: Long = 0L,
    val rawTotalPaise: Long = 0L,
    val roundOffPaise: Long = 0L,
    val totalPaise: Long = 0L
)

object TaxCalculator {
    /**
     * Pure unit-testable TaxCalculator implementing PRD Section 5.
     * All parameters and calculations use Long paise arithmetic.
     */
    fun calculate(
        items: List<OrderItem>,
        parcelChargePaise: Long = 0L,
        discountPaise: Long = 0L
    ): TaxCalculationResult {
        val rawSubtotal = items.sumOf { it.unitPricePaise * it.qty.toLong() }
        val subtotalPaise = maxOf(0L, rawSubtotal)
        val validParcelCharge = maxOf(0L, parcelChargePaise)
        val maxDiscount = subtotalPaise + validParcelCharge
        val clampedDiscount = discountPaise.coerceIn(0L, maxDiscount)
        val effectiveItemDiscount = minOf(clampedDiscount, subtotalPaise)

        var totalGst = 0L
        var vatPaise = 0L

        for (item in items) {
            if (item.qty <= 0 || item.unitPricePaise <= 0L) continue
            val lineSubtotal = item.unitPricePaise * item.qty.toLong()
            val discountedLine = if (subtotalPaise > 0L) {
                Math.round(lineSubtotal.toDouble() * (subtotalPaise - effectiveItemDiscount).toDouble() / subtotalPaise.toDouble())
            } else {
                0L
            }

            if (item.taxPercent > 0) {
                when {
                    item.taxType.equals("GST", ignoreCase = true) -> {
                        val lineGst = Math.round(discountedLine.toDouble() * item.taxPercent.toDouble() / 100.0)
                        totalGst += lineGst
                    }
                    item.taxType.equals("VAT", ignoreCase = true) -> {
                        val lineVat = Math.round(discountedLine.toDouble() * item.taxPercent.toDouble() / 100.0)
                        vatPaise += lineVat
                    }
                }
            }
        }

        val cgstPaise = totalGst / 2
        val sgstPaise = totalGst - cgstPaise

        val rawTotal = subtotalPaise + validParcelCharge - clampedDiscount + cgstPaise + sgstPaise + vatPaise

        val (roundOffPaise, totalPaise) = if (rawTotal < 0L) {
            Pair(-rawTotal, 0L)
        } else {
            val rounded = ((rawTotal + 50L) / 100L) * 100L
            val roundOff = rounded - rawTotal
            Pair(roundOff, rounded)
        }

        return TaxCalculationResult(
            subtotalPaise = subtotalPaise,
            parcelChargePaise = validParcelCharge,
            discountPaise = clampedDiscount,
            cgstPaise = cgstPaise,
            sgstPaise = sgstPaise,
            vatPaise = vatPaise,
            rawTotalPaise = rawTotal,
            roundOffPaise = roundOffPaise,
            totalPaise = totalPaise
        )
    }
}
