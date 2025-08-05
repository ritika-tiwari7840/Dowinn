package com.ritika.dowinnApp.api.dataclasses

data class ApiResponse<T>(
    val status: String,
    val payload: T? = null,
    val message: String? = null,
    val error: String? = null,
    val errors: Map<String, List<String>>? = null
)
