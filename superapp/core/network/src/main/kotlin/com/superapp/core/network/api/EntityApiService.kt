package com.superapp.core.network.api

import com.superapp.core.network.model.EntityDto
import com.superapp.core.network.model.PaginatedResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EntityApiService {

    @GET("entities")
    suspend fun getEntities(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): PaginatedResponseDto<EntityDto>

    @GET("entities/{id}")
    suspend fun getEntityById(
        @Path("id") id: String
    ): EntityDto
}
