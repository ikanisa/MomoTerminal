package com.superapp.core.data.mapper

import com.google.gson.Gson
import com.superapp.core.database.entity.EntityEntity
import com.superapp.core.domain.model.Entity
import com.superapp.core.domain.model.EntityStatus
import com.superapp.core.domain.model.PaginatedResult
import com.superapp.core.domain.model.Pagination
import com.superapp.core.network.model.EntityDto
import com.superapp.core.network.model.PaginatedResponseDto
import com.superapp.core.network.model.PaginationDto
import java.time.Instant

private val gson = Gson()

fun EntityDto.toDomain(): Entity {
    return Entity(
        id = id,
        type = type,
        title = title,
        description = description,
        metadata = metadata,
        status = EntityStatus.valueOf(status.uppercase()),
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli()
    )
}

fun EntityDto.toEntity(): EntityEntity {
    return EntityEntity(
        id = id,
        type = type,
        title = title,
        description = description,
        metadataJson = gson.toJson(metadata),
        status = status,
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli()
    )
}

fun EntityEntity.toDomain(): Entity {
    return Entity(
        id = id,
        type = type,
        title = title,
        description = description,
        metadata = gson.fromJson(metadataJson, Map::class.java) as Map<String, Any>,
        status = EntityStatus.valueOf(status.uppercase()),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PaginationDto.toDomain(): Pagination {
    return Pagination(
        page = page,
        pageSize = pageSize,
        totalPages = totalPages,
        totalItems = totalItems
    )
}

fun <T, R> PaginatedResponseDto<T>.toDomain(mapper: (T) -> R): PaginatedResult<R> {
    return PaginatedResult(
        data = data.map(mapper),
        pagination = pagination.toDomain()
    )
}
