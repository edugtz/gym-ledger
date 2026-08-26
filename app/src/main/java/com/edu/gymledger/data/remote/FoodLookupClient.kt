package com.edu.gymledger.data.remote

import com.edu.gymledger.data.remote.dto.FoodLookupConfigDto
import com.edu.gymledger.data.remote.dto.GenericLookupDataDto
import com.edu.gymledger.data.remote.dto.PackagedFoodLookupDataDto

interface FoodLookupClient {
    suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<FoodLookupConfigDto>
    suspend fun searchGeneric(
        baseUrl: String,
        apiKey: String,
        query: String
    ): FoodLookupOutcome<GenericLookupDataDto>
    suspend fun lookupBarcode(
        baseUrl: String,
        apiKey: String,
        barcode: String
    ): FoodLookupOutcome<PackagedFoodLookupDataDto>
}
