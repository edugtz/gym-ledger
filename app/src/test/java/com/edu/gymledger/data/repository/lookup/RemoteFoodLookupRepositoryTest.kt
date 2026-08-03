package com.edu.gymledger.data.repository.lookup

import com.edu.gymledger.data.remote.FoodLookupClient
import com.edu.gymledger.data.remote.FoodLookupError
import com.edu.gymledger.data.remote.FoodLookupOutcome
import com.edu.gymledger.data.remote.MonotonicTimeSource
import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.FeaturesDto
import com.edu.gymledger.data.remote.dto.GenericLookupItemDto
import com.edu.gymledger.data.remote.dto.NutritionPer100gDto
import com.edu.gymledger.data.remote.dto.ProvidersDto
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        fakeClient.searchResult = FoodLookupOutcome.Success(listOf(
            GenericLookupItemDto(
                id = "usda:123",
                source = "USDA",
                type = "generic",
                name = "Egg",
                nutritionPer100g = NutritionPer100gDto(
                    caloriesKcal = 143.0,
                    proteinG = 12.6,
                    carbohydrateG = 0.7,
                    fatG = 9.5
                )
            )
        ))

        val result = repository.searchGeneric(defaultSettings, "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        assertEquals(1, (result as FoodLookupOutcome.Success).data.size)
        assertEquals("Egg", result.data[0].name)
    }

    @Test
    fun searchGeneric_emptyProviderResults_returnsEmpty() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)
        fakeClient.searchResult = FoodLookupOutcome.Success(emptyList())

        val result = repository.searchGeneric(defaultSettings, "egg")

        assertTrue(result is FoodLookupOutcome.Empty)
    }

    @Test
    fun searchGeneric_allNutrientsNull_returnsEmpty() = runTest {
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig)
        fakeClient.searchResult = FoodLookupOutcome.Success(listOf(
            GenericLookupItemDto(
                id = "usda:123",
                source = "USDA",
                type = "generic",
                name = "Unknown",
                nutritionPer100g = NutritionPer100gDto(
                    caloriesKcal = null,
                    proteinG = null,
                    carbohydrateG = null,
                    fatG = null
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

    private fun assertFalse(value: Boolean) {
        org.junit.Assert.assertFalse(value)
    }

    class FakeFoodLookupClient : FoodLookupClient {
        var configResult: FoodLookupOutcome<FoodLookupConfigDto> = FoodLookupOutcome.Error(FoodLookupError.Transport)
        var searchResult: FoodLookupOutcome<List<GenericLookupItemDto>> = FoodLookupOutcome.Success(emptyList())
        var fetchConfigCallCount = 0
            private set
        var searchGenericCallCount = 0
            private set

        override suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<FoodLookupConfigDto> {
            fetchConfigCallCount++
            return configResult
        }

        override suspend fun searchGeneric(
            baseUrl: String,
            apiKey: String,
            query: String
        ): FoodLookupOutcome<List<GenericLookupItemDto>> {
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
