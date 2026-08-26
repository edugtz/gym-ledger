package com.edu.gymledger.data.remote

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OkHttpFoodLookupClientTest {

    private val successConfigBody = """
        {
            "ok": true,
            "data": {
                "onlineLookupAvailable": true,
                "providers": { "usda": true, "openFoodFacts": false },
                "features": { "genericFoodSearch": true, "barcodeLookup": false },
                "minQueryLength": 3,
                "safeMode": false
            }
        }
    """.trimIndent()

    private val conservativeConfigBody = """
        {
            "ok": true,
            "data": {
                "onlineLookupAvailable": false,
                "providers": { "usda": false, "openFoodFacts": false },
                "features": { "genericFoodSearch": false, "barcodeLookup": false },
                "minQueryLength": 3,
                "safeMode": true
            }
        }
    """.trimIndent()

    private val successGenericBody = """
        {
            "ok": true,
            "data": {
                "query": "egg",
                "source": "USDA",
                "attribution": "USDA FoodData Central",
                "isApproximate": true,
                "results": [
                    {
                        "externalId": "usda:171287",
                        "name": "Whole egg, large",
                        "description": "Whole egg, large",
                        "dataType": "survey_fndds_food",
                        "nutritionPer100g": {
                            "caloriesKcal": 143.0,
                            "proteinG": 12.6,
                            "carbohydrateG": 0.7,
                            "fatG": 9.5
                        }
                    }
                ]
            }
        }
    """.trimIndent()

    private val twoResultsGenericBody = """
        {
            "ok": true,
            "data": {
                "query": "egg",
                "source": "USDA",
                "attribution": "USDA FoodData Central",
                "isApproximate": true,
                "results": [
                    {
                        "externalId": "usda:171287",
                        "name": "Whole egg, large",
                        "description": "Whole egg, large",
                        "dataType": "survey_fndds_food",
                        "nutritionPer100g": {
                            "caloriesKcal": 143.0,
                            "proteinG": 12.6,
                            "carbohydrateG": 0.7,
                            "fatG": 9.5
                        }
                    },
                    {
                        "externalId": "usda:171286",
                        "name": "Egg, whole, cooked",
                        "description": "Egg, whole, cooked",
                        "dataType": "survey_fndds_food",
                        "nutritionPer100g": {
                            "caloriesKcal": 155.0,
                            "proteinG": 12.5,
                            "carbohydrateG": 1.1,
                            "fatG": 10.6
                        }
                    }
                ]
            }
        }
    """.trimIndent()

    private val emptyResultsBody = """
        {
            "ok": true,
            "data": {
                "query": "zzz",
                "source": "USDA",
                "attribution": "USDA FoodData Central",
                "isApproximate": true,
                "results": []
            }
        }
    """.trimIndent()

    private val successBarcodeBody = """
        {
            "ok": true,
            "data": {
                "barcode": "0123456789012",
                "source": "OPEN_FOOD_FACTS",
                "attribution": "Open Food Facts — ODbL",
                "isApproximate": true,
                "product": {
                    "externalId": "0123456789012",
                    "name": "Hazelnut spread",
                    "genericName": "Spread",
                    "brands": ["BrandA"],
                    "quantity": "450 g",
                    "servingSize": "20 g",
                    "nutritionPer100g": {
                        "caloriesKcal": 544.0,
                        "proteinG": 4.0,
                        "carbohydrateG": 57.0,
                        "fatG": 32.0
                    }
                }
            }
        }
    """.trimIndent()

    private val incompleteBarcodeBody = """
        {
            "ok": true,
            "data": {
                "barcode": "1234567890123",
                "source": "OPEN_FOOD_FACTS",
                "attribution": "Open Food Facts — ODbL",
                "isApproximate": false,
                "product": {
                    "externalId": "1234567890123",
                    "name": "Mystery product",
                    "brands": [],
                    "quantity": null,
                    "servingSize": null,
                    "nutritionPer100g": {
                        "caloriesKcal": 200.0,
                        "proteinG": null,
                        "carbohydrateG": 40.0,
                        "fatG": null
                    }
                }
            }
        }
    """.trimIndent()

    private fun errorBody(code: String, message: String = "test") =
        """{"ok":false,"error":{"code":"$code","message":"$message"}}"""

    @Test
    fun fetchConfig_correctRoute_noKeyHeader() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successConfigBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.fetchConfig("https://example.com/")

        assertTrue(result is FoodLookupOutcome.Success)
        val config = (result as FoodLookupOutcome.Success).data
        assertTrue(config.onlineLookupAvailable)
        assertTrue(config.providers.usda)
        assertEquals(3, config.minQueryLength)
        assertEquals("GET", fakeCall.capturedRequest!!.method)
        assertTrue(fakeCall.capturedRequest!!.url.encodedPath.endsWith("/v1/config"))
        assertFalse(fakeCall.capturedRequest!!.headers.names().contains("X-GymLedger-Key"))
    }

    @Test
    fun fetchConfig_conservativeEnvelope_decodesFields() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, conservativeConfigBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.fetchConfig("https://example.com/")

        assertTrue(result is FoodLookupOutcome.Success)
        val config = (result as FoodLookupOutcome.Success).data
        assertFalse(config.onlineLookupAvailable)
        assertTrue(config.safeMode)
        assertFalse(config.providers.usda)
        assertFalse(config.features.genericFoodSearch)
    }

    @Test
    fun fetchConfig_envelopeOkFalse_returnsMalformed() = runTest {
        val body = """
            {
                "ok": false,
                "error": { "code": "lookup_disabled", "message": "Lookup is disabled" }
            }
        """.trimIndent()
        val fakeCall = FakeCall(response = jsonResponse(200, body))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.fetchConfig("https://example.com/")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun fetchConfig_missingDataField_returnsMalformed() = runTest {
        val fakeCall = FakeCall(response = jsonResponse(200, """{"ok": true}"""))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.fetchConfig("https://example.com/")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun fetchConfig_nullData_returnsMalformed() = runTest {
        val fakeCall = FakeCall(response = jsonResponse(200, """{"ok": true, "data": null}"""))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.fetchConfig("https://example.com/")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun fetchConfig_missingOk_returnsMalformed() = runTest {
        val body = """{"data": {"onlineLookupAvailable": true}}"""
        val fakeCall = FakeCall(response = jsonResponse(200, body))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.fetchConfig("https://example.com/")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun searchGeneric_okFalseOn200_returnsMalformed() = runTest {
        val body = """{"ok": false, "error": {"code": "not_found", "message": "Not found"}}"""
        val fakeCall = FakeCall(response = jsonResponse(200, body))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun searchGeneric_nullData_returnsMalformed() = runTest {
        val fakeCall = FakeCall(response = jsonResponse(200, """{"ok": true, "data": null}"""))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun searchGeneric_missingOk_returnsMalformed() = runTest {
        val body = """{"data": {"query": "egg", "source": "USDA", "results": []}}"""
        val fakeCall = FakeCall(response = jsonResponse(200, body))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun searchGeneric_correctRoute_withKeyHeader() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successGenericBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "test-api-key", "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        val data = (result as FoodLookupOutcome.Success).data
        assertEquals(1, data.results.size)
        assertEquals("Whole egg, large", data.results[0].name)
        assertEquals("test-api-key", fakeCall.capturedRequest!!.header("X-GymLedger-Key"))
        assertTrue(fakeCall.capturedRequest!!.url.encodedPath.contains("/v1/foods/generic"))
        assertEquals("egg", fakeCall.capturedRequest!!.url.queryParameter("q"))
    }

    @Test
    fun searchGeneric_keyNotInUrl() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successGenericBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        client.searchGeneric("https://example.com/", "secret-key", "query")

        val url = fakeCall.capturedRequest!!.url.toString()
        assertFalse(url.contains("secret-key"))
    }

    @Test
    fun searchGeneric_blankKey_noHeader() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successGenericBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        client.searchGeneric("https://example.com/", "", "egg")

        assertTrue(fakeCall.capturedRequest!!.header("X-GymLedger-Key") == null)
    }

    @Test
    fun searchGeneric_externalIdPreserved() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successGenericBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        val data = (result as FoodLookupOutcome.Success).data
        assertEquals("usda:171287", data.results[0].externalId)
    }

    @Test
    fun searchGeneric_metadataPreserved() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successGenericBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        val data = (result as FoodLookupOutcome.Success).data
        assertEquals("egg", data.query)
        assertEquals("USDA", data.source)
        assertEquals("USDA FoodData Central", data.attribution)
        assertTrue(data.isApproximate)
    }

    @Test
    fun searchGeneric_twoResults_distinctExternalIds() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, twoResultsGenericBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        val data = (result as FoodLookupOutcome.Success).data
        assertEquals(2, data.results.size)
        assertEquals("usda:171287", data.results[0].externalId)
        assertEquals("usda:171286", data.results[1].externalId)
        assertNotEquals(data.results[0].externalId, data.results[1].externalId)
    }

    @Test
    fun searchGeneric_emptyResults_returnsSuccessWithEmptyData() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, emptyResultsBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Success)
        assertTrue((result as FoodLookupOutcome.Success).data.results.isEmpty())
    }

    @Test
    fun http400_invalidQuery() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(400, errorBody("invalid_query"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "x")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.InvalidQuery, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http401_unauthorized() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(401, errorBody("unauthorized"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Unauthorized, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http404_notFound_returnsEmpty() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(404, errorBody("not_found"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Empty)
    }

    @Test
    fun http429_budgetExceeded() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(429, errorBody("budget_exceeded"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.BudgetExceeded, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http429_providerRateLimited_isProviderErrorNotBudget() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(429, errorBody("provider_rate_limited"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http502_providerError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(502, errorBody("provider_error"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http502_providerUnavailable_mapsProviderError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("provider_unavailable"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_lookupDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("lookup_disabled"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_onlineLookupDisabled_mapsLookupDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("online_lookup_disabled"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_providerDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("provider_disabled"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_featureDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("feature_disabled"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.FeatureDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_configurationError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("configuration_error"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ConfigurationError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_providerError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("provider_error"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http504_providerTimeout_mapsTransport() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(504, errorBody("provider_timeout"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Transport, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_malformedBody_statusFallbackLookupDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, "not json {{{")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun unknownErrorCode_statusFallback() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("mystery_code"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http429_noBody_statusFallbackBudgetExceeded() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(429, "")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.BudgetExceeded, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http500_unknownStatus_mapsTransport() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(500, "{}")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Transport, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http502_noBody_statusFallbackProviderError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(502, "")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun malformedBody_returnsMalformedResponse() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, "not json {{{")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun cancellation_cancelsCall() = runTest {
        val fakeCall = FakeCall()
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val job = launch {
            client.searchGeneric("https://example.com/", "key", "query")
        }
        testScheduler.advanceTimeBy(1)
        job.cancel()
        job.join()

        assertTrue(fakeCall.isCancelled)
    }

    @Test
    fun ioException_returnsTransport() = runTest {
        val fakeCall = FakeCall(
            exception = java.io.IOException("connection failed")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Transport, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun successGenericBody_decodesCompleteNutrition() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successGenericBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        val item = (result as FoodLookupOutcome.Success).data.results[0]
        assertEquals("usda:171287", item.externalId)
        assertEquals(143.0, item.nutritionPer100g.caloriesKcal!!, 0.001)
        assertEquals(12.6, item.nutritionPer100g.proteinG!!, 0.001)
        assertEquals(0.7, item.nutritionPer100g.carbohydrateG!!, 0.001)
        assertEquals(9.5, item.nutritionPer100g.fatG!!, 0.001)
    }

    // --- Barcode lookup tests ---

    @Test
    fun lookupBarcode_correctRoute_pathSegmentWithLeadingZero_andKeyHeader() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successBarcodeBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "test-api-key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Success)
        assertEquals("GET", fakeCall.capturedRequest!!.method)
        assertTrue(fakeCall.capturedRequest!!.url.encodedPath.endsWith("/v1/foods/barcode/0123456789012"))
        assertEquals("test-api-key", fakeCall.capturedRequest!!.header("X-GymLedger-Key"))
        assertEquals("0123456789012", (result as FoodLookupOutcome.Success).data.barcode)
    }

    @Test
    fun lookupBarcode_completeProduct_fullDtoDecoded() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successBarcodeBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Success)
        val data = (result as FoodLookupOutcome.Success).data
        assertEquals("OPEN_FOOD_FACTS", data.source)
        assertEquals("Open Food Facts — ODbL", data.attribution)
        assertTrue(data.isApproximate)
        assertEquals("0123456789012", data.product.externalId)
        assertEquals("Hazelnut spread", data.product.name)
        assertEquals(listOf("BrandA"), data.product.brands)
        assertEquals("450 g", data.product.quantity)
        assertEquals("20 g", data.product.servingSize)
        assertEquals(544.0, data.product.nutritionPer100g.caloriesKcal!!, 0.001)
        assertEquals(4.0, data.product.nutritionPer100g.proteinG!!, 0.001)
        assertEquals(57.0, data.product.nutritionPer100g.carbohydrateG!!, 0.001)
        assertEquals(32.0, data.product.nutritionPer100g.fatG!!, 0.001)
    }

    @Test
    fun lookupBarcode_incompleteNutrition_nullsPreserved() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, incompleteBarcodeBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "1234567890123")

        assertTrue(result is FoodLookupOutcome.Success)
        val product = (result as FoodLookupOutcome.Success).data.product
        assertEquals("Mystery product", product.name)
        assertEquals(200.0, product.nutritionPer100g.caloriesKcal!!, 0.001)
        assertNull(product.nutritionPer100g.proteinG)
        assertEquals(40.0, product.nutritionPer100g.carbohydrateG!!, 0.001)
        assertNull(product.nutritionPer100g.fatG)
        assertNull(product.quantity)
        assertNull(product.servingSize)
    }

    @Test
    fun lookupBarcode_notFound_returnsEmpty() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(404, errorBody("not_found"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0000000000000")

        assertTrue(result is FoodLookupOutcome.Empty)
    }

    @Test
    fun lookupBarcode_invalidBarcodeCode_mapsInvalidBarcode() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(400, errorBody("invalid_barcode"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "1234")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.InvalidBarcode, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_unauthorizedCode_mapsUnauthorized() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(401, errorBody("unauthorized"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Unauthorized, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_lookupDisabledCode_mapsLookupDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("lookup_disabled"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_providerDisabledCode_mapsProviderDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("provider_disabled"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_featureDisabledCode_mapsFeatureDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("feature_disabled"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.FeatureDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_budgetExceededCode_mapsBudgetExceeded() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(429, errorBody("budget_exceeded"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.BudgetExceeded, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_configurationErrorCode_mapsConfigurationError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("configuration_error"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ConfigurationError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_providerErrorCode_mapsProviderError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, errorBody("provider_error"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_providerTimeoutCode_mapsTransport() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(504, errorBody("provider_timeout"))
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Transport, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_nonJsonSuccessBody_returnsMalformed() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, "not json {{{")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_typeMismatchedNutrient_returnsMalformed() = runTest {
        val body = """
            {
                "ok": true,
                "data": {
                    "barcode": "0123456789012",
                    "product": {
                        "externalId": "0123456789012",
                        "name": "Hazelnut spread",
                        "nutritionPer100g": { "caloriesKcal": "high" }
                    }
                }
            }
        """.trimIndent()
        val fakeCall = FakeCall(response = jsonResponse(200, body))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_okFalseOn200_returnsMalformed() = runTest {
        val body = """{"ok": false, "error": {"code": "not_found", "message": "Not found"}}"""
        val fakeCall = FakeCall(response = jsonResponse(200, body))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.MalformedResponse, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun lookupBarcode_okTrueMissingData_returnsEmpty() = runTest {
        val fakeCall = FakeCall(response = jsonResponse(200, """{"ok": true}"""))
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.lookupBarcode("https://example.com/", "key", "0123456789012")

        assertTrue(result is FoodLookupOutcome.Empty)
    }

    @Test
    fun lookupBarcode_cancellation_cancelsCall() = runTest {
        val fakeCall = FakeCall()
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val job = launch {
            client.lookupBarcode("https://example.com/", "key", "0123456789012")
        }
        testScheduler.advanceTimeBy(1)
        job.cancel()
        job.join()

        assertTrue(fakeCall.isCancelled)
    }

    // --- Test helpers ---

    private fun jsonResponse(code: Int, body: String): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://example.com/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body(
                ResponseBody.create(
                    "application/json".toMediaType(),
                    body
                )
            )
            .build()
    }

    // --- Fakes ---

    class FakeCallFactory(private val fakeCall: FakeCall) : Call.Factory {
        override fun newCall(request: Request): Call {
            fakeCall.capturedRequest = request
            return fakeCall
        }
    }

    class FakeCall(
        private val response: Response? = null,
        private val exception: java.io.IOException? = null
    ) : Call {

        var capturedRequest: Request? = null
            internal set

        var isCancelled = false
            private set

        private var pendingCallback: Callback? = null

        override fun request(): Request = capturedRequest ?: Request.Builder().url("https://example.com/").build()

        override fun enqueue(responseCallback: Callback) {
            if (response == null && exception == null) {
                pendingCallback = responseCallback
            } else if (exception != null) {
                responseCallback.onFailure(this, exception)
            } else {
                responseCallback.onResponse(this, response!!)
            }
        }

        fun completeWithResponse(response: Response) {
            pendingCallback?.onResponse(this, response)
            pendingCallback = null
        }

        fun completeWithFailure(exception: java.io.IOException) {
            pendingCallback?.onFailure(this, exception)
            pendingCallback = null
        }

        override fun cancel() {
            isCancelled = true
            pendingCallback?.onFailure(this, java.io.IOException("Call cancelled"))
            pendingCallback = null
        }

        override fun isExecuted(): Boolean = false
        override fun isCanceled(): Boolean = isCancelled
        override fun timeout(): okio.Timeout = okio.Timeout.NONE
        override fun clone(): Call = this
        override fun execute(): Response {
            throw UnsupportedOperationException("Use enqueue")
        }
    }
}
