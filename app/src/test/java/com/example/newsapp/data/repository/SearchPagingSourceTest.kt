package com.example.newsapp.data.repository

import androidx.paging.PagingSource
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.domain.util.ConnectivityMonitor
import com.example.newsapp.module.Article
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class SearchPagingSourceTest {

    private lateinit var api: PulseBackendApi
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var pagingSource: SearchPagingSource

    @Before
    fun setup() {
        api = mockk()
        connectivityMonitor = mockk()
        pagingSource = SearchPagingSource(api, connectivityMonitor, "test query")
    }

    @Test
    fun `loadState append returns Error state when network fails on append`() = runTest {
        // Mock connectivity to be true initially
        coEvery { connectivityMonitor.isOnline() } returns true

        // Simulate an HTTP error for the append load
        val errorResponse = Response.error<List<com.example.newsapp.data.remote.dto.PulseArticleDto>>(
            500, "Internal Server Error".toResponseBody(null)
        )
        
        coEvery { 
            api.searchNews(query = "test query", cursor = "next_cursor_123", limit = 10) 
        } returns errorResponse

        // Force loadState.append (simulated by calling load with LoadParams.Append)
        val loadParams = PagingSource.LoadParams.Append(
            key = "next_cursor_123",
            loadSize = 10,
            placeholdersEnabled = false
        )

        val result = pagingSource.load(loadParams)

        // Verify that the result is an Error state, to reproduce the bug
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertTrue(errorResult.throwable is retrofit2.HttpException)
    }
}
