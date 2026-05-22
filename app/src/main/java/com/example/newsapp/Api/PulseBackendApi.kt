package com.example.newsapp.Api

import com.example.newsapp.data.remote.dto.PulseArticleDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PulseBackendApi {
    @GET("api/news")
    suspend fun getNewsFeed(@Query("page") page: Int): Response<List<PulseArticleDto>>
}
