package com.edu.gymledger.data.remote

import com.edu.gymledger.data.remote.dto.ErrorResponseDto
import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.FoodLookupConfigResponseDto
import com.edu.gymledger.data.remote.dto.GenericLookupDataDto
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
            val envelope = json.decodeFromString(FoodLookupConfigResponseDto.serializer(), body)
            check(envelope.ok) { "Config response envelope has ok=false" }
            envelope.data
        }
    }

    override suspend fun searchGeneric(
        baseUrl: String,
        apiKey: String,
        query: String
    ): FoodLookupOutcome<GenericLookupDataDto> {
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
            val envelope = json.decodeFromString(GenericLookupResponseDto.serializer(), body)
            check(envelope.ok) { "Generic response envelope has ok=false" }
            envelope.data
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

    private fun <T> mapResponse(
        response: Response,
        parse: (String) -> T?
    ): FoodLookupOutcome<T> {
        val body = response.body?.string() ?: return FoodLookupOutcome.Error(FoodLookupError.MalformedResponse)

        return if (response.isSuccessful) {
            try {
                val parsed = parse(body)
                if (parsed == null) FoodLookupOutcome.Empty
                else FoodLookupOutcome.Success(parsed)
            } catch (e: Exception) {
                FoodLookupOutcome.Error(FoodLookupError.MalformedResponse)
            }
        } else {
            mapError(response.code, body)
        }
    }

    private fun <T> mapError(status: Int, body: String): FoodLookupOutcome<T> {
        val code = runCatching {
            json.decodeFromString(ErrorResponseDto.serializer(), body).error.code
        }.getOrNull()

        if (!code.isNullOrBlank()) {
            when (code) {
                "invalid_query" -> return FoodLookupOutcome.Error(FoodLookupError.InvalidQuery)
                "unauthorized" -> return FoodLookupOutcome.Error(FoodLookupError.Unauthorized)
                "lookup_disabled", "online_lookup_disabled" ->
                    return FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
                "provider_disabled" -> return FoodLookupOutcome.Error(FoodLookupError.ProviderDisabled)
                "feature_disabled" -> return FoodLookupOutcome.Error(FoodLookupError.FeatureDisabled)
                "budget_exceeded" -> return FoodLookupOutcome.Error(FoodLookupError.BudgetExceeded)
                "configuration_error" -> return FoodLookupOutcome.Error(FoodLookupError.ConfigurationError)
                "provider_error", "provider_unavailable", "provider_rate_limited" ->
                    return FoodLookupOutcome.Error(FoodLookupError.ProviderError)
                "provider_timeout" -> return FoodLookupOutcome.Error(FoodLookupError.Transport)
                "not_found" -> return FoodLookupOutcome.Empty
            }
        }

        return when (status) {
            400 -> FoodLookupOutcome.Error(FoodLookupError.InvalidQuery)
            401 -> FoodLookupOutcome.Error(FoodLookupError.Unauthorized)
            404 -> FoodLookupOutcome.Empty
            429 -> FoodLookupOutcome.Error(FoodLookupError.BudgetExceeded)
            502 -> FoodLookupOutcome.Error(FoodLookupError.ProviderError)
            503 -> FoodLookupOutcome.Error(FoodLookupError.LookupDisabled)
            504 -> FoodLookupOutcome.Error(FoodLookupError.Transport)
            else -> FoodLookupOutcome.Error(FoodLookupError.Transport)
        }
    }
}
