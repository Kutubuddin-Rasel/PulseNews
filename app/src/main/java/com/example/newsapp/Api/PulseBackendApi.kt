package com.example.newsapp.Api

import com.example.newsapp.data.remote.dto.PulseArticleDto
import com.example.newsapp.data.remote.dto.PulseMetaDto
import com.example.newsapp.data.remote.dto.TaxonomyDto
import com.example.newsapp.data.remote.dto.BookmarkRequest
import com.example.newsapp.data.remote.dto.TelemetryBatchRequest
import com.example.newsapp.data.remote.dto.TrendingTopicDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers

interface PulseBackendApi {
    @GET("api/news")
    suspend fun getNewsFeed(
        @Query("page") page: Int?, 
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("edition") edition: String? = null
    ): Response<List<PulseArticleDto>>

    @Headers("X-Pulse-Auth: required")
    @GET("api/news/foryou")
    suspend fun getForYouFeed(
        @Query("page") page: Int?, 
        @Query("limit") limit: Int = 20,
        @Query("weights") weights: String? = null
    ): Response<List<PulseArticleDto>>

    @GET("api/news/meta")
    suspend fun getNewsMeta(): Response<PulseMetaDto>

    @GET("api/news/search")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int = 20
    ): Response<List<PulseArticleDto>>

    @GET("api/news/trending")
    suspend fun getTrendingTopics(): Response<List<TrendingTopicDto>>

    @Headers("X-Pulse-Auth: required")
    @GET("api/bookmarks")
    suspend fun getBookmarks(
        @Header("x-device-id") deviceId: String,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null
    ): Response<com.example.newsapp.data.remote.dto.PaginatedBookmarksDto>

    @Headers("X-Pulse-Auth: required")
    @POST("api/bookmarks")
    suspend fun addBookmark(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Header("x-device-id") deviceId: String,
        @Body request: BookmarkRequest
    ): Response<Unit>

    @Headers("X-Pulse-Auth: required")
    @DELETE("api/bookmarks/{articleId}")
    suspend fun removeBookmark(
        @Header("x-device-id") deviceId: String,
        @Path("articleId") articleId: String
    ): Response<Unit>

    @GET("api/taxonomy")
    suspend fun getTaxonomy(): Response<TaxonomyDto>

    @Headers("X-Pulse-Auth: required")
    @POST("api/news/telemetry")
    suspend fun postInteractions(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: TelemetryBatchRequest
    ): Response<Unit>

    @Headers("X-Pulse-Auth: required")
    @POST("api/news/{id}/summary")
    suspend fun getAiSummary(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Path("id") articleId: String,
        @Body request: com.example.newsapp.data.remote.dto.AiSummaryRequest
    ): Response<com.example.newsapp.data.remote.dto.AiSummaryResponse>
}
