package com.edu.gymledger.data.remote

sealed interface FoodLookupOutcome<out T> {
    data class Success<T>(val data: T) : FoodLookupOutcome<T>
    data object Empty : FoodLookupOutcome<Nothing>
    data class Error(val reason: FoodLookupError) : FoodLookupOutcome<Nothing>
}

enum class FoodLookupError {
    Transport,
    Unauthorized,
    InvalidQuery,
    InvalidBarcode,
    LookupDisabled,
    ProviderDisabled,
    FeatureDisabled,
    BudgetExceeded,
    ConfigurationError,
    ProviderError,
    MalformedResponse
}
