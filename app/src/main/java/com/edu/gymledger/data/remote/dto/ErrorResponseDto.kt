package com.edu.gymledger.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val ok: Boolean = false,
    val error: ErrorDetailDto = ErrorDetailDto()
)

@Serializable
data class ErrorDetailDto(
    val code: String = "",
    val message: String = ""
)
