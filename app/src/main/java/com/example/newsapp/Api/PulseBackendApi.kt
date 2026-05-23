package com.example.newsapp.Api

import com.example.newsapp.data.remote.dto.PulseArticleDto
import com.example.newsapp.data.remote.dto.PulseMetaDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PulseBackendApi {
    @GET("api/news")
    suspend fun getNewsFeed(@Query("page") page: Int): Response<List<PulseArticleDto>>

    @GET("api/news/meta")
    suspend fun getNewsMeta(): Response<PulseMetaDto>
}
