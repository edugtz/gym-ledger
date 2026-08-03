package com.edu.gymledger.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointValidatorTest {

    @Test
    fun blankInput_returnsDefaultUrl() {
        val result = EndpointValidator.resolve("")
        assertTrue(result is EndpointResult.Valid)
        val url = (result as EndpointResult.Valid).url
        assertEquals("https", url.scheme)
        assertEquals("gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev", url.host)
        assertEquals("/", url.encodedPath)
    }

    @Test
    fun whitespaceInput_returnsDefaultUrl() {
        val result = EndpointValidator.resolve("   ")
        assertTrue(result is EndpointResult.Valid)
        assertEquals(EndpointValidator.defaultUrl, (result as EndpointResult.Valid).url)
    }

    @Test
    fun validHttpsUrl_normalizedWithTrailingSlash() {
        val result = EndpointValidator.resolve("https://example.com/api")
        assertTrue(result is EndpointResult.Valid)
        val url = (result as EndpointResult.Valid).url
        assertEquals("https", url.scheme)
        assertEquals("example.com", url.host)
        assertEquals("/api/", url.encodedPath)
    }

    @Test
    fun validHttpsUrl_alreadyHasTrailingSlash_unchanged() {
        val result = EndpointValidator.resolve("https://example.com/api/")
        assertTrue(result is EndpointResult.Valid)
        assertEquals("/api/", (result as EndpointResult.Valid).url.encodedPath)
    }

    @Test
    fun validHttpsUrl_doubleTrailingSlash_normalizedToOne() {
        val result = EndpointValidator.resolve("https://example.com/api//")
        assertTrue(result is EndpointResult.Valid)
        assertEquals("/api/", (result as EndpointResult.Valid).url.encodedPath)
    }

    @Test
    fun validHttpsUrl_tripleTrailingSlash_normalizedToOne() {
        val result = EndpointValidator.resolve("https://example.com/api///")
        assertTrue(result is EndpointResult.Valid)
        assertEquals("/api/", (result as EndpointResult.Valid).url.encodedPath)
    }

    @Test
    fun validHttpsUrl_rootPathMultipleSlashes_normalizedToOne() {
        val result = EndpointValidator.resolve("https://example.com///")
        assertTrue(result is EndpointResult.Valid)
        assertEquals("/", (result as EndpointResult.Valid).url.encodedPath)
    }

    @Test
    fun httpScheme_returnsInvalid() {
        val result = EndpointValidator.resolve("http://example.com/api")
        assertTrue(result is EndpointResult.Invalid)
    }

    @Test
    fun userInfo_returnsInvalid() {
        val result = EndpointValidator.resolve("https://user:pass@example.com/api")
        assertTrue(result is EndpointResult.Invalid)
    }

    @Test
    fun queryParam_returnsInvalid() {
        val result = EndpointValidator.resolve("https://example.com/api?foo=bar")
        assertTrue(result is EndpointResult.Invalid)
    }

    @Test
    fun fragment_returnsInvalid() {
        val result = EndpointValidator.resolve("https://example.com/api#section")
        assertTrue(result is EndpointResult.Invalid)
    }

    @Test
    fun malformedUrl_returnsInvalid() {
        val result = EndpointValidator.resolve("not-a-url")
        assertTrue(result is EndpointResult.Invalid)
    }

    @Test
    fun defaultUrl_isHttps() {
        assertEquals("https", EndpointValidator.defaultUrl.scheme)
    }

    @Test
    fun defaultUrl_hasTrailingSlash() {
        assertEquals("/", EndpointValidator.defaultUrl.encodedPath)
    }

    @Test
    fun preservesBasePath() {
        val result = EndpointValidator.resolve("https://example.com/custom/path")
        assertTrue(result is EndpointResult.Valid)
        assertEquals("/custom/path/", (result as EndpointResult.Valid).url.encodedPath)
    }
}
