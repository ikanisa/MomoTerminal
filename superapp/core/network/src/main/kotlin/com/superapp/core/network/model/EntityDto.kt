package com.superapp.core.network.model

import com.google.gson.annotations.SerializedName

data class EntityDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("metadata") val metadata: Map<String, Any>,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class PaginatedResponseDto<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("pagination") val pagination: PaginationDto
)

data class PaginationDto(
    @SerializedName("page") val page: Int,
    @SerializedName("pageSize") val pageSize: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("totalItems") val totalItems: Int
)

data class ErrorResponseDto(
    @SerializedName("error") val error: ErrorDto
)

data class ErrorDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("details") val details: Map<String, Any>?
)
