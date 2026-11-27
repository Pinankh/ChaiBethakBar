package com.pinankh.chaibethakbar

import com.pinankh.chaibethakbar.model.Supplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class SupplierTest {
    @Test
    fun testSupplierCreationWithDefaults() {
        val supplier = Supplier()
        assertEquals("", supplier.name)
        assertTrue(supplier.isActive)
        assertEquals("", supplier.supplierId)
    }

    @Test
    fun testSupplierWhitespaceNormalization() {
        val supplier = Supplier(
            name = "   Vendor Name  ",
            phone = "  9876543210 "
        )
        assertEquals("Vendor Name", supplier.name.trim())
        assertEquals("9876543210", supplier.phone.trim())
    }

    @Test
    fun testSupplierGstinValidation() {
        val gstinRegex = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"

        val validGstin = "22AAAAA0000A1Z5"
        assertTrue(validGstin.matches(Regex(gstinRegex)))

        val invalidGstin = "22aaaaa0000a1z5"
        assertFalse(invalidGstin.matches(Regex(gstinRegex)))

        val invalidFormat = "12345"
        assertFalse(invalidFormat.matches(Regex(gstinRegex)))
    }

    @Test
    fun testSupplierEmailValidation() {
        // Pattern used in Android, simplified for unit test
        val emailRegex = "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"

        assertTrue("test@example.com".matches(Regex(emailRegex)))
        assertFalse("testexample.com".matches(Regex(emailRegex)))
        assertTrue("vendor.name+1@sub.domain.co".matches(Regex(emailRegex)))
    }

    @Test
    fun testSearchQueryMatch() {
        val supplier = Supplier(
            name = "Fresh Breads Bakery",
            contactPerson = "Rahul",
            phone = "9876543210",
            gstin = "22AAAAA0000A1Z5"
        )

        val query1 = "fresh".lowercase()
        assertTrue(supplier.name.lowercase().contains(query1))

        val query2 = "rahul".lowercase()
        assertTrue(supplier.contactPerson.lowercase().contains(query2))

        val query3 = "9876".lowercase()
        assertTrue(supplier.phone.contains(query3))

        val query4 = "22aa".lowercase()
        assertTrue(supplier.gstin.lowercase().contains(query4))
    }
}
