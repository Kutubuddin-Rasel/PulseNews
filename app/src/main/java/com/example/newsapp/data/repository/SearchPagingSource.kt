package com.example.newsapp.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.data.mapper.toDomainOrNull
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.module.Article
import com.example.newsapp.data.mapper.toCacheEntity
import com.example.newsapp.Room.ArticleDatabase
import retrofit2.HttpException
import java.io.IOException

class SearchPagingSource(
    private val api: PulseBackendApi,
    private val connectivityMonitor: ConnectivityMonitor,
    private val database: ArticleDatabase,
    private val query: String
) : PagingSource<Int, Article>() {

    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        if (!connectivityMonitor.isOnline()) {
            return LoadResult.Error(IOException("You need an active internet connection to search the global news database."))
        }

        return try {
            val page = params.key ?: 1
            val response = api.searchNews(query = query, page = page, limit = params.loadSize)

            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val articles = dtos.mapNotNull { it.toDomainOrNull() }
                
                // Cache the search results in the database so ArticleDetailScreen can find them locally
                val fetchedAt = System.currentTimeMillis()
                val cacheEntities = articles.mapIndexed { index, article -> 
                    article.toCacheEntity("search", (page - 1) * params.loadSize + index, fetchedAt) 
                }
                database.cachedFeedDao().upsertAll(cacheEntities)
                
                val nextKey = if (dtos.isEmpty() || dtos.size < params.loadSize) {
                    null
                } else {
                    page + 1
                }

                LoadResult.Page(
                    data = articles,
                    prevKey = if (page == 1) null else page - 1,
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
