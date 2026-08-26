package com.edu.gymledger.feature.nutrition

import com.edu.gymledger.data.repository.lookup.BarcodeLookupAvailability
import com.edu.gymledger.data.repository.lookup.OnlineSearchAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartFoodEntryAvailabilityUiTest {

    private val unavailableOnline = listOf(
        OnlineSearchAvailability.Disabled,
        OnlineSearchAvailability.NotConfigured,
        OnlineSearchAvailability.UsdaDisabled,
        OnlineSearchAvailability.SafeMode,
        OnlineSearchAvailability.InvalidEndpoint,
        OnlineSearchAvailability.RemoteDisabled
    )

    private val unavailableBarcode = listOf(
        BarcodeLookupAvailability.Disabled,
        BarcodeLookupAvailability.NotConfigured,
        BarcodeLookupAvailability.OpenFoodFactsDisabled,
        BarcodeLookupAvailability.SafeMode,
        BarcodeLookupAvailability.InvalidEndpoint,
        BarcodeLookupAvailability.RemoteDisabled
    )

    private fun availableOnline() =
        OnlineSearchAvailability.Available(resolvedUrl = "https://example.com", minQueryLength = 3)

    private fun availableBarcode() =
        BarcodeLookupAvailability.Available(resolvedUrl = "https://example.com")

    // --- Requirement 1: unavailable ONLINE shows the availability state, not the query helper ---

    @Test
    fun unavailableOnline_showsAvailabilityMessage() {
        for (state in unavailableOnline) {
            assertNotNull(
                "Expected an availability explanation for $state",
                SmartFoodEntryAvailabilityUi.onlineAvailabilityMessage(state, isChecking = false)
            )
        }
    }

    @Test
    fun unavailableOnline_hidesQueryLengthHelper() {
        for (state in unavailableOnline) {
            assertNull(
                "Expected no query helper for $state",
                SmartFoodEntryAvailabilityUi.onlineQueryHelperText(
                    availability = state,
                    query = "",
                    minQueryLength = 3,
                    isLoading = false,
                    hasSubmitted = false
                )
            )
        }
    }

    @Test
    fun availableOnline_showsQueryLengthHelperForShortQuery() {
        val helper = SmartFoodEntryAvailabilityUi.onlineQueryHelperText(
            availability = availableOnline(),
            query = "ab",
            minQueryLength = 3,
            isLoading = false,
            hasSubmitted = false
        )
        assertEquals("Enter at least 3 characters to search online.", helper)
    }

    @Test
    fun availableOnline_showsPressSearchHelperWhenNotSubmitted() {
        val helper = SmartFoodEntryAvailabilityUi.onlineQueryHelperText(
            availability = availableOnline(),
            query = "egg",
            minQueryLength = 3,
            isLoading = false,
            hasSubmitted = false
        )
        assertEquals("Press Search online to search.", helper)
    }

    @Test
    fun availableOnline_noHelperAfterSubmittedWithResultsCleared() {
        val helper = SmartFoodEntryAvailabilityUi.onlineQueryHelperText(
            availability = availableOnline(),
            query = "egg",
            minQueryLength = 3,
            isLoading = false,
            hasSubmitted = true
        )
        assertNull(helper)
    }

    // --- Requirement 2: unavailable BARCODE exposes its availability state ---

    @Test
    fun unavailableBarcode_exposesAvailabilityMessage() {
        for (state in unavailableBarcode) {
            assertNotNull(
                "Expected an availability explanation for $state",
                SmartFoodEntryAvailabilityUi.barcodeAvailabilityMessage(state, isChecking = false)
            )
        }
    }

    @Test
    fun availableBarcode_showsNoAvailabilityMessage() {
        assertNull(
            SmartFoodEntryAvailabilityUi.barcodeAvailabilityMessage(availableBarcode(), isChecking = false)
        )
    }

    // --- Requirement 3: messages clearly distinguish each applicable state ---

    @Test
    fun onlineMessages_areDistinctPerState() {
        val messages = unavailableOnline.map {
            SmartFoodEntryAvailabilityUi.onlineAvailabilityMessage(it, isChecking = false)
        }
        for (message in messages) {
            assertNotNull(message)
        }
        assertEquals(messages.size, messages.toSet().size)
    }

    @Test
    fun barcodeMessages_areDistinctPerState() {
        val messages = unavailableBarcode.map {
            SmartFoodEntryAvailabilityUi.barcodeAvailabilityMessage(it, isChecking = false)
        }
        for (message in messages) {
            assertNotNull(message)
        }
        assertEquals(messages.size, messages.toSet().size)
    }

    // --- Requirement 3: inputs/actions remain disabled while unavailable ---

    @Test
    fun unavailableOnline_inputAndSubmitDisabled() {
        for (state in unavailableOnline) {
            assertFalse(
                "Expected input disabled for $state",
                SmartFoodEntryAvailabilityUi.isOnlineInputEnabled(state, isLoading = false)
            )
            assertFalse(
                "Expected submit disabled for $state",
                SmartFoodEntryAvailabilityUi.isOnlineSubmitEnabled(
                    availability = state,
                    isLoading = false,
                    query = "egg",
                    minQueryLength = 3
                )
            )
        }
    }

    @Test
    fun unavailableBarcode_inputAndSubmitDisabled() {
        for (state in unavailableBarcode) {
            assertFalse(
                "Expected action disabled for $state",
                SmartFoodEntryAvailabilityUi.isBarcodeActionEnabled(state, isLoading = false)
            )
        }
    }

    @Test
    fun availableOnline_inputAndSubmitEnabled() {
        val available = availableOnline()
        assertTrue(SmartFoodEntryAvailabilityUi.isOnlineInputEnabled(available, isLoading = false))
        assertTrue(
            SmartFoodEntryAvailabilityUi.isOnlineSubmitEnabled(
                availability = available,
                isLoading = false,
                query = "egg",
                minQueryLength = 3
            )
        )
    }

    @Test
    fun availableBarcode_inputAndSubmitEnabled() {
        assertTrue(
            SmartFoodEntryAvailabilityUi.isBarcodeActionEnabled(availableBarcode(), isLoading = false)
        )
    }

    @Test
    fun loadingDisablesActions_evenWhenAvailable() {
        val available = availableOnline()
        assertFalse(SmartFoodEntryAvailabilityUi.isOnlineInputEnabled(available, isLoading = true))
        assertFalse(
            SmartFoodEntryAvailabilityUi.isOnlineSubmitEnabled(
                availability = available,
                isLoading = true,
                query = "egg",
                minQueryLength = 3
            )
        )
        assertFalse(
            SmartFoodEntryAvailabilityUi.isBarcodeActionEnabled(availableBarcode(), isLoading = true)
        )
    }
}
