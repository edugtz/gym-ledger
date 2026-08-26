package com.edu.gymledger.data.repository.lookup

import com.edu.gymledger.data.remote.BarcodeValidator
import com.edu.gymledger.data.remote.EndpointResult
import com.edu.gymledger.data.remote.EndpointValidator
import com.edu.gymledger.data.remote.FoodLookupClient
import com.edu.gymledger.data.remote.FoodLookupError
import com.edu.gymledger.data.remote.FoodLookupOutcome
import com.edu.gymledger.data.remote.MonotonicTimeSource
import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.GenericLookupDataDto
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import com.edu.gymledger.domain.model.lookup.RemoteFoodLookupResult
import com.edu.gymledger.domain.model.lookup.RemoteFoodReferenceMapper.toRemoteResultOrNull
import com.edu.gymledger.domain.model.lookup.RemotePackagedFoodResult
import com.edu.gymledger.domain.model.lookup.PackagedFoodReferenceMapper.toRemoteResultOrNull

open class RemoteFoodLookupRepository(
    private val client: FoodLookupClient,
    private val timeSource: MonotonicTimeSource
) {

    private var cachedConfig: FoodLookupConfigDto? = null
    private var cachedEndpoint: String? = null
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
        val endpoint = EndpointValidator.resolve(settings.foodLookupEndpoint)
        if (endpoint is EndpointResult.Invalid) return conservativeConfig()

        val baseUrl = (endpoint as EndpointResult.Valid).url.toString()

        val now = timeSource.nowMillis()
        val cached = cachedConfig
        if (cached != null && cachedEndpoint == baseUrl && (now - configFetchTime) < configCacheDurationMs) {
            return cached
        }

        return when (val outcome = client.fetchConfig(baseUrl)) {
            is FoodLookupOutcome.Success -> {
                cachedConfig = outcome.data
                cachedEndpoint = baseUrl
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
                val data: GenericLookupDataDto = outcome.data

                val results = data.results.mapNotNull { dto ->
                    dto.toRemoteResultOrNull(data.source, data.attribution, data.isApproximate)
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

    open fun getBarcodeAvailability(
        settings: OnlineAssistanceSettings,
        config: FoodLookupConfigDto?
    ): BarcodeLookupAvailability {
        if (!settings.onlineFoodLookupEnabled) return BarcodeLookupAvailability.Disabled
        if (settings.foodLookupApiKey.isBlank()) return BarcodeLookupAvailability.NotConfigured
        if (!settings.openFoodFactsEnabled) return BarcodeLookupAvailability.OpenFoodFactsDisabled
        if (settings.safeModeEnabled) return BarcodeLookupAvailability.SafeMode
        val endpoint = EndpointValidator.resolve(settings.foodLookupEndpoint)
        if (endpoint is EndpointResult.Invalid) return BarcodeLookupAvailability.InvalidEndpoint
        val resolvedUrl = (endpoint as EndpointResult.Valid).url.toString()
        if (config != null && !config.safeMode && config.onlineLookupAvailable &&
            config.providers.openFoodFacts && config.features.barcodeLookup
        ) return BarcodeLookupAvailability.Available(resolvedUrl)
        return BarcodeLookupAvailability.RemoteDisabled
    }

    open suspend fun lookupBarcode(
        settings: OnlineAssistanceSettings,
        barcode: String
    ): FoodLookupOutcome<RemotePackagedFoodResult> {
        val normalizedBarcode = BarcodeValidator.normalize(barcode)
            ?: return FoodLookupOutcome.Error(FoodLookupError.InvalidBarcode)
        val endpoint = EndpointValidator.resolve(settings.foodLookupEndpoint)
        if (!settings.onlineFoodLookupEnabled) return FoodLookupOutcome.Error(FoodLookupError.Transport)
        if (settings.foodLookupApiKey.isBlank()) return FoodLookupOutcome.Error(FoodLookupError.Unauthorized)
        if (!settings.openFoodFactsEnabled) return FoodLookupOutcome.Error(FoodLookupError.ProviderDisabled)
        if (settings.safeModeEnabled) return FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
        if (endpoint is EndpointResult.Invalid) return FoodLookupOutcome.Error(FoodLookupError.Transport)
        val config = ensureConfig(settings)
        when (val availability = getBarcodeAvailability(settings, config)) {
            is BarcodeLookupAvailability.Available -> Unit
            is BarcodeLookupAvailability.OpenFoodFactsDisabled -> return FoodLookupOutcome.Error(FoodLookupError.ProviderDisabled)
            is BarcodeLookupAvailability.RemoteDisabled -> return FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
            is BarcodeLookupAvailability.SafeMode -> return FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
            is BarcodeLookupAvailability.Disabled -> return FoodLookupOutcome.Error(FoodLookupError.Transport)
            is BarcodeLookupAvailability.NotConfigured -> return FoodLookupOutcome.Error(FoodLookupError.Unauthorized)
            is BarcodeLookupAvailability.InvalidEndpoint -> return FoodLookupOutcome.Error(FoodLookupError.Transport)
        }
        val baseUrl = (endpoint as EndpointResult.Valid).url.toString()
        return when (val outcome = client.lookupBarcode(baseUrl, settings.foodLookupApiKey, normalizedBarcode)) {
            is FoodLookupOutcome.Success -> outcome.data.toRemoteResultOrNull()?.let { FoodLookupOutcome.Success(it) }
                ?: FoodLookupOutcome.Empty
            is FoodLookupOutcome.Empty -> FoodLookupOutcome.Empty
            is FoodLookupOutcome.Error -> outcome
        }
    }

    fun resetConfigCache() {
        cachedConfig = null
        cachedEndpoint = null
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

sealed interface BarcodeLookupAvailability {
    data object Disabled : BarcodeLookupAvailability
    data object NotConfigured : BarcodeLookupAvailability
    data object OpenFoodFactsDisabled : BarcodeLookupAvailability
    data object SafeMode : BarcodeLookupAvailability
    data object InvalidEndpoint : BarcodeLookupAvailability
    data object RemoteDisabled : BarcodeLookupAvailability
    data class Available(val resolvedUrl: String) : BarcodeLookupAvailability
}
