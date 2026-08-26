package com.edu.gymledger.data.repository.lookup

import com.edu.gymledger.data.remote.FoodLookupClient
import com.edu.gymledger.data.remote.dto.PackagedFoodLookupDataDto
import com.edu.gymledger.data.remote.FoodLookupError
import com.edu.gymledger.data.remote.FoodLookupOutcome
import com.edu.gymledger.data.remote.MonotonicTimeSource
import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.FeaturesDto
import com.edu.gymledger.data.remote.dto.GenericLookupDataDto
import com.edu.gymledger.data.remote.dto.GenericLookupItemDto
import com.edu.gymledger.data.remote.dto.NutritionPer100gDto
import com.edu.gymledger.data.remote.dto.PackagedFoodProductDto
import com.edu.gymledger.data.remote.dto.PackagedNutritionDto
import com.edu.gymledger.data.remote.dto.ProvidersDto
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteFoodLookupRepositoryTest {

    private lateinit var fakeClient: FakeFoodLookupClient
    private lateinit var fakeTimeSource: FakeMonotonicTimeSource
    private lateinit var repository: RemoteFoodLookupRepository

    private val defaultSettings = OnlineAssistanceSettings(
        onlineFoodLookupEnabled = true,
        foodLookupEndpoint = "",
        foodLookupApiKey = "test-key",
        usdaEnabled = true,
        openFoodFactsEnabled = false,
        safeModeEnabled = false
    )

    private val enabledConfig = FoodLookupConfigDto(
        onlineLookupAvailable = true,
        providers = ProvidersDto(usda = true, openFoodFacts = false),
        features = FeaturesDto(genericFoodSearch = true, barcodeLookup = false),
        minQueryLength = 3,
        safeMode = false
    )

    @Before
    fun setup() {
        fakeClient = FakeFoodLookupClient()
        fakeTimeSource = FakeMonotonicTimeSource()
        repository = RemoteFoodLookupRepository(fakeClient, fakeTimeSource)
    }

    // --- Gating tests ---

    @Test
    fun onlineFoodLookupDisabled_returnsDisabled() {
        val settings = defaultSettings.copy(onlineFoodLookupEnabled = false)
        val availability = repository.getEffectiveAvailability(settings, enabledConfig)
        assertTrue(availability is OnlineSearchAvailability.Disabled)
    }

    @Test
    fun blankApiKey_returnsNotConfigured() {
        val settings = defaultSettings.copy(foodLookupApiKey = "")
        val availability = repository.getEffectiveAvailability(settings, enabledConfig)
        assertTrue(availability is OnlineSearchAvailability.NotConfigured)
    }

    @Test
    fun usdaDisabled_returnsUsdaDisabled() {
        val settings = defaultSettings.copy(usdaEnabled = false)
        val availability = repository.getEffectiveAvailability(settings, enabledConfig)
        assertTrue(availability is OnlineSearchAvailability.UsdaDisabled)
    }

    @Test
    fun safeModeEnabled_returnsSafeMode() {
        val settings = defaultSettings.copy(safeModeEnabled = true)
        val availability = repository.getEffectiveAvailability(settings, enabledConfig)
        assertTrue(availability is OnlineSearchAvailability.SafeMode)
    }

    @Test
    fun invalidEndpoint_returnsInvalidEndpoint() {
        val settings = defaultSettings.copy(foodLookupEndpoint = "http://insecure.com")
        val availability = repository.getEffectiveAvailability(settings, enabledConfig)
        assertTrue(availability is OnlineSearchAvailability.InvalidEndpoint)
    }

    @Test
    fun remoteSafeMode_returnsRemoteDisabled() {
        val config = enabledConfig.copy(safeMode = true)
        val availability = repository.getEffectiveAvailability(defaultSettings, config)
        assertTrue(availability is OnlineSearchAvailability.RemoteDisabled)
    }

    @Test
    fun conservativeConfig_disablesLookup() {
        val conservative = FoodLookupConfigDto(
            onlineLookupAvailable = false,
            providers = ProvidersDto(usda = false),
            features = FeaturesDto(genericFoodSearch = false),
            minQueryLength = 3,
            safeMode = true
        )
        val availability = repository.getEffectiveAvailability(defaultSettings, conservative)
        assertTrue(availability is OnlineSearchAvailability.RemoteDisabled)
    }

    @Test
    fun remoteProviderDisabled_returnsRemoteDisabled() {
        val config = enabledConfig.copy(providers = ProvidersDto(usda = false))
        val availability = repository.getEffectiveAvailability(defaultSettings, config)
        assertTrue(availability is OnlineSearchAvailability.RemoteDisabled)
    }

    @Test
    fun remoteFeatureDisabled_returnsRemoteDisabled() {
        val config = enabledConfig.copy(features = FeaturesDto(genericFoodSearch = false))
        val availability = repository.getEffectiveAvailability(defaultSettings, config)
        assertTrue(availability is OnlineSearchAvailability.RemoteDisabled)
    }

    @Test
    fun allGatesPass_returnsAvailable() {
        val availability = repository.getEffectiveAvailability(defaultSettings, enabledConfig)
        assertTrue(availability is OnlineSearchAvailability.Available)
        assertEquals(3, (availability as OnlineSearchAvailability.Available).minQueryLength)
    }

    @Test
    fun endpointBlank_usesDefaultUrl() {
        val availability = repository.getEffectiveAvailability(defaultSettings, enabledConfig)
        assertTrue(availability is OnlineSearchAvailability.Available)
        val url = (availability as OnlineSearchAvailability.Available).resolvedUrl
        assertTrue(url.contains("gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev"))
    }

    @Test
    fun endpointValid_usesCustomUrl() {
        val settings = defaultSettings.copy(foodLookupEndpoint = "https://custom.example.com/api/")
        val availability = repository.getEffectiveAvailability(settings, enabledConfig)
        assertTrue(availability is OnlineSearchAvailability.Available)
        val url = (availability as OnlineSearchAvailability.Available).resolvedUrl
        assertTrue(url.contains("custom.example.com"))
    }

    // --- Config cache tests ---

    @Test
    fun configCache_firstFetch_fetchesFromClient() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)

        val config = repository.ensureConfig(defaultSettings)

        assertEquals(enabledConfig, config)
        assertEquals(1, fakeClient.fetchConfigCallCount)
    }

    @Test
    fun configCache_reuseBeforeExpiry_doesNotRefetch() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)

        repository.ensureConfig(defaultSettings)
        repository.ensureConfig(defaultSettings)

        assertEquals(1, fakeClient.fetchConfigCallCount)
    }

    @Test
    fun configCache_refetchAfterExpiry_fetchesAgain() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)

        repository.ensureConfig(defaultSettings)
        fakeTimeSource.advance(5 * 60 * 1000L + 1)
        repository.ensureConfig(defaultSettings)

        assertEquals(2, fakeClient.fetchConfigCallCount)
    }

    @Test
    fun configCache_endpointChange_doesNotReuseOtherEndpointConfig() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)

        repository.ensureConfig(defaultSettings)
        val customSettings = defaultSettings.copy(foodLookupEndpoint = "https://custom.example.com/api/")
        repository.ensureConfig(customSettings)

        assertEquals(2, fakeClient.fetchConfigCallCount)
        assertEquals(
            "https://custom.example.com/api/",
            fakeClient.fetchConfigBaseUrls.last()
        )
    }

    @Test
    fun configCache_resetConfigCache_clearsCache() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)

        repository.ensureConfig(defaultSettings)
        repository.resetConfigCache()
        repository.ensureConfig(defaultSettings)

        assertEquals(2, fakeClient.fetchConfigCallCount)
    }

    @Test
    fun configFetchFailure_returnsConservativeConfig() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Error(FoodLookupError.Transport)

        val config = repository.ensureConfig(defaultSettings)

        assertFalse(config.onlineLookupAvailable)
        assertTrue(config.safeMode)
        assertEquals(3, config.minQueryLength)
    }

    // --- Search tests ---

    @Test
    fun searchGeneric_shortQuery_returnsInvalidQuery() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)

        val result = repository.searchGeneric(defaultSettings, "ab")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.InvalidQuery, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun searchGeneric_enabledValid_returnsSuccess() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)
        fakeClient.searchResult = FoodLookupOutcome.Success(genericData(
            results = listOf(
                GenericLookupItemDto(
                    externalId = "usda:123",
                    name = "Egg",
                    description = "Egg",
                    dataType = "survey_fndds_food",
                    nutritionPer100g = NutritionPer100gDto(
                        caloriesKcal = 143.0,
                        proteinG = 12.6,
                        carbohydrateG = 0.7,
                        fatG = 9.5
                    )
                )
            )
        ))

        val result = repository.searchGeneric(defaultSettings, "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        assertEquals(1, (result as FoodLookupOutcome.Success).data.size)
        assertEquals("Egg", result.data[0].name)
    }

    @Test
    fun searchGeneric_responseMetadataPreserved() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)
        fakeClient.searchResult = FoodLookupOutcome.Success(genericData(
            source = "USDA",
            attribution = "USDA FoodData Central",
            isApproximate = true,
            results = listOf(
                GenericLookupItemDto(
                    externalId = "usda:123",
                    name = "Egg",
                    description = "Egg",
                    dataType = "survey_fndds_food",
                    nutritionPer100g = NutritionPer100gDto(
                        caloriesKcal = 143.0,
                        proteinG = 12.6,
                        carbohydrateG = 0.7,
                        fatG = 9.5
                    )
                )
            )
        ))

        val result = repository.searchGeneric(defaultSettings, "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        val item = (result as FoodLookupOutcome.Success).data[0]
        assertEquals("USDA", item.source)
        assertEquals("USDA FoodData Central", item.attribution)
        assertTrue(item.isApproximate)
    }

    @Test
    fun searchGeneric_twoResults_distinctExternalIdsPreserved() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)
        fakeClient.searchResult = FoodLookupOutcome.Success(genericData(
            results = listOf(
                GenericLookupItemDto(
                    externalId = "usda:171287",
                    name = "Whole egg, large",
                    description = "Whole egg, large",
                    dataType = "survey_fndds_food",
                    nutritionPer100g = NutritionPer100gDto(143.0, 12.6, 0.7, 9.5)
                ),
                GenericLookupItemDto(
                    externalId = "usda:171286",
                    name = "Egg, whole, cooked",
                    description = "Egg, whole, cooked",
                    dataType = "survey_fndds_food",
                    nutritionPer100g = NutritionPer100gDto(155.0, 12.5, 1.1, 10.6)
                )
            )
        ))

        val result = repository.searchGeneric(defaultSettings, "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        val items = (result as FoodLookupOutcome.Success).data
        assertEquals(2, items.size)
        assertEquals("usda:171287", items[0].externalId)
        assertEquals("usda:171286", items[1].externalId)
        assertNotEquals(items[0].externalId, items[1].externalId)
    }

    @Test
    fun searchGeneric_emptyProviderResults_returnsEmpty() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)
        fakeClient.searchResult = FoodLookupOutcome.Success(genericData())

        val result = repository.searchGeneric(defaultSettings, "egg")

        assertTrue(result is FoodLookupOutcome.Empty)
    }

    @Test
    fun searchGeneric_allNutrientsNull_returnsEmpty() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)
        fakeClient.searchResult = FoodLookupOutcome.Success(genericData(
            results = listOf(
                GenericLookupItemDto(
                    externalId = "usda:123",
                    name = "Unknown",
                    description = "Unknown",
                    dataType = "survey_fndds_food",
                    nutritionPer100g = NutritionPer100gDto()
                )
            )
        ))

        val result = repository.searchGeneric(defaultSettings, "unknown")

        assertTrue(result is FoodLookupOutcome.Empty)
    }

    @Test
    fun searchGeneric_transportError_returnsError() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)
        fakeClient.searchResult = FoodLookupOutcome.Error(FoodLookupError.Transport)

        val result = repository.searchGeneric(defaultSettings, "egg")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Transport, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun searchGeneric_notConfigured_returnsError() = runTest {
        val settings = defaultSettings.copy(foodLookupApiKey = "")
        val result = repository.searchGeneric(settings, "egg")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Unauthorized, (result as FoodLookupOutcome.Error).reason)
    }

    // --- Barcode lookup gates (zero client calls) ---

    @Test
    fun lookupBarcode_offlineDisabled_returnsTransport() = runTest {
        val settings = barcodeSettings().copy(onlineFoodLookupEnabled = false)

        val result = repository.lookupBarcode(settings, "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Transport, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_blankKey_returnsUnauthorized() = runTest {
        val settings = barcodeSettings().copy(foodLookupApiKey = "")

        val result = repository.lookupBarcode(settings, "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Unauthorized, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_offProviderDisabled_returnsProviderDisabled() = runTest {
        val settings = barcodeSettings().copy(openFoodFactsEnabled = false)

        val result = repository.lookupBarcode(settings, "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderDisabled, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_localSafeMode_returnsLookupDisabled() = runTest {
        val settings = barcodeSettings().copy(safeModeEnabled = true)

        val result = repository.lookupBarcode(settings, "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_invalidEndpoint_returnsTransport() = runTest {
        val settings = barcodeSettings().copy(foodLookupEndpoint = "http://insecure.com")

        val result = repository.lookupBarcode(settings, "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Transport, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_invalidBarcode_returnsInvalidBarcodeBeforeConfigFetch() = runTest {
        val result = repository.lookupBarcode(barcodeSettings(), "1234")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.InvalidBarcode, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    // --- Barcode remote config gates ---

    @Test
    fun lookupBarcode_remoteSafeMode_returnsLookupDisabled() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig().copy(safeMode = true))

        val result = repository.lookupBarcode(barcodeSettings(), "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_remoteLookupUnavailable_returnsLookupDisabled() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig().copy(onlineLookupAvailable = false))

        val result = repository.lookupBarcode(barcodeSettings(), "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_remoteOffProviderDisabled_returnsLookupDisabled() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(
            barcodeConfig().copy(providers = ProvidersDto(usda = false, openFoodFacts = false))
        )

        val result = repository.lookupBarcode(barcodeSettings(), "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_remoteFeatureDisabled_returnsLookupDisabled() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(
            barcodeConfig().copy(features = FeaturesDto(genericFoodSearch = false, barcodeLookup = false))
        )

        val result = repository.lookupBarcode(barcodeSettings(), "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    // --- Barcode flow behavior ---

    @Test
    fun lookupBarcode_configCacheReused_acrossTwoLookups() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedData())

        repository.lookupBarcode(barcodeSettings(), "0123456789012")
        repository.lookupBarcode(barcodeSettings(), "0123456789013")

        assertEquals(1, fakeClient.fetchConfigCallCount)
        assertEquals(2, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_passesNormalizedArgsToClient() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedData())

        val result = repository.lookupBarcode(barcodeSettings(), " 0123456789012 ")

        assertTrue(result is FoodLookupOutcome.Success)
        assertTrue(
            fakeClient.lastLookupBarcodeBaseUrl!!
                .contains("gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev")
        )
        assertEquals("test-key", fakeClient.lastLookupBarcodeApiKey)
        assertEquals("0123456789012", fakeClient.lastLookupBarcodeArg)
        assertEquals("0123456789012", (result as FoodLookupOutcome.Success).data.barcode)
    }

    @Test
    fun lookupBarcode_success_mapsRemoteResult() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedData())

        val result = repository.lookupBarcode(barcodeSettings(), "0123456789012")

        assertTrue(result is FoodLookupOutcome.Success)
        val data = (result as FoodLookupOutcome.Success).data
        assertEquals("0123456789012", data.barcode)
        assertEquals("Hazelnut spread", data.name)
        assertEquals("OPEN_FOOD_FACTS", data.source)
        assertEquals("Open Food Facts — ODbL", data.attribution)
        assertEquals(544, data.caloriesPer100g!!)
        assertEquals(4.0, data.proteinPer100g!!, 0.001)
        assertEquals(57.0, data.carbohydratePer100g!!, 0.001)
        assertEquals(32.0, data.fatPer100g!!, 0.001)
        assertTrue(data.hasCompleteNutrition)
        assertEquals(1, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun lookupBarcode_unknownProduct_returnsEmpty() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Empty

        val result = repository.lookupBarcode(barcodeSettings(), "9999999999999")

        assertTrue(result is FoodLookupOutcome.Empty)
    }

    @Test
    fun lookupBarcode_providerError_returnsError() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Error(FoodLookupError.ProviderError)

        val result = repository.lookupBarcode(barcodeSettings(), "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_incompleteProduct_returnsSuccessWithNullNutrition() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(
            packagedData(barcode = "1234567890123", caloriesKcal = 210.0, proteinG = null, fatG = null)
        )

        val result = repository.lookupBarcode(barcodeSettings(), "1234567890123")

        assertTrue(result is FoodLookupOutcome.Success)
        val data = (result as FoodLookupOutcome.Success).data
        assertFalse(data.hasCompleteNutrition)
        assertEquals(210, data.caloriesPer100g!!)
        assertNull(data.proteinPer100g)
        assertEquals(57.0, data.carbohydratePer100g!!, 0.001)
        assertNull(data.fatPer100g)
    }

    @Test
    fun lookupBarcode_namelessProduct_returnsEmpty() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(
            PackagedFoodLookupDataDto(
                barcode = "0123456789012",
                source = "OPEN_FOOD_FACTS",
                attribution = "Open Food Facts — ODbL",
                isApproximate = false,
                product = PackagedFoodProductDto(
                    externalId = "0123456789012",
                    name = null,
                    genericName = null,
                    nutritionPer100g = PackagedNutritionDto(200.0, 5.0, 30.0, 10.0)
                )
            )
        )

        val result = repository.lookupBarcode(barcodeSettings(), "0123456789012")

        assertTrue(result is FoodLookupOutcome.Empty)
        assertEquals(1, fakeClient.lookupBarcodeCallCount)
    }

    // --- getBarcodeAvailability state tests ---

    @Test
    fun barcodeAvailability_offlineDisabled_returnsDisabled() {
        val settings = barcodeSettings().copy(onlineFoodLookupEnabled = false)
        val availability = repository.getBarcodeAvailability(settings, barcodeConfig())
        assertTrue(availability is BarcodeLookupAvailability.Disabled)
    }

    @Test
    fun barcodeAvailability_blankKey_returnsNotConfigured() {
        val settings = barcodeSettings().copy(foodLookupApiKey = "")
        val availability = repository.getBarcodeAvailability(settings, barcodeConfig())
        assertTrue(availability is BarcodeLookupAvailability.NotConfigured)
    }

    @Test
    fun barcodeAvailability_offProviderDisabled_returnsOpenFoodFactsDisabled() {
        val settings = barcodeSettings().copy(openFoodFactsEnabled = false)
        val availability = repository.getBarcodeAvailability(settings, barcodeConfig())
        assertTrue(availability is BarcodeLookupAvailability.OpenFoodFactsDisabled)
    }

    @Test
    fun barcodeAvailability_localSafeMode_returnsSafeMode() {
        val settings = barcodeSettings().copy(safeModeEnabled = true)
        val availability = repository.getBarcodeAvailability(settings, barcodeConfig())
        assertTrue(availability is BarcodeLookupAvailability.SafeMode)
    }

    @Test
    fun barcodeAvailability_invalidEndpoint_returnsInvalidEndpoint() {
        val settings = barcodeSettings().copy(foodLookupEndpoint = "http://insecure.com")
        val availability = repository.getBarcodeAvailability(settings, barcodeConfig())
        assertTrue(availability is BarcodeLookupAvailability.InvalidEndpoint)
    }

    @Test
    fun barcodeAvailability_remoteFlagsOff_returnsRemoteDisabled() {
        val config = barcodeConfig().copy(onlineLookupAvailable = false)
        val availability = repository.getBarcodeAvailability(barcodeSettings(), config)
        assertTrue(availability is BarcodeLookupAvailability.RemoteDisabled)
    }

    @Test
    fun barcodeAvailability_allGatesPass_returnsAvailable() {
        val availability = repository.getBarcodeAvailability(barcodeSettings(), barcodeConfig())
        assertTrue(availability is BarcodeLookupAvailability.Available)
        assertTrue(
            (availability as BarcodeLookupAvailability.Available).resolvedUrl
                .contains("gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev")
        )
    }

    // --- Helpers ---

    private fun genericData(
        source: String = "USDA",
        attribution: String = "USDA FoodData Central",
        isApproximate: Boolean = true,
        results: List<GenericLookupItemDto> = emptyList()
    ) = GenericLookupDataDto(
        query = "egg",
        source = source,
        attribution = attribution,
        isApproximate = isApproximate,
        results = results
    )

    private fun barcodeSettings() = OnlineAssistanceSettings(
        onlineFoodLookupEnabled = true,
        foodLookupEndpoint = "",
        foodLookupApiKey = "test-key",
        usdaEnabled = true,
        openFoodFactsEnabled = true,
        safeModeEnabled = false
    )

    private fun barcodeConfig() = FoodLookupConfigDto(
        onlineLookupAvailable = true,
        providers = ProvidersDto(usda = false, openFoodFacts = true),
        features = FeaturesDto(genericFoodSearch = false, barcodeLookup = true),
        minQueryLength = 3,
        safeMode = false
    )

    private fun packagedData(
        barcode: String = "0123456789012",
        caloriesKcal: Double? = 544.0,
        proteinG: Double? = 4.0,
        carbohydrateG: Double? = 57.0,
        fatG: Double? = 32.0
    ) = PackagedFoodLookupDataDto(
        barcode = barcode,
        source = "OPEN_FOOD_FACTS",
        attribution = "Open Food Facts — ODbL",
        isApproximate = true,
        product = PackagedFoodProductDto(
            externalId = barcode,
            name = "Hazelnut spread",
            nutritionPer100g = PackagedNutritionDto(caloriesKcal, proteinG, carbohydrateG, fatG)
        )
    )

    private fun assertFalse(value: Boolean) {
        org.junit.Assert.assertFalse(value)
    }

    class FakeFoodLookupClient : FoodLookupClient {
        var configResult: FoodLookupOutcome<FoodLookupConfigDto> = FoodLookupOutcome.Error(FoodLookupError.Transport)
        var searchResult: FoodLookupOutcome<GenericLookupDataDto> = FoodLookupOutcome.Success(GenericLookupDataDto())
        var fetchConfigCallCount = 0
            private set
        var fetchConfigBaseUrls: MutableList<String> = mutableListOf()
            private set
        var searchGenericCallCount = 0
            private set
        var barcodeResult: FoodLookupOutcome<PackagedFoodLookupDataDto> = FoodLookupOutcome.Empty
        var lookupBarcodeCallCount = 0
            private set
        var lastLookupBarcodeBaseUrl: String? = null
            private set
        var lastLookupBarcodeApiKey: String? = null
            private set
        var lastLookupBarcodeArg: String? = null
            private set

        override suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<FoodLookupConfigDto> {
            fetchConfigCallCount++
            fetchConfigBaseUrls.add(baseUrl)
            return configResult
        }

        override suspend fun searchGeneric(
            baseUrl: String,
            apiKey: String,
            query: String
        ): FoodLookupOutcome<GenericLookupDataDto> {
            searchGenericCallCount++
            return searchResult
        }

        override suspend fun lookupBarcode(
            baseUrl: String,
            apiKey: String,
            barcode: String
        ): FoodLookupOutcome<PackagedFoodLookupDataDto> {
            lookupBarcodeCallCount++
            lastLookupBarcodeBaseUrl = baseUrl
            lastLookupBarcodeApiKey = apiKey
            lastLookupBarcodeArg = barcode
            return barcodeResult
        }
    }

    class FakeMonotonicTimeSource : MonotonicTimeSource {
        private var currentTime = 0L

        override fun nowMillis(): Long = currentTime

        fun advance(ms: Long) {
            currentTime += ms
        }
    }
}
