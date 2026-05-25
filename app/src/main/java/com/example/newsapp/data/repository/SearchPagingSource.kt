package com.example.newsapp.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.data.mapper.toDomainOrNull
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.module.Article
import retrofit2.HttpException
import java.io.IOException

class SearchPagingSource(
    private val api: PulseBackendApi,
    private val connectivityMonitor: ConnectivityMonitor,
    private val query: String
) : PagingSource<String, Article>() {

    override fun getRefreshKey(state: PagingState<String, Article>): String? {
        return null // Search results typically don't need mid-list refresh anchoring
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Article> {
        if (!connectivityMonitor.isOnline()) {
            return LoadResult.Error(IOException("You need an active internet connection to search the global news database."))
        }

        return try {
            val cursor = params.key
            val response = api.searchNews(query = query, cursor = cursor, limit = params.loadSize)

            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val articles = dtos.mapNotNull { it.toDomainOrNull() }
                
                val nextKey = if (dtos.isEmpty()) {
                    null
                } else {
                    dtos.last().id
                }

                LoadResult.Page(
                    data = articles,
                    prevKey = null, // Backend cursor pagination is generally forward-only
                    nextKey = nextKey
                )
            } else {
                LoadResult.Error(HttpException(response))
            }
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
