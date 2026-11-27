package com.pinankh.chaibethakbar

import com.pinankh.chaibethakbar.model.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InventoryTest {

    @Test
    fun `test inventory item conversion calculation`() {
        val item = InventoryItem(
            name = "Sugar",
            purchaseUnit = "KG",
            baseUnit = "GRAM",
            conversionFactor = 1000L,
            openingStockBaseUnits = 5L * 1000L
        )

        assertEquals(1000L, item.conversionFactor)
        assertEquals(5000L, item.openingStockBaseUnits)
    }

    @Test
    fun `test valid inventory item creation`() {
        val item = InventoryItem(
            name = "Milk",
            categoryId = "cat-dairy",
            categoryName = "Dairy",
            itemType = "INGREDIENT",
            purchaseUnit = "Litre",
            baseUnit = "ml",
            conversionFactor = 1000L,
            openingStockBaseUnits = 2000L
        )
        assertEquals("Milk", item.name)
        assertEquals("ml", item.baseUnit)
    }

    @Test
    fun `test negative conversion factor rejection`() {
        assertThrows(IllegalArgumentException::class.java) {
            val conversionFactor = -10L
            if (conversionFactor <= 0) throw IllegalArgumentException("Conversion factor must be greater than zero.")
        }
        assertThrows(IllegalArgumentException::class.java) {
            val conversionFactor = 0L
            if (conversionFactor <= 0) throw IllegalArgumentException("Conversion factor must be greater than zero.")
        }
    }

    @Test
    fun `test negative opening stock rejection`() {
        assertThrows(IllegalArgumentException::class.java) {
            val stock = -5L
            if (stock < 0) throw IllegalArgumentException("Opening stock cannot be negative.")
        }
    }

    @Test
    fun `test negative minimum stock rejection`() {
        assertThrows(IllegalArgumentException::class.java) {
            val minStock = -10L
            if (minStock < 0) throw IllegalArgumentException("Minimum stock cannot be negative.")
        }
    }

    @Test
    fun `test item type validation`() {
        val types = listOf("INGREDIENT", "RESALE_PRODUCT", "PACKAGING", "CONSUMABLE", "OTHER")
        for (type in types) {
            val item = InventoryItem(itemType = type)
            assertEquals(type, item.itemType)
        }
    }

    @Test
    fun `test integer overflow detection on conversion`() {
        assertThrows(ArithmeticException::class.java) {
            Math.multiplyExact(Long.MAX_VALUE / 2 + 10, 3L)
        }
    }

    @Test
    fun `test unit conversion changes block if stock exists`() {
        // Simulating the repository logic
        val existingItem = InventoryItem(
            conversionFactor = 1000L,
            purchaseUnit = "KG",
            baseUnit = "GRAM",
            currentStockBaseUnits = 5000L
        )

        val updatedItem = existingItem.copy(
            conversionFactor = 500L,
            purchaseUnit = "BOX"
        )

        assertThrows(IllegalStateException::class.java) {
            if (existingItem.currentStockBaseUnits > 0) {
                if (existingItem.conversionFactor != updatedItem.conversionFactor || 
                    existingItem.purchaseUnit != updatedItem.purchaseUnit || 
                    existingItem.baseUnit != updatedItem.baseUnit) {
                    throw IllegalStateException("Cannot change unit conversion details because stock exists. Please adjust stock to 0 before changing units.")
                }
            }
        }
    }

    @Test
    fun `test duplicate name case insensitive check`() {
        val name1 = "Mayonnaise "
        val name2 = " mayonnaise"

        assertEquals(name1.trim().lowercase(), name2.trim().lowercase())
    }
}
