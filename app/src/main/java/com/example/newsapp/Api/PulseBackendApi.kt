package com.example.newsapp.Api

import com.example.newsapp.data.remote.dto.PulseArticleDto
import com.example.newsapp.data.remote.dto.PulseMetaDto
import com.example.newsapp.data.remote.dto.TaxonomyDto
import com.example.newsapp.data.remote.dto.BookmarkRequest
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Header

interface PulseBackendApi {
    @GET("api/news")
    suspend fun getNewsFeed(@Query("cursor") cursor: String?, @Query("limit") limit: Int = 20): Response<List<PulseArticleDto>>

    @GET("api/news/foryou")
    suspend fun getForYouFeed(@Query("cursor") cursor: String?, @Query("limit") limit: Int = 20): Response<List<PulseArticleDto>>

    @GET("api/news/meta")
    suspend fun getNewsMeta(): Response<PulseMetaDto>

    @GET("api/news/search")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<List<PulseArticleDto>>

    @GET("api/news/trending")
    suspend fun getTrendingTopics(): Response<List<String>>

    @GET("api/bookmarks")
    suspend fun getBookmarks(@Header("x-device-id") deviceId: String): Response<List<PulseArticleDto>>

    @POST("api/bookmarks")
    suspend fun addBookmark(
        @Header("x-device-id") deviceId: String,
        @Body request: BookmarkRequest
    ): Response<Unit>

    @DELETE("api/bookmarks/{articleId}")
    suspend fun removeBookmark(
        @Header("x-device-id") deviceId: String,
        @Path("articleId") articleId: String
    ): Response<Unit>

    @GET("api/taxonomy")
    suspend fun getTaxonomy(): Response<TaxonomyDto>
}
