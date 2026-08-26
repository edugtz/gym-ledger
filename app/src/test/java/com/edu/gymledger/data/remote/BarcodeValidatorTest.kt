package com.edu.gymledger.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeValidatorTest {
    @Test fun acceptsSupportedLengthsAndPreservesLeadingZero() {
        assertEquals("0123456789012", BarcodeValidator.normalize(" 0123456789012 "))
        assertEquals("12345678", BarcodeValidator.normalize("12345678"))
        assertEquals("123456789012", BarcodeValidator.normalize("123456789012"))
        assertEquals("12345678901234", BarcodeValidator.normalize("12345678901234"))
    }

    @Test fun rejectsInvalidInput() {
        assertNull(BarcodeValidator.normalize("1234"))
        assertNull(BarcodeValidator.normalize("ABC12345"))
        assertNull(BarcodeValidator.normalize("1234 5678"))
    }

    @Test fun rejectsBlankAndWhitespace() {
        assertNull(BarcodeValidator.normalize(""))
        assertNull(BarcodeValidator.normalize("   "))
        assertNull(BarcodeValidator.normalize("\t \n"))
    }

    @Test fun rejectsNonAsciiDigits() {
        assertNull(BarcodeValidator.normalize("٠١٢٣٤٥٦٧٨٩٠١٢"))
        assertNull(BarcodeValidator.normalize("０１２３４５６７８９０１２"))
    }

    @Test fun rejectsUnsupportedLengths() {
        assertNull(BarcodeValidator.normalize("12345678901"))
        assertNull(BarcodeValidator.normalize("123456789012345"))
    }
}
