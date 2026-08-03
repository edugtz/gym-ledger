package com.edu.gymledger.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object EndpointValidator {

    private const val DEFAULT_WORKER_URL = "https://gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev/"

    val defaultUrl: HttpUrl = DEFAULT_WORKER_URL.toHttpUrlOrNull()
        ?: throw IllegalStateException("Default worker URL is invalid")

    fun resolve(input: String): EndpointResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return EndpointResult.Valid(defaultUrl)
        }

        val url = trimmed.toHttpUrlOrNull() ?: return EndpointResult.Invalid

        if (url.scheme != "https") return EndpointResult.Invalid
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) return EndpointResult.Invalid
        if (url.querySize > 0) return EndpointResult.Invalid
        if (url.fragment != null) return EndpointResult.Invalid

        val normalized = url.newBuilder()
            .encodedPath(normalizeTrailingSlash(url.encodedPath))
            .build()

        return EndpointResult.Valid(normalized)
    }

    private fun normalizeTrailingSlash(path: String): String {
        return if (path.endsWith("/")) path else "$path/"
    }
}

sealed interface EndpointResult {
    data class Valid(val url: HttpUrl) : EndpointResult
    data object Invalid : EndpointResult
}
