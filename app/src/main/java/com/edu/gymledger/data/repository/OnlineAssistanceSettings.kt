package com.edu.gymledger.data.repository

data class OnlineAssistanceSettings(
    val onlineFoodLookupEnabled: Boolean = false,
    val foodLookupEndpoint: String = "",
    val foodLookupApiKey: String = "",
    val usdaEnabled: Boolean = true,
    val openFoodFactsEnabled: Boolean = true,
    val safeModeEnabled: Boolean = true
)
