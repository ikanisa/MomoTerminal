package com.superapp.core.domain.model

data class Entity(
    val id: String,
    val type: String,
    val title: String,
    val description: String?,
    val metadata: Map<String, Any>,
    val status: EntityStatus,
    val createdAt: Long,
    val updatedAt: Long
)

enum class EntityStatus {
    ACTIVE,
    INACTIVE,
    ARCHIVED
}

data class PaginatedResult<T>(
    val data: List<T>,
    val pagination: Pagination
)

data class Pagination(
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalItems: Int
)
