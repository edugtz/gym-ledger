package com.edu.gymledger.data.remote

import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.GenericLookupItemDto
import com.edu.gymledger.data.remote.dto.GenericLookupResponseDto
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

class OkHttpFoodLookupClient(
    private val callFactory: Call.Factory
) : FoodLookupClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    override suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<FoodLookupConfigDto> {
        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegments("v1/config")
            ?.build()
            ?: return FoodLookupOutcome.Error(FoodLookupError.Transport)

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return executeRequest(request) { body ->
            json.decodeFromString(FoodLookupConfigDto.serializer(), body)
        }
    }

    override suspend fun searchGeneric(
        baseUrl: String,
        apiKey: String,
        query: String
    ): FoodLookupOutcome<List<GenericLookupItemDto>> {
        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegments("v1/foods/generic")
            ?.addQueryParameter("q", query.trim())
            ?.build()
            ?: return FoodLookupOutcome.Error(FoodLookupError.Transport)

        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("X-GymLedger-Key", apiKey)
        }

        return executeRequest(requestBuilder.build()) { body ->
            val response = json.decodeFromString(GenericLookupResponseDto.serializer(), body)
            response.data.results.ifEmpty { null }
        }
    }

    private suspend fun <T> executeRequest(
        request: Request,
        parse: (String) -> T?
    ): FoodLookupOutcome<T> {
        return suspendCancellableCoroutine { continuation ->
            val call = callFactory.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resume(FoodLookupOutcome.Error(FoodLookupError.Transport))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!continuation.isActive) return

                        val outcome = mapResponse(response, parse)
                        continuation.resume(outcome)
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(FoodLookupOutcome.Error(FoodLookupError.MalformedResponse))
                        }
                    } finally {
                        response.body?.close()
                    }
                }
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> mapResponse(
        response: Response,
        parse: (String) -> T?
    ): FoodLookupOutcome<T> {
        val body = response.body?.string() ?: return FoodLookupOutcome.Error(FoodLookupError.MalformedResponse)

        return when (response.code) {
            200 -> {
                try {
                    val parsed = parse(body)
                    if (parsed == null) FoodLookupOutcome.Empty
                    else FoodLookupOutcome.Success(parsed)
                } catch (e: Exception) {
                    FoodLookupOutcome.Error(FoodLookupError.MalformedResponse)
                }
            }
            400 -> FoodLookupOutcome.Error(FoodLookupError.InvalidQuery)
            401 -> FoodLookupOutcome.Error(FoodLookupError.Unauthorized)
            429 -> FoodLookupOutcome.Error(FoodLookupError.BudgetExceeded)
            503 -> parse503Error(body)
            else -> FoodLookupOutcome.Error(FoodLookupError.Transport)
        }
    }

    private fun parse503Error(body: String): FoodLookupOutcome.Error {
        return try {
            val errorBody = json.decodeFromString(ErrorBody.serializer(), body)
            when (errorBody.error) {
                "lookup_disabled" -> FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
                "provider_disabled" -> FoodLookupOutcome.Error(FoodLookupError.ProviderDisabled)
                "feature_disabled" -> FoodLookupOutcome.Error(FoodLookupError.FeatureDisabled)
                "configuration_error" -> FoodLookupOutcome.Error(FoodLookupError.ConfigurationError)
                "provider_error" -> FoodLookupOutcome.Error(FoodLookupError.ProviderError)
                else -> FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
            }
        } catch (e: Exception) {
            FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
        }
    }

    @kotlinx.serialization.Serializable
    private data class ErrorBody(
        val error: String = ""
    )
}
