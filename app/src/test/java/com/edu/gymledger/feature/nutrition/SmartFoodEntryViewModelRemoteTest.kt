package com.edu.gymledger.feature.nutrition

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
import com.edu.gymledger.data.repository.FoodReferenceRepository
import com.edu.gymledger.data.repository.FoodRepository
import com.edu.gymledger.data.repository.OnlineAssistanceSettings
import com.edu.gymledger.data.repository.lookup.BarcodeLookupAvailability
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

    // --- Stale search state cleared on query change (Fix 1) ---

    @Test
    fun onOnlineQueryChange_clearsStaleResults() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.searchResult = FoodLookupOutcome.Success(genericData(listOf(eggItemDto())))
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.onlineResults.size)

        viewModel.onOnlineQueryChange("chicken")

        val state = viewModel.uiState.value
        assertEquals("chicken", state.onlineQuery)
        assertTrue(state.onlineResults.isEmpty())
        assertEquals(null, state.onlineError)
        assertFalse(state.hasSubmittedOnlineSearch)
    }

    @Test
    fun onOnlineQueryChange_clearsEmptyResultState() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.searchResult = FoodLookupOutcome.Success(emptyData())
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("zzz")
        viewModel.submitOnlineSearch()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.onlineError)

        viewModel.onOnlineQueryChange("egg")

        val state = viewModel.uiState.value
        assertEquals(null, state.onlineError)
        assertFalse(state.hasSubmittedOnlineSearch)
        assertTrue(state.onlineResults.isEmpty())
    }

    // --- Empty-result state only after actual submission (Fix 2) ---

    @Test
    fun typingValidQueryWithoutSubmit_noEmptyState() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("egg")

        val state = viewModel.uiState.value
        assertTrue(state.onlineQuery.trim().length >= state.minQueryLength)
        assertFalse(state.hasSubmittedOnlineSearch)
        assertEquals(null, state.onlineError)
        assertTrue(state.onlineResults.isEmpty())
    }

    @Test
    fun submitOnlineSearch_emptyResult_setsEmptyMessage() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.searchResult = FoodLookupOutcome.Success(emptyData())
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.onOnlineQueryChange("zzz")
        viewModel.submitOnlineSearch()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasSubmittedOnlineSearch)
        assertTrue(state.onlineResults.isEmpty())
        assertEquals("No foods found online. Try another term or add it manually.", state.onlineError)
    }

    // --- Config fetch cancellation (Fix 3) ---

    @Test
    fun leavingOnlineMode_cancelsPendingConfigFetch() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.configGate = CompletableDeferred()
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.mode == SmartFoodEntryMode.ONLINE)
        assertTrue(viewModel.uiState.value.isCheckingOnlineAvailability)

        viewModel.toggleOnlineMode(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.mode == SmartFoodEntryMode.ONLINE)
        assertFalse(viewModel.uiState.value.isCheckingOnlineAvailability)

        fakeClient.configGate!!.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.mode == SmartFoodEntryMode.ONLINE)
        assertFalse(state.isCheckingOnlineAvailability)
        assertTrue(state.onlineAvailability !is OnlineSearchAvailability.Available)
    }

    @Test
    fun cancelSearch_cancelsPendingConfigFetch() = runTest {
        settingsFlow.value = enabledSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(enabledConfig())
        fakeClient.configGate = CompletableDeferred()
        advanceUntilIdle()
        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        viewModel.cancelSearch()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCheckingOnlineAvailability)

        fakeClient.configGate!!.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCheckingOnlineAvailability)
        assertTrue(state.onlineAvailability !is OnlineSearchAvailability.Available)
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
        assertFalse(viewModel.uiState.value.mode == SmartFoodEntryMode.ONLINE)
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

    // --- Barcode mode tests ---

    @Test
    fun defaultMode_isLocal() {
        assertEquals(SmartFoodEntryMode.LOCAL, viewModel.uiState.value.mode)
    }

    @Test
    fun enterBarcodeMode_fetchesConfigOnceAndBecomesAvailable() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SmartFoodEntryMode.BARCODE, state.mode)
        assertTrue(state.barcodeAvailability is BarcodeLookupAvailability.Available)
        assertFalse(state.isCheckingOnlineAvailability)
        assertEquals(1, fakeClient.fetchConfigCallCount)
    }

    @Test
    fun leavingBarcodeForOnline_cancelsInFlightLookup() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeGate = CompletableDeferred()
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()
        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBarcodeSearching)

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(SmartFoodEntryMode.ONLINE, viewModel.uiState.value.mode)
        assertFalse(viewModel.uiState.value.isBarcodeSearching)

        fakeClient.barcodeGate!!.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SmartFoodEntryMode.ONLINE, state.mode)
        assertEquals(null, state.barcodeResult)
        assertFalse(state.isBarcodeSearching)
    }

    @Test
    fun leavingBarcodeForLocal_cancelsInFlightLookup() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeGate = CompletableDeferred()
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()
        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBarcodeSearching)

        viewModel.toggleOnlineMode(false)
        advanceUntilIdle()

        assertEquals(SmartFoodEntryMode.LOCAL, viewModel.uiState.value.mode)
        assertFalse(viewModel.uiState.value.isBarcodeSearching)
        assertFalse(viewModel.uiState.value.isCheckingOnlineAvailability)

        fakeClient.barcodeGate!!.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SmartFoodEntryMode.LOCAL, state.mode)
        assertEquals(null, state.barcodeResult)
    }

    @Test
    fun barcodeLookup_staleResponseAfterModeSwitch_doesNotOverwriteState() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto())
        fakeClient.barcodeGate = CompletableDeferred()
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()
        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBarcodeSearching)

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        fakeClient.barcodeGate!!.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SmartFoodEntryMode.ONLINE, state.mode)
        assertEquals(null, state.barcodeResult)
        assertEquals(null, state.barcodeError)
        assertFalse(state.isBarcodeSearching)
        assertFalse(state.barcodeNotFound)
    }

    @Test
    fun barcodeTextChange_updatesStateWithoutLookup() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("0123456789012")

        val state = viewModel.uiState.value
        assertEquals("0123456789012", state.barcodeText)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
        assertFalse(state.hasSubmittedBarcodeLookup)
    }

    @Test
    fun barcodeTextChange_clearsStaleResultAndError() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto())
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()
        assertEquals("Hazelnut spread", viewModel.uiState.value.barcodeResult?.name)

        viewModel.onBarcodeTextChange("1234567890123")

        val state = viewModel.uiState.value
        assertEquals("1234567890123", state.barcodeText)
        assertEquals(null, state.barcodeResult)
        assertEquals(null, state.barcodeError)
        assertFalse(state.hasSubmittedBarcodeLookup)
        assertFalse(state.barcodeNotFound)
    }

    @Test
    fun submitBarcodeLookup_invalidBarcode_noLookupAndValidationMessage() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("1234")
        viewModel.submitBarcodeLookup()

        val state = viewModel.uiState.value
        assertFalse(state.isBarcodeSearching)
        assertTrue(state.hasSubmittedBarcodeLookup)
        assertEquals("Enter an 8, 12, 13, or 14-digit barcode.", state.barcodeError)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun submitBarcodeLookup_availabilityNotAvailable_zeroLookups() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig().copy(safeMode = true))
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.barcodeAvailability !is BarcodeLookupAvailability.Available)

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()

        val state = viewModel.uiState.value
        assertFalse(state.isBarcodeSearching)
        assertEquals(null, state.barcodeError)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun submitBarcodeLookup_validBarcode_successShowsResult() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto())
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange(" 0123456789012 ")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, fakeClient.lookupBarcodeCallCount)
        assertFalse(state.isBarcodeSearching)
        assertEquals("0123456789012", state.barcodeText)
        assertEquals("Hazelnut spread", state.barcodeResult?.name)
        assertTrue(state.barcodeResult!!.hasCompleteNutrition)
        assertEquals(null, state.barcodeError)
        assertFalse(state.barcodeNotFound)
    }

    @Test
    fun submitBarcodeLookup_duplicateSubmission_ignored() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto())
        fakeClient.barcodeGate = CompletableDeferred()
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()

        assertEquals(1, fakeClient.lookupBarcodeCallCount)
        assertTrue(viewModel.uiState.value.isBarcodeSearching)

        fakeClient.barcodeGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("Hazelnut spread", viewModel.uiState.value.barcodeResult?.name)
    }

    @Test
    fun submitBarcodeLookup_unknownBarcode_setsBarcodeNotFoundState() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Empty
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("9999999999999")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.barcodeNotFound)
        assertNotNull(state.barcodeError)
        assertEquals(null, state.barcodeResult)
        assertFalse(state.isBarcodeSearching)
    }

    @Test
    fun submitBarcodeLookup_namelessProduct_setsBarcodeNotFoundState() = runTest {
        settingsFlow.value = barcodeSettings()
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
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.barcodeNotFound)
        assertNotNull(state.barcodeError)
        assertEquals(null, state.barcodeResult)
        assertFalse(state.isBarcodeSearching)
        assertEquals(1, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun submitBarcodeLookup_errorAfterSuccess_clearsStaleResult() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto())
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()
        assertEquals("Hazelnut spread", viewModel.uiState.value.barcodeResult?.name)

        fakeClient.barcodeResult = FoodLookupOutcome.Error(FoodLookupError.Transport)
        viewModel.onBarcodeTextChange("1234567890123")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.barcodeResult)
        assertNotNull(state.barcodeError)
        assertFalse(state.barcodeNotFound)
    }

    @Test
    fun leavingBarcodeMode_clearsStaleBarcodeState() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto())
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasSubmittedBarcodeLookup)

        viewModel.toggleOnlineMode(false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SmartFoodEntryMode.LOCAL, state.mode)
        assertEquals(null, state.barcodeResult)
        assertEquals(null, state.barcodeError)
        assertFalse(state.isBarcodeSearching)
        assertFalse(state.hasSubmittedBarcodeLookup)
        assertFalse(state.barcodeNotFound)
    }

    @Test
    fun resetState_cancelsInFlightBarcodeLookup() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto())
        fakeClient.barcodeGate = CompletableDeferred()
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()
        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBarcodeSearching)

        viewModel.resetState()
        advanceUntilIdle()

        assertEquals(SmartFoodEntryMode.LOCAL, viewModel.uiState.value.mode)
        fakeClient.barcodeGate!!.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.barcodeResult)
        assertFalse(state.isBarcodeSearching)
        assertEquals(1, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun useBarcodeProduct_prefillsSelectedFoodFlow() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto())
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()

        viewModel.useBarcodeProduct()

        val state = viewModel.uiState.value
        assertEquals("Hazelnut spread", state.selectedReference?.name)
        assertEquals("100", state.gramsText)
        assertEquals("544", state.caloriesText)
        assertEquals("4", state.proteinText)
    }

    @Test
    fun useBarcodeProduct_incompleteResult_doesNotSelectFood() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        fakeClient.barcodeResult = FoodLookupOutcome.Success(packagedDto(proteinG = null))
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.barcodeResult?.hasCompleteNutrition)

        viewModel.useBarcodeProduct()

        assertEquals(null, viewModel.uiState.value.selectedReference)
    }

    @Test
    fun settingsReemission_keepsConfirmedBarcodeAvailability() = runTest {
        settingsFlow.value = barcodeSettings()
        fakeClient.configResult = FoodLookupOutcome.Success(barcodeConfig())
        advanceUntilIdle()
        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.barcodeAvailability is BarcodeLookupAvailability.Available)

        settingsFlow.value = barcodeSettings().copy(usdaEnabled = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.barcodeAvailability is BarcodeLookupAvailability.Available)
        assertTrue(state.onlineAvailability is OnlineSearchAvailability.UsdaDisabled)
    }

    // --- Mode selector always visible (bug fix) ---

    @Test
    fun onlineDisabled_canStillSwitchToOnlineMode() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(onlineFoodLookupEnabled = false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isOnlineAvailable)

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()

        assertEquals(SmartFoodEntryMode.ONLINE, viewModel.uiState.value.mode)
        assertEquals(0, fakeClient.fetchConfigCallCount)
        assertTrue(viewModel.uiState.value.onlineAvailability is OnlineSearchAvailability.Disabled)
    }

    @Test
    fun barcodeMode_selectableWhenMissingApiKey() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "",
            openFoodFactsEnabled = true
        )
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        assertEquals(SmartFoodEntryMode.BARCODE, viewModel.uiState.value.mode)
        assertTrue(viewModel.uiState.value.barcodeAvailability is BarcodeLookupAvailability.NotConfigured)
    }

    @Test
    fun barcodeMode_selectableWhenSafeMode() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key",
            openFoodFactsEnabled = true,
            safeModeEnabled = true
        )
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        assertEquals(SmartFoodEntryMode.BARCODE, viewModel.uiState.value.mode)
        assertTrue(viewModel.uiState.value.barcodeAvailability is BarcodeLookupAvailability.SafeMode)
    }

    @Test
    fun barcodeMode_selectableWhenOffDisabled() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "key",
            openFoodFactsEnabled = false
        )
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()

        assertEquals(SmartFoodEntryMode.BARCODE, viewModel.uiState.value.mode)
        assertTrue(viewModel.uiState.value.barcodeAvailability is BarcodeLookupAvailability.OpenFoodFactsDisabled)
    }

    @Test
    fun selectingUnavailableBarcodeMode_performsZeroLookup() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "",
            openFoodFactsEnabled = true
        )
        advanceUntilIdle()

        viewModel.onBarcodeModeSelected()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.barcodeAvailability !is BarcodeLookupAvailability.Available)

        viewModel.onBarcodeTextChange("0123456789012")
        viewModel.submitBarcodeLookup()

        val state = viewModel.uiState.value
        assertFalse(state.isBarcodeSearching)
        assertEquals(0, fakeClient.lookupBarcodeCallCount)
    }

    @Test
    fun selectingUnavailableOnlineMode_performsZeroSearch() = runTest {
        settingsFlow.value = OnlineAssistanceSettings(
            onlineFoodLookupEnabled = true,
            foodLookupApiKey = "",
            usdaEnabled = true
        )
        advanceUntilIdle()

        viewModel.toggleOnlineMode(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.onlineAvailability !is OnlineSearchAvailability.Available)

        viewModel.onOnlineQueryChange("egg")
        viewModel.submitOnlineSearch()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isOnlineSearching)
        assertEquals(0, fakeClient.searchGenericCallCount)
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

    private fun barcodeSettings() = OnlineAssistanceSettings(
        onlineFoodLookupEnabled = true,
        foodLookupApiKey = "key",
        usdaEnabled = true,
        openFoodFactsEnabled = true,
        safeModeEnabled = false
    )

    private fun barcodeConfig() = FoodLookupConfigDto(
        onlineLookupAvailable = true,
        providers = ProvidersDto(usda = true, openFoodFacts = true),
        features = FeaturesDto(genericFoodSearch = true, barcodeLookup = true),
        minQueryLength = 3,
        safeMode = false
    )

    private fun packagedDto(
        name: String = "Hazelnut spread",
        caloriesKcal: Double? = 544.0,
        proteinG: Double? = 4.0,
        carbohydrateG: Double? = 57.0,
        fatG: Double? = 32.0
    ) = PackagedFoodLookupDataDto(
        barcode = "0123456789012",
        source = "OPEN_FOOD_FACTS",
        attribution = "Open Food Facts — ODbL",
        isApproximate = true,
        product = PackagedFoodProductDto(
            externalId = "0123456789012",
            name = name,
            nutritionPer100g = PackagedNutritionDto(caloriesKcal, proteinG, carbohydrateG, fatG)
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

        override fun listAllRanked(): Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> = kotlinx.coroutines.flow.flow {
            listAllFlow.collect { items -> emit(rankFoods(items)) }
        }

        override fun searchRanked(query: String): Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> = kotlinx.coroutines.flow.flow {
            listAllFlow.collect { items -> emit(rankFoods(items.filter { it.name.contains(query, ignoreCase = true) })) }
        }

        override fun listFavorites(): Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> = kotlinx.coroutines.flow.flow {
            listAllFlow.collect { items -> emit(rankFoods(items.filter { it.isFavorite }).sortedWith(favoriteComparator)) }
        }

        override fun searchFavorites(query: String): Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> = kotlinx.coroutines.flow.flow {
            listAllFlow.collect { items -> emit(rankFoods(items.filter { it.isFavorite && it.name.contains(query, ignoreCase = true) }).sortedWith(favoriteComparator)) }
        }

        override fun listRecent(): Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> = kotlinx.coroutines.flow.flow {
            listAllFlow.collect { items -> emit(items.filter { it.lastUsedAt != null }.sortedWith(recentComparator)) }
        }

        override fun searchRecent(query: String): Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>> = kotlinx.coroutines.flow.flow {
            listAllFlow.collect { items -> emit(items.filter { it.lastUsedAt != null && it.name.contains(query, ignoreCase = true) }.sortedWith(recentComparator)) }
        }

        override suspend fun setFavorite(foodId: Long, isFavorite: Boolean, favoriteAt: Long?) {
            val idx = stored.indexOfFirst { it.id == foodId }
            if (idx >= 0) {
                stored[idx] = stored[idx].copy(isFavorite = isFavorite, favoriteAt = favoriteAt)
                listAllFlow.value = stored.toList()
            }
        }

        override suspend fun markUsed(foodId: Long, usedAtMillis: Long) {
            val idx = stored.indexOfFirst { it.id == foodId }
            if (idx >= 0) {
                stored[idx] = stored[idx].copy(lastUsedAt = usedAtMillis)
                listAllFlow.value = stored.toList()
            }
        }

        private val recentComparator = compareByDescending<com.edu.gymledger.data.db.entity.FoodEntity> {
            it.lastUsedAt ?: Long.MIN_VALUE
        }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }.thenByDescending { it.id }

        private val favoriteComparator = recentComparator

        private fun rankFoods(items: List<com.edu.gymledger.data.db.entity.FoodEntity>) = items.sortedWith(
            compareByDescending<com.edu.gymledger.data.db.entity.FoodEntity> { it.isFavorite }
                .thenByDescending { it.lastUsedAt ?: Long.MIN_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenByDescending { it.id }
        )
    }

    class FakeFoodLookupClient : FoodLookupClient {
        var configResult: FoodLookupOutcome<FoodLookupConfigDto> = FoodLookupOutcome.Error(FoodLookupError.Transport)
        var searchResult: FoodLookupOutcome<GenericLookupDataDto> = FoodLookupOutcome.Success(GenericLookupDataDto())
        var configGate: CompletableDeferred<Unit>? = null
        var searchGate: CompletableDeferred<Unit>? = null
        var barcodeGate: CompletableDeferred<Unit>? = null
        var fetchConfigCallCount = 0
            private set
        var searchGenericCallCount = 0
            private set
        var barcodeResult: FoodLookupOutcome<PackagedFoodLookupDataDto> = FoodLookupOutcome.Empty
        var lookupBarcodeCallCount = 0
            private set

        override suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<FoodLookupConfigDto> {
            fetchConfigCallCount++
            configGate?.await()
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

        override suspend fun lookupBarcode(
            baseUrl: String,
            apiKey: String,
            barcode: String
        ): FoodLookupOutcome<PackagedFoodLookupDataDto> {
            lookupBarcodeCallCount++
            barcodeGate?.await()
            return barcodeResult
        }
    }

    class FakeMonotonicTimeSource : MonotonicTimeSource {
        override fun nowMillis(): Long = 0L
    }
}
