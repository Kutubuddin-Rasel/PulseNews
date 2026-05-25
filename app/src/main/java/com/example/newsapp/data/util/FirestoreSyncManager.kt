package com.example.newsapp.data.util

import android.util.Log
import com.example.newsapp.Room.ArticleDao
import com.example.newsapp.module.Article
import com.example.newsapp.module.Source
import com.example.newsapp.domain.model.GamificationProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncManager @Inject constructor(
    private val authManager: AuthManager,
    private val articleDao: ArticleDao,
    private val engagementTracker: dagger.Lazy<EngagementTracker>
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            authManager.currentUser.collectLatest { user ->
                if (user != null) {
                    // Start syncing down
                    startListeningToSavedArticles(user.uid)
                    startListeningToPreferences(user.uid)
                    startListeningToGamification(user.uid)
                }
            }
        }
    }

    private fun startListeningToSavedArticles(uid: String) {
        firestore.collection("users").document(uid).collection("saved_articles")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreSyncManager", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val article = Article(
                                    url = doc.getString("url") ?: "",
                                    author = doc.getString("author"),
                                    content = doc.getString("content"),
                                    description = doc.getString("description"),
                                    publishedAt = doc.getString("publishedAt"),
                                    source = Source(
                                        id = doc.getString("sourceId"),
                                        name = doc.getString("sourceName") ?: ""
                                    ),
                                    title = doc.getString("title") ?: "",
                                    urlToImage = doc.getString("urlToImage")
                                )
                                // Merge remote state down to local Room DB
                                articleDao.upsertArticle(article)
                            } catch (ex: Exception) {
                                Log.e("FirestoreSyncManager", "Error parsing article", ex)
                            }
                        }
                    }
                }
            }
    }

    suspend fun pushArticleSave(article: Article) {
        val user = authManager.currentUser.value ?: return
        try {
            // Encode URL to make it a valid Firestore document ID
            val docId = URLEncoder.encode(article.url, StandardCharsets.UTF_8.toString())
            
            val articleMap = hashMapOf(
                "url" to article.url,
                "author" to article.author,
                "content" to article.content,
                "description" to article.description,
                "publishedAt" to article.publishedAt,
                "sourceId" to article.source.id,
                "sourceName" to article.source.name,
                "title" to article.title,
                "urlToImage" to article.urlToImage,
                "savedAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(user.uid)
                .collection("saved_articles").document(docId)
                .set(articleMap)
                // We don't await because Firestore SDK handles offline queuing seamlessly
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error pushing article save", e)
        }
    }

    suspend fun pushArticleUnsave(url: String) {
        val user = authManager.currentUser.value ?: return
        try {
            val docId = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            firestore.collection("users").document(user.uid)
                .collection("saved_articles").document(docId)
                .delete()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error pushing article unsave", e)
        }
    }

    private fun startListeningToPreferences(uid: String) {
        firestore.collection("users").document(uid).collection("preferences").document("algorithm")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                // Downstream preference sync would go here, requiring AlgorithmPreferencesRepository injection.
                // To avoid circular dependency, it's better to expose a StateFlow of remote preferences
                // or just handle it here if we use a callback or event bus.
            }
    }

    suspend fun pushPreferences(tech: Float, politics: Float, global: Float, business: Float, health: Float) {
        val user = authManager.currentUser.value ?: return
        try {
            val prefMap = hashMapOf(
                "tech" to tech,
                "politics" to politics,
                "global" to global,
                "business" to business,
                "health" to health,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid)
                .collection("preferences").document("algorithm")
                .set(prefMap)
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error pushing preferences", e)
        }
    }

    private fun startListeningToGamification(uid: String) {
        firestore.collection("users").document(uid).collection("gamification").document("profile")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                
                try {
                    val currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0
                    val longestStreak = snapshot.getLong("longestStreak")?.toInt() ?: 0
                    val totalArticlesRead = snapshot.getLong("totalArticlesRead")?.toInt() ?: 0
                    val lastReadDateEpochDay = snapshot.getLong("lastReadDateEpochDay") ?: 0L
                    val lastSyncedAt = snapshot.getLong("lastSyncedAt") ?: 0L
                    
                    @Suppress("UNCHECKED_CAST")
                    val categoryCountsMap = snapshot.get("categoryReadCounts") as? Map<String, Int> ?: emptyMap()
                    
                    val profile = GamificationProfile(
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        totalArticlesRead = totalArticlesRead,
                        lastReadDateEpochDay = lastReadDateEpochDay,
                        categoryReadCounts = categoryCountsMap,
                        lastSyncedAt = lastSyncedAt
                    )
                    
                    scope.launch {
                        engagementTracker.get().updateFromRemote(profile)
                    }
                } catch (ex: Exception) {
                    Log.e("FirestoreSyncManager", "Error parsing gamification profile", ex)
                }
            }
    }

    fun pushGamificationState(profile: GamificationProfile) {
        val user = authManager.currentUser.value ?: return
        try {
            val syncTime = System.currentTimeMillis()
            val profileMap = hashMapOf(
                "currentStreak" to profile.currentStreak,
                "longestStreak" to profile.longestStreak,
                "totalArticlesRead" to profile.totalArticlesRead,
                "lastReadDateEpochDay" to profile.lastReadDateEpochDay,
                "categoryReadCounts" to profile.categoryReadCounts,
                "lastSyncedAt" to syncTime
            )
            
            firestore.collection("users").document(user.uid)
                .collection("gamification").document("profile")
                .set(profileMap)
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error pushing gamification profile", e)
        }
    }
}
