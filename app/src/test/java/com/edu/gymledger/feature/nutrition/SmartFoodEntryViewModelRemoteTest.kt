package com.edu.gymledger.feature.nutrition

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
import com.edu.gymledger.data.repository.FoodReferenceRepository
import com.edu.gymledger.data.repository.FoodRepository
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import com.edu.gymledger.data.repository.lookup.OnlineSearchAvailability
import com.edu.gymledger.data.repository.lookup.RemoteFoodLookupRepository
import com.edu.gymledger.domain.model.lookup.RemoteFoodLookupResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmartFoodEntryViewModelRemoteTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeFoodRepository: FoodRepository
    private lateinit var fakeFoodDao: FakeFoodDao
    private lateinit var fakeReferenceRepository: FoodReferenceRepository
    private lateinit var fakeClient: FakeFoodLookupClient
    private lateinit var fakeTimeSource: FakeMonotonicTimeSource
    private lateinit var remoteRepository: RemoteFoodLookupRepository
    private lateinit var settingsFlow: MutableStateFlow<OnlineAssistanceSettings>
    private lateinit var viewModel: SmartFoodEntryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeFoodDao = FakeFoodDao()
        fakeFoodRepository = FoodRepository(fakeFoodDao)
        fakeReferenceRepository = FoodReferenceRepository()
        settingsFlow = MutableStateFlow(OnlineAssistanceSettings())
        fakeClient = FakeFoodLookupClient()
        fakeTimeSource = FakeMonotonicTimeSource()
        remoteRepository = RemoteFoodLookupRepository(fakeClient, fakeTimeSource)
        viewModel = SmartFoodEntryViewModel(
            fakeReferenceRepository,
            fakeFoodRepository,
            remoteRepository,
            settingsFlow
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun enabledSettings() = OnlineAssistanceSettings(
        onlineFoodLookupEnabled = true,
        foodLookupApiKey = "key",
        usdaEnabled = true,
        openFoodFactsEnabled = false,
        safeModeEnabled = false
    )

    @Test
    fun onlineSettingDisabled_toggleAbsent() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(onlineFoodLookupEnabled = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isOnlineAvailable)
    }

    @Test
    fun onlineSettingEnabled_toggleVisible() = runTest {
        settingsFlow.value = enabledSettings()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isOnlineAvailable)
    }

    @Test
    fun onlineEnabled_missingKey_notConfiguredState() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = ""
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isOnlineAvailable)
        assertTrue(state.onlineAvailability is OnlineSearchAvailability.NotConfigured)
    }

    @Test
    fun onlineEnabled_usdaDisabled_inlineState() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key",
            usdaEnabled = false
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.onlineAvailability is OnlineSearchAvailability.UsdaDisabled)
    }

    @Test
    fun onlineEnabled_safeModeEnabled_inlineState() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key",
            safeModeEnabled = true
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.onlineAvailability is OnlineSearchAvailability.SafeMode)
    }

    // --- Local gates must block fetchConfig (Fix 5) ---

    @Test
    fun toggleOnlineMode_onlineDisabled_noConfigCall() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(onlineFoodLookupEnabled = false)
        advanceUntilIdle()

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(0, fakeClient.fetchConfigCallCount)
    }

    @Test
    fun toggleOnlineMode_missingKey_noConfigCall() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = ""
        )
        advanceUntilIdle()

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertTrue(viewModel.uiState.value.onlineAvailability is OnlineSearchAvailability.NotConfigured)
    }

    @Test
    fun toggleOnlineMode_usdaDisabled_noConfigCall() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key",
            usdaEnabled = false
        )
        advanceUntilIdle()

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertTrue(viewModel.uiState.value.onlineAvailability is OnlineSearchAvailability.UsdaDisabled)
    }

    @Test
    fun toggleOnlineMode_safeModeEnabled_noConfigCall() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key",
            safeModeEnabled = true
        )
        advanceUntilIdle()

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertTrue(viewModel.uiState.value.onlineAvailability is OnlineSearchAvailability.SafeMode)
    }

    @Test
    fun toggleOnlineMode_invalidEndpoint_noConfigCall() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key",
            foodLookupEndpoint = "http://insecure.com",
            safeModeEnabled = false
        )
        advanceUntilIdle()

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertTrue(viewModel.uiState.value.onlineAvailability is OnlineSearchAvailability.InvalidEndpoint)
    }

    @Test
    fun toggleOnlineMode_allGatesPass_fetchesConfigOnce() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        advanceUntilIdle()

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(1, fakeClient.fetchConfigCallCount)
        assertTrue(viewModel.uiState.value.onlineAvailability is OnlineSearchAvailability.Available)
    }

    // --- Config cache survives sheet reopen (Fix 7) ---

    @Test
    fun resetState_keepsConfigCache() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        advanceUntilIdle()

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()
        assertEquals(1, fakeClient.fetchConfigCallCount)

        viewModel.resetState()
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(1, fakeClient.fetchConfigCallCount)
    }

    // --- Search behavior ---

    @Test
    fun onOnlineQueryChange_updatesQuery() = runTest {
        viewModel.onOnlineQueryChange("egg")
        assertEquals("egg", viewModel.uiState.value.onlineQuery)
    }

    @Test
    fun submitOnlineSearch_notInOnlineMode_returnsImmediately() = runTest {
        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()

        assertFalse(viewModel.uiState.value.isOnlineSearching)
    }

    @Test
    fun submitOnlineSearch_tooShortQuery_showsError() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("ab")
        viewModel.submitOnlineSearch()

        val state = viewModel.uiState.value
        assertNotNull(state.onlineError)
        assertTrue(state.onlineError!!.contains("3"))
    }

    @Test
    fun submitOnlineSearch_validQuery_showsResults() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.searchResult = FoodLookupOutcome.Success(genericData(listOf(eggItemDto())))
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isOnlineSearching)
        assertEquals(1, state.onlineResults.size)
        assertEquals("Egg", state.onlineResults[0].name)
    }

    @Test
    fun submitOnlineSearch_error_showsMessage() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.searchResult = FoodLookupOutcome.Error(FoodLookupError.Transport)
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.onlineError)
        assertTrue(state.onlineError!!.contains("connection"))
    }

    @Test
    fun submitOnlineSearch_remoteDisabled_doesNotSetError() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig().copy(safeMode = true))
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.onlineAvailability is OnlineSearchAvailability.RemoteDisabled)
        assertEquals(0, fakeClient.searchGenericCallCount)
    }

    @Test
    fun selectOnlineResult_prefillsEditableFields() = runTest {
        val result = RemoteFoodLookupResult(
            externalId = "usda:123",
            name = "Egg",
            description = null,
            dataType = null,
            source = "USDA",
            attribution = "USDA FoodData Central",
            isApproximate = true,
            caloriesPer100g = 143,
            proteinPer100g = 12.6,
            carbohydratePer100g = 0.7,
            fatPer100g = 9.5
        )

        viewModel.selectOnlineResult(result)

        val state = viewModel.uiState.value
        assertNotNull(state.selectedReference)
        assertEquals("Egg", state.nameText)
        assertEquals("143", state.caloriesText)
        assertEquals("100", state.gramsText)
    }

    @Test
    fun selectOnlineResult_remainsEditable() = runTest {
        val result = RemoteFoodLookupResult(
            externalId = "usda:123",
            name = "Egg",
            description = null,
            dataType = null,
            source = "USDA",
            attribution = "USDA FoodData Central",
            isApproximate = true,
            caloriesPer100g = 143,
            proteinPer100g = 12.6,
            carbohydratePer100g = 0.7,
            fatPer100g = 9.5
        )

        viewModel.selectOnlineResult(result)
        viewModel.onCaloriesChange("150")

        assertEquals("150", viewModel.uiState.value.caloriesText)
    }

    @Test
    fun cancelSearch_stopsSearching() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.searchResult = FoodLookupOutcome.Success(emptyData())
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        viewModel.cancelSearch()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOnlineSearching)
    }

    @Test
    fun leavingOnlineMode_cancelsInFlightSearch() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.searchGate = CompletableDeferred()
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isOnlineSearching)

        viewModel.toggleOnlineMode(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOnlineSearching)
        assertFalse(viewModel.uiState.value.onlineMode)
        fakeClient.searchGate!!.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun duplicateSubmission_prevented() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.searchResult = FoodLookupOutcome.Success(genericData(listOf(eggItemDto())))
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        viewModel.submitOnlineSearch()
        advanceUntilIdle()

        assertEquals(1, fakeClient.searchGenericCallCount)
    }

    @Test
    fun keyNeverInUiState() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "secret-key-12345"
        )
        advanceUntilIdle()

        val stateString = viewModel.uiState.value.toString()
        assertFalse(stateString.contains("secret-key-12345"))
    }

    @Test
    fun localSearch_stillWorks() = runTest {
        viewModel.onSearchQueryChange("egg")
        val results = viewModel.uiState.value.searchResults
        assertTrue(results.isNotEmpty())
    }

    // --- Helpers ---

    private fun enabledConfig() = FoodLookupConfigDto(
        onlineLookupAvailable = true,
        providers = ProvidersDto(usda = true),
        features = FeaturesDto(genericFoodSearch = true),
        minQueryLength = 3,
        safeMode = false
    )

    private fun genericData(results: List<GenericLookupItemDto>) = GenericLookupDataDto(
        query = "egg",
        source = "USDA",
        attribution = "USDA FoodData Central",
        isApproximate = true,
        results = results
    )

    private fun emptyData() = GenericLookupDataDto(
        query = "egg",
        source = "USDA",
        attribution = "USDA FoodData Central",
        isApproximate = true,
        results = emptyList()
    )

    private fun eggItemDto() = GenericLookupItemDto(
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

    // --- Handwritten fakes ---

    class FakeFoodDao : com.edu.gymledger.data.db.dao.FoodDao {
        private val stored = mutableListOf<com.edu.gymledger.data.db.entity.FoodEntity>()
        private val listAllFlow = MutableStateFlow(emptyList<com.edu.gymledger.data.db.entity.FoodEntity>())

        override suspend fun insert(food: com.edu.gymledger.data.db.entity.FoodEntity): Long {
            val id = (stored.maxOfOrNull { it.id } ?: 0L) + 1
            val inserted = food.copy(id = id)
            stored.add(inserted)
            listAllFlow.value = stored.toList()
            return id
        }

        override suspend fun update(food: com.edu.gymledger.data.db.entity.FoodEntity) {
            val idx = stored.indexOfFirst { it.id == food.id }
            if (idx >= 0) {
                stored[idx] = food
                listAllFlow.value = stored.toList()
            }
        }

        override suspend fun delete(food: com.edu.gymledger.data.db.entity.FoodEntity) {
            stored.removeAll { it.id == food.id }
            listAllFlow.value = stored.toList()
        }

        override suspend fun getById(id: Long): com.edu.gymledger.data.db.entity.FoodEntity? {
            return stored.find { it.id == id }
        }

        override fun listAll(): Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> = listAllFlow

        override fun searchByName(query: String): Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> {
            val lower = query.lowercase()
            return kotlinx.coroutines.flow.flow {
                listAllFlow.collect { items ->
                    emit(items.filter { it.name.lowercase().contains(lower) })
                }
            }
        }
    }

    class FakeFoodLookupClient : FoodLookupClient {
        var configResult: FoodLookupOutcome<FoodLookupConfigDto> = FoodLookupOutcome.Error(FoodLookupError.Transport)
        var searchResult: FoodLookupOutcome<GenericLookupDataDto> = FoodLookupOutcome.Success(GenericLookupDataDto())
        var searchGate: CompletableDeferred<Unit>? = null
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
        ): FoodLookupOutcome<GenericLookupDataDto> {
            searchGenericCallCount++
            searchGate?.await()
            return searchResult
        }
    }

    class FakeMonotonicTimeSource : MonotonicTimeSource {
        override fun nowMillis(): Long = 0L
    }
}
