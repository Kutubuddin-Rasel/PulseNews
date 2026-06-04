package com.example.newsapp.data.repository

import android.content.Context
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import com.example.newsapp.Api.PulseBackendApi
import com.example.newsapp.Room.ArticleDao
import com.example.newsapp.data.util.DeviceIdProvider
import com.example.newsapp.data.util.FirestoreSyncManager
import com.example.newsapp.module.Article
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue

class BookmarkStressTest {

    private lateinit var repository: SavedArticleRepositoryImpl
    private lateinit var articleDao: ArticleDao
    private lateinit var firestoreSyncManager: FirestoreSyncManager
    private lateinit var context: Context
    private lateinit var backendApi: PulseBackendApi
    private lateinit var deviceIdProvider: DeviceIdProvider
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        articleDao = mockk(relaxed = true)
        firestoreSyncManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        backendApi = mockk(relaxed = true)
        deviceIdProvider = mockk(relaxed = true)
        
        // Mock WorkManager so enqueueUniqueWork does not throw during setup
        workManager = mockk(relaxed = true)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        repository = SavedArticleRepositoryImpl(
            articleDao = articleDao,
            firestoreSyncManager = firestoreSyncManager,
            context = context,
            pulseBackendApi = backendApi,
            deviceIdProvider = deviceIdProvider
        )
    }

    @Test
    fun `rapid concurrent clicks at the bookmark function before DB initializes`() = runBlocking(Dispatchers.IO) {
        // Simulate an uninitialized or slow DB by adding artificial delay in mock
        var dbInit = false
        coEvery { articleDao.upsertArticle(any()) } coAnswers {
            if (!dbInit) {
                delay(100) // DB initialization delay
                dbInit = true
            }
        }

        val article = Article(
            url = "https://example.com/stress",
            backendId = "backend_id_1",
            author = null,
            content = null,
            description = null,
            publishedAt = null,
            source = com.example.newsapp.module.Source(id = null, name = "Test"),
            title = "Stress Test",
            urlToImage = null
        )
        
        // Fire rapid concurrent clicks
        coroutineScope {
            val jobs = (1..100).map {
                async {
                    repository.saveArticle(article)
                }
            }
            jobs.awaitAll()
        }

        // Ideally, this test would crash if it exposes a race condition or
        // NullPointerException inside the bookmark logic, but we run it to reproduce the bug
        // from the SQA report.
        assertTrue(dbInit)
    }
}
