package com.edu.gymledger.data.remote

import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.GenericLookupItemDto

interface FoodLookupClient {
    suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<FoodLookupConfigDto>
    suspend fun searchGeneric(
        baseUrl: String,
        apiKey: String,
        query: String
    ): FoodLookupOutcome<List<GenericLookupItemDto>>
}
