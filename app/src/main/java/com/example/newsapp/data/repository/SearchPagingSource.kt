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
    private val searchResultCache: SearchResultCache,
    private val query: String,
    // Related-Perspectives: the current article's backend id, excluded server-side
    // so the story you're reading never appears among its own alternative views.
    private val excludeId: String? = null
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
            val response = api.searchNews(query = query, page = page, limit = params.loadSize, excludeId = excludeId)

            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val articles = dtos.mapNotNull { it.toDomainOrNull() }
                // CONF5: log silent drops so a contract regression is observable, not invisible.
                val dropped = dtos.size - articles.size
                if (dropped > 0) {
                    android.util.Log.w(
                        "ArticleMapper",
                        "Dropped $dropped/${dtos.size} search articles (query=$query) — missing link/title"
                    )
                }

                // S3: persistence is delegated to SearchResultCache (SRP) — this source's only job
                // is to turn a query+page into a LoadResult. The cache owns the F1 clear-on-new-search
                // policy so the local search partition can't grow without bound.
                searchResultCache.cache(articles, page, params.loadSize)
                
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
