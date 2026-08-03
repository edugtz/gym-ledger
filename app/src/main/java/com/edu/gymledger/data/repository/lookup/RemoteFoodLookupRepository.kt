package com.edu.gymledger.data.repository.lookup

import com.edu.gymledger.data.remote.EndpointResult
import com.edu.gymledger.data.remote.EndpointValidator
import com.edu.gymledger.data.remote.FoodLookupClient
import com.edu.gymledger.data.remote.FoodLookupError
import com.edu.gymledger.data.remote.FoodLookupOutcome
import com.edu.gymledger.data.remote.MonotonicTimeSource
import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.GenericLookupItemDto
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import com.edu.gymledger.domain.model.lookup.RemoteFoodLookupResult
import com.edu.gymledger.domain.model.lookup.RemoteFoodReferenceMapper.toRemoteResultOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

open class RemoteFoodLookupRepository(
    private val client: FoodLookupClient,
    private val timeSource: MonotonicTimeSource
) {

    private var cachedConfig: FoodLookupConfigDto? = null
    private var configFetchTime: Long = 0L

    private val configCacheDurationMs = 5 * 60 * 1000L

    open fun getEffectiveAvailability(
        settings: OnlineAssistanceSettings,
        config: FoodLookupConfigDto?
    ): OnlineSearchAvailability {
        if (!settings.onlineFoodLookupEnabled) return OnlineSearchAvailability.Disabled

        if (settings.foodLookupApiKey.isBlank()) return OnlineSearchAvailability.NotConfigured
        if (!settings.usdaEnabled) return OnlineSearchAvailability.UsdaDisabled
        if (settings.safeModeEnabled) return OnlineSearchAvailability.SafeMode

        val endpoint = EndpointValidator.resolve(settings.foodLookupEndpoint)
        if (endpoint is EndpointResult.Invalid) return OnlineSearchAvailability.InvalidEndpoint

        val resolvedUrl = (endpoint as EndpointResult.Valid).url.toString()

        if (config != null && !config.safeMode &&
            config.onlineLookupAvailable &&
            config.providers.usda &&
            config.features.genericFoodSearch
        ) {
            return OnlineSearchAvailability.Available(
                resolvedUrl = resolvedUrl,
                minQueryLength = config.minQueryLength
            )
        }

        return OnlineSearchAvailability.RemoteDisabled
    }

    open suspend fun ensureConfig(settings: OnlineAssistanceSettings): FoodLookupConfigDto {
        val now = timeSource.nowMillis()
        val cached = cachedConfig
        if (cached != null && (now - configFetchTime) < configCacheDurationMs) {
            return cached
        }

        val endpoint = EndpointValidator.resolve(settings.foodLookupEndpoint)
        if (endpoint is EndpointResult.Invalid) return conservativeConfig()

        val baseUrl = (endpoint as EndpointResult.Valid).url.toString()
        return when (val outcome = client.fetchConfig(baseUrl)) {
            is FoodLookupOutcome.Success -> {
                cachedConfig = outcome.data
                configFetchTime = now
                outcome.data
            }
            else -> conservativeConfig()
        }
    }

    open suspend fun searchGeneric(
        settings: OnlineAssistanceSettings,
        query: String
    ): FoodLookupOutcome<List<RemoteFoodLookupResult>> {
        val trimmed = query.trim()

        if (!settings.onlineFoodLookupEnabled) {
            return FoodLookupOutcome.Error(FoodLookupError.Transport)
        }
        if (settings.foodLookupApiKey.isBlank()) {
            return FoodLookupOutcome.Error(FoodLookupError.Unauthorized)
        }
        if (!settings.usdaEnabled) {
            return FoodLookupOutcome.Error(FoodLookupError.ProviderDisabled)
        }
        if (settings.safeModeEnabled) {
            return FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
        }
        val endpoint = EndpointValidator.resolve(settings.foodLookupEndpoint)
        if (endpoint is EndpointResult.Invalid) {
            return FoodLookupOutcome.Error(FoodLookupError.Transport)
        }

        val config = ensureConfig(settings)

        if (config.safeMode) {
            return FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
        }
        if (!config.onlineLookupAvailable) {
            return FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
        }
        if (!config.providers.usda) {
            return FoodLookupOutcome.Error(FoodLookupError.ProviderDisabled)
        }
        if (!config.features.genericFoodSearch) {
            return FoodLookupOutcome.Error(FoodLookupError.FeatureDisabled)
        }

        if (trimmed.length < config.minQueryLength) {
            return FoodLookupOutcome.Error(FoodLookupError.InvalidQuery)
        }

        val baseUrl = (endpoint as EndpointResult.Valid).url.toString()

        return when (val outcome = client.searchGeneric(baseUrl, settings.foodLookupApiKey, trimmed)) {
            is FoodLookupOutcome.Success -> {
                val source = "USDA"
                val attribution = "USDA FoodData Central"
                val isApproximate = true

                val results = outcome.data.mapNotNull { dto ->
                    dto.toRemoteResultOrNull(source, attribution, isApproximate)
                }

                if (results.isEmpty()) {
                    FoodLookupOutcome.Empty
                } else {
                    FoodLookupOutcome.Success(results)
                }
            }
            is FoodLookupOutcome.Empty -> FoodLookupOutcome.Empty
            is FoodLookupOutcome.Error -> outcome
        }
    }

    fun resetConfigCache() {
        cachedConfig = null
        configFetchTime = 0L
    }

    private fun conservativeConfig(): FoodLookupConfigDto {
        return FoodLookupConfigDto(
            onlineLookupAvailable = false,
            providers = com.edu.gymledger.data.remote.dto.ProvidersDto(
                usda = false,
                openFoodFacts = false
            ),
            features = com.edu.gymledger.data.remote.dto.FeaturesDto(
                genericFoodSearch = false,
                barcodeLookup = false
            ),
            minQueryLength = 3,
            safeMode = true
        )
    }

    private fun mapAvailabilityToError(availability: OnlineSearchAvailability): FoodLookupError {
        return when (availability) {
            is OnlineSearchAvailability.Disabled -> FoodLookupError.Transport
            is OnlineSearchAvailability.NotConfigured -> FoodLookupError.Unauthorized
            is OnlineSearchAvailability.UsdaDisabled -> FoodLookupError.ProviderDisabled
            is OnlineSearchAvailability.SafeMode -> FoodLookupError.LookupDisabled
            is OnlineSearchAvailability.InvalidEndpoint -> FoodLookupError.Transport
            is OnlineSearchAvailability.RemoteDisabled -> FoodLookupError.LookupDisabled
            is OnlineSearchAvailability.Available -> FoodLookupError.Transport
        }
    }
}

sealed interface OnlineSearchAvailability {
    data object Disabled : OnlineSearchAvailability
    data object NotConfigured : OnlineSearchAvailability
    data object UsdaDisabled : OnlineSearchAvailability
    data object SafeMode : OnlineSearchAvailability
    data object InvalidEndpoint : OnlineSearchAvailability
    data object RemoteDisabled : OnlineSearchAvailability
    data class Available(val resolvedUrl: String, val minQueryLength: Int) : OnlineSearchAvailability
}
