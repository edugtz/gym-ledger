package com.edu.gymledger.data.remote

object BarcodeValidator {
    private val allowedLengths = setOf(8, 12, 13, 14)

    fun normalize(value: String): String? {
        val trimmed = value.trim()
        return trimmed.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } && it.length in allowedLengths }
    }
}
