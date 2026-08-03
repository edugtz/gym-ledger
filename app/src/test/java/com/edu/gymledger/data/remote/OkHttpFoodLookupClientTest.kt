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
import okhttp3.internal.connection.RealCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OkHttpFoodLookupClientTest {

    private val successConfigBody = """
        {
            "onlineLookupAvailable": true,
            "providers": { "usda": true, "openFoodFacts": false },
            "features": { "genericFoodSearch": true, "barcodeLookup": false },
            "minQueryLength": 3,
            "safeMode": false
        }
    """.trimIndent()

    private val successGenericBody = """
        {
            "data": {
                "results": [
                    {
                        "id": "usda:171287",
                        "source": "USDA",
                        "type": "generic",
                        "name": "Whole egg, large",
                        "dataType": "survey_fndds_food",
                        "description": null,
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

    private val emptyResultsBody = """
        { "data": { "results": [] } }
    """.trimIndent()

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
    fun searchGeneric_correctRoute_withKeyHeader() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, successGenericBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "test-api-key", "egg")

        assertTrue(result is FoodLookupOutcome.Success)
        val items = (result as FoodLookupOutcome.Success).data
        assertEquals(1, items.size)
        assertEquals("Whole egg, large", items[0].name)
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
    fun searchGeneric_emptyResults_returnsEmpty() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(200, emptyResultsBody)
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Empty)
    }

    @Test
    fun http400_returnsInvalidQuery() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(400, """{"error":"invalid_query"}""")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "x")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.InvalidQuery, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http401_returnsUnauthorized() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(401, """{"error":"unauthorized"}""")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.Unauthorized, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http429_returnsBudgetExceeded() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(429, """{"error":"budget_exceeded"}""")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.BudgetExceeded, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_lookupDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, """{"error":"lookup_disabled"}""")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.LookupDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_providerDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, """{"error":"provider_disabled"}""")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ProviderDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_featureDisabled() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, """{"error":"feature_disabled"}""")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.FeatureDisabled, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_configurationError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, """{"error":"configuration_error"}""")
        )
        val client = OkHttpFoodLookupClient(FakeCallFactory(fakeCall))

        val result = client.searchGeneric("https://example.com/", "key", "query")

        assertTrue(result is FoodLookupOutcome.Error)
        assertEquals(FoodLookupError.ConfigurationError, (result as FoodLookupOutcome.Error).reason)
    }

    @Test
    fun http503_providerError() = runTest {
        val fakeCall = FakeCall(
            response = jsonResponse(503, """{"error":"provider_error"}""")
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
        val item = (result as FoodLookupOutcome.Success).data[0]
        assertEquals("usda:171287", item.id)
        assertEquals(143.0, item.nutritionPer100g.caloriesKcal!!, 0.001)
        assertEquals(12.6, item.nutritionPer100g.proteinG!!, 0.001)
        assertEquals(0.7, item.nutritionPer100g.carbohydrateG!!, 0.001)
        assertEquals(9.5, item.nutritionPer100g.fatG!!, 0.001)
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
