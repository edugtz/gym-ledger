package com.edu.gymledger.feature.nutrition

import com.edu.gymledger.data.repository.lookup.BarcodeLookupAvailability
import com.edu.gymledger.data.repository.lookup.OnlineSearchAvailability

internal object SmartFoodEntryAvailabilityUi {

    fun onlineAvailabilityMessage(
        availability: OnlineSearchAvailability,
        isChecking: Boolean
    ): String? = when (availability) {
        is OnlineSearchAvailability.Disabled -> "Online lookup is turned off in Settings."
        is OnlineSearchAvailability.NotConfigured -> "Online lookup isn't configured. Add an API key in Settings."
        is OnlineSearchAvailability.UsdaDisabled -> "Online lookup isn't available. Enable USDA in Settings."
        is OnlineSearchAvailability.SafeMode -> "Online lookup isn't available while safe mode is on."
        is OnlineSearchAvailability.InvalidEndpoint -> "The lookup endpoint URL is invalid. Check Settings."
        is OnlineSearchAvailability.RemoteDisabled ->
            if (isChecking) null else "Online lookup is temporarily disabled."
        is OnlineSearchAvailability.Available -> null
    }

    fun barcodeAvailabilityMessage(
        availability: BarcodeLookupAvailability,
        isChecking: Boolean
    ): String? = when (availability) {
        is BarcodeLookupAvailability.Disabled -> "Barcode lookup is turned off in Settings."
        is BarcodeLookupAvailability.NotConfigured -> "Barcode lookup isn't configured. Add an API key in Settings."
        is BarcodeLookupAvailability.OpenFoodFactsDisabled -> "Barcode lookup isn't available. Enable Open Food Facts in Settings."
        is BarcodeLookupAvailability.SafeMode -> "Barcode lookup isn't available while safe mode is on."
        is BarcodeLookupAvailability.InvalidEndpoint -> "The lookup endpoint URL is invalid. Check Settings."
        is BarcodeLookupAvailability.RemoteDisabled ->
            if (isChecking) null else "Barcode lookup is temporarily disabled."
        is BarcodeLookupAvailability.Available -> null
    }

    fun isOnlineInputEnabled(
        availability: OnlineSearchAvailability,
        isLoading: Boolean
    ): Boolean = !isLoading && availability is OnlineSearchAvailability.Available

    fun isOnlineSubmitEnabled(
        availability: OnlineSearchAvailability,
        isLoading: Boolean,
        query: String,
        minQueryLength: Int
    ): Boolean = !isLoading &&
        query.trim().length >= minQueryLength &&
        availability is OnlineSearchAvailability.Available

    fun isBarcodeActionEnabled(
        availability: BarcodeLookupAvailability,
        isLoading: Boolean
    ): Boolean = !isLoading && availability is BarcodeLookupAvailability.Available

    fun onlineQueryHelperText(
        availability: OnlineSearchAvailability,
        query: String,
        minQueryLength: Int,
        isLoading: Boolean,
        hasSubmitted: Boolean
    ): String? {
        if (availability !is OnlineSearchAvailability.Available) return null
        if (isLoading) return null
        return when {
            query.trim().length < minQueryLength ->
                "Enter at least $minQueryLength characters to search online."
            !hasSubmitted -> "Press Search online to search."
            else -> null
        }
    }
}
