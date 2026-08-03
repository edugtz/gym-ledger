package com.edu.gymledger.data.repository.lookup

import com.edu.gymledger.data.remote.FoodLookupClient
import com.edu.gymledger.data.remote.FoodLookupError
import com.edu.gymledger.data.remote.FoodLookupOutcome
import com.edu.gymledger.data.remote.MonotonicTimeSource
import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.FeaturesDto
import com.edu.gymledger.data.remote.dto.GenericLookupDataDto
import com.edu.gymledger.data.remote.dto.GenericLookupItemDto
import com.edu.gymledger.data.remote.dto.NutritionPer100gDto
import com.edu.gymledger.data.remote.dto.ProvidersDto
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    }

    class FakeMonotonicTimeSource : MonotonicTimeSource {
        private var currentTime = 0L

        override fun nowMillis(): Long = currentTime

        fun advance(ms: Long) {
            currentTime += ms
        }
    }
}
