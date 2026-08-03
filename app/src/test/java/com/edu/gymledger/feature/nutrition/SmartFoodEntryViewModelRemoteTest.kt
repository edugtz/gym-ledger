package com.edu.gymledger.feature.nutrition

import com.edu.gymledger.data.remote.FoodLookupClient
import com.edu.gymledger.data.remote.FoodLookupError
import com.edu.gymledger.data.remote.FoodLookupOutcome
import com.edu.gymledger.data.remote.MonotonicTimeSource
import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.FeaturesDto
import com.edu.gymledger.data.remote.dto.GenericLookupItemDto
import com.edu.gymledger.data.remote.dto.NutritionPer100gDto
import com.edu.gymledger.data.remote.dto.ProvidersDto
import com.edu.gymledger.data.repository.FoodReferenceRepository
import com.edu.gymledger.data.repository.FoodRepository
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import com.edu.gymledger.data.repository.lookup.OnlineSearchAvailability
import com.edu.gymledger.data.repository.lookup.RemoteFoodLookupRepository
import com.edu.gymledger.domain.model.lookup.RemoteFoodLookupResult
import com.edu.gymledger.domain.model.lookup.RemoteFoodReferenceMapper
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
    private lateinit var fakeRemoteRepository: FakeRemoteFoodLookupRepository
    private lateinit var settingsFlow: MutableStateFlow<OnlineAssistanceSettings>
    private lateinit var viewModel: SmartFoodEntryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeFoodDao = FakeFoodDao()
        fakeFoodRepository = FoodRepository(fakeFoodDao)
        fakeReferenceRepository = FoodReferenceRepository()
        settingsFlow = MutableStateFlow(OnlineAssistanceSettings())
        fakeRemoteRepository = FakeRemoteFoodLookupRepository()
        viewModel = SmartFoodEntryViewModel(
            fakeReferenceRepository,
            fakeFoodRepository,
            fakeRemoteRepository,
            settingsFlow
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onlineSettingDisabled_toggleAbsent() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(onlineFoodLookupEnabled = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isOnlineAvailable)
    }

    @Test
    fun onlineSettingEnabled_toggleVisible() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key"
        )
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
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key"
        )
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
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key"
        )
        fakeRemoteRepository.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeRemoteRepository.searchResult = FoodLookupOutcome.Success(listOf(eggItemDto()))
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
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key"
        )
        fakeRemoteRepository.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeRemoteRepository.searchResult = FoodLookupOutcome.Error(FoodLookupError.Transport)
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
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key"
        )
        fakeRemoteRepository.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeRemoteRepository.searchResult = FoodLookupOutcome.Success(emptyList())
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
    fun duplicateSubmission_prevented() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key"
        )
        fakeRemoteRepository.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeRemoteRepository.searchResult = FoodLookupOutcome.Success(listOf(eggItemDto()))
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        viewModel.submitOnlineSearch()
        advanceUntilIdle()

        assertEquals(1, fakeRemoteRepository.searchCallCount)
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

    private fun eggItemDto() = GenericLookupItemDto(
        id = "usda:123",
        name = "Egg",
        source = "USDA",
        type = "generic",
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

        override fun listAll(): kotlinx.coroutines.flow.Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> = listAllFlow

        override fun searchByName(query: String): kotlinx.coroutines.flow.Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> {
            val lower = query.lowercase()
            return kotlinx.coroutines.flow.flow {
                listAllFlow.collect { items ->
                    emit(items.filter { it.name.lowercase().contains(lower) })
                }
            }
        }
    }

    class FakeRemoteFoodLookupRepository : RemoteFoodLookupRepository(
        object : FoodLookupClient {
            override suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<FoodLookupConfigDto> =
                FoodLookupOutcome.Error(FoodLookupError.Transport)
            override suspend fun searchGeneric(baseUrl: String, apiKey: String, query: String): FoodLookupOutcome<List<GenericLookupItemDto>> =
                FoodLookupOutcome.Error(FoodLookupError.Transport)
        },
        object : MonotonicTimeSource {
            override fun nowMillis(): Long = 0L
        }
    ) {
        var configResult: FoodLookupOutcome<FoodLookupConfigDto> = FoodLookupOutcome.Error(FoodLookupError.Transport)
        var searchResult: FoodLookupOutcome<List<GenericLookupItemDto>> = FoodLookupOutcome.Success(emptyList())
        var searchCallCount = 0
            private set

        override fun getEffectiveAvailability(
            settings: OnlineAssistanceSettings,
            config: FoodLookupConfigDto?
        ): OnlineSearchAvailability {
            if (!settings.onlineFoodLookupEnabled) return OnlineSearchAvailability.Disabled
            if (settings.foodLookupApiKey.isBlank()) return OnlineSearchAvailability.NotConfigured
            if (!settings.usdaEnabled) return OnlineSearchAvailability.UsdaDisabled
            if (settings.safeModeEnabled) return OnlineSearchAvailability.SafeMode
            if (config != null && config.safeMode) return OnlineSearchAvailability.RemoteDisabled
            if (config != null && !config.onlineLookupAvailable) return OnlineSearchAvailability.RemoteDisabled
            if (config != null && !config.providers.usda) return OnlineSearchAvailability.RemoteDisabled
            if (config != null && !config.features.genericFoodSearch) return OnlineSearchAvailability.RemoteDisabled
            return OnlineSearchAvailability.Available("https://example.com/", 3)
        }

        override suspend fun ensureConfig(settings: OnlineAssistanceSettings): FoodLookupConfigDto {
            return when (val r = configResult) {
                is FoodLookupOutcome.Success -> r.data
                else -> FoodLookupConfigDto(
                    onlineLookupAvailable = false,
                    providers = ProvidersDto(usda = false),
                    features = FeaturesDto(genericFoodSearch = false),
                    minQueryLength = 3,
                    safeMode = true
                )
            }
        }

        override suspend fun searchGeneric(
            settings: OnlineAssistanceSettings,
            query: String
        ): FoodLookupOutcome<List<RemoteFoodLookupResult>> {
            searchCallCount++
            return when (val r = searchResult) {
                is FoodLookupOutcome.Success -> {
                    val results = r.data.mapNotNull { dto ->
                        with(RemoteFoodReferenceMapper) {
                            dto.toRemoteResultOrNull("USDA", "USDA FoodData Central", true)
                        }
                    }
                    if (results.isEmpty()) FoodLookupOutcome.Empty
                    else FoodLookupOutcome.Success(results)
                }
                is FoodLookupOutcome.Error -> FoodLookupOutcome.Error(r.reason)
                is FoodLookupOutcome.Empty -> FoodLookupOutcome.Empty
            }
        }
    }
}
