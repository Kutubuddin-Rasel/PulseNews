package com.example.newsapp.data.util

import android.util.Log
import com.example.newsapp.Room.ArticleDao
import com.example.newsapp.module.Article
import com.example.newsapp.module.ArticleTaxonomy
import com.example.newsapp.module.Source
import com.example.newsapp.domain.model.GamificationProfile
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

import com.example.newsapp.domain.manager.FirestoreSyncManager
import com.example.newsapp.domain.repository.AuthRepository
import com.example.newsapp.domain.tracker.EngagementTracker

@Singleton
class FirestoreSyncManagerImpl @Inject constructor(
    private val authManager: AuthRepository,
    private val articleDao: ArticleDao,
    private val engagementTracker: dagger.Lazy<EngagementTracker>
) : FirestoreSyncManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    // FS1: hold each registration so we can detach it on user change / logout. Discarding these
    // (as before) leaks listeners that keep firing on the previous uid's documents.
    private var savedArticlesListener: ListenerRegistration? = null
    private var preferencesListener: ListenerRegistration? = null
    private var gamificationListener: ListenerRegistration? = null

    init {
        scope.launch {
            authManager.currentUser.collectLatest { user ->
                // FS1: tear down the previous user's listeners before (re)subscribing. Without this,
                // every login/logout/account-switch stacks listeners → leak, duplicate Room writes,
                // ongoing Firestore read billing, and the prior account's data bleeding into the
                // new session.
                removeAllListeners()
                if (user != null) {
                    startListeningToSavedArticles(user.uid)
                    startListeningToPreferences(user.uid)
                    startListeningToGamification(user.uid)
                }
            }
        }
    }

    private fun removeAllListeners() {
        savedArticlesListener?.remove(); savedArticlesListener = null
        preferencesListener?.remove(); preferencesListener = null
        gamificationListener?.remove(); gamificationListener = null
    }

    private fun startListeningToSavedArticles(uid: String) {
        savedArticlesListener = firestore.collection("users").document(uid).collection("saved_articles")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreSyncManager", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch {
                    // FS4: apply remote deletions too — process the delta instead of upsert-only.
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        try {
                            when (change.type) {
                                DocumentChange.Type.REMOVED -> {
                                    val url = doc.getString("url")
                                    if (!url.isNullOrEmpty()) articleDao.deleteByUrl(url)
                                }
                                else -> articleDao.upsertArticle(doc.toArticle()) // ADDED / MODIFIED
                            }
                        } catch (ex: Exception) {
                            Log.e("FirestoreSyncManager", "Error syncing saved article", ex)
                        }
                    }
                }
            }
    }

    /** FS3: reconstruct the full [Article] shape (backendId, scores, region, summary, taxonomy). */
    private fun DocumentSnapshot.toArticle(): Article {
        @Suppress("UNCHECKED_CAST")
        val taxonomyMap = get("taxonomy") as? Map<String, Any?>
        val taxonomy = taxonomyMap?.let {
            @Suppress("UNCHECKED_CAST")
            ArticleTaxonomy(
                categories = (it["categories"] as? List<String>) ?: emptyList(),
                tags = (it["tags"] as? List<String>) ?: emptyList(),
                mlConfidence = (it["mlConfidence"] as? Number)?.toFloat(),
                id = it["id"] as? String,
                articleId = it["articleId"] as? String
            )
        }
        return Article(
            url = getString("url") ?: "",
            backendId = getString("backendId") ?: "",
            author = getString("author"),
            content = getString("content"),
            description = getString("description"),
            publishedAt = getString("publishedAt"),
            source = Source(id = getString("sourceId"), name = getString("sourceName") ?: ""),
            title = getString("title") ?: "",
            urlToImage = getString("urlToImage"),
            summary = getString("summary"),
            regionCode = getString("regionCode"),
            sourceTier = getLong("sourceTier")?.toInt(),
            gravity_score = (get("gravity_score") as? Number)?.toFloat(),
            personalized_score = (get("personalized_score") as? Number)?.toDouble(),
            distance = (get("distance") as? Number)?.toDouble(),
            taxonomy = taxonomy
        )
    }

    override suspend fun pushArticleSave(article: Article) = withContext(Dispatchers.IO) {
        val user = authManager.currentUser.value ?: return@withContext
        try {
            // Encode URL to make it a valid Firestore document ID
            val docId = URLEncoder.encode(article.url, StandardCharsets.UTF_8.toString())
            
            // FS3: persist the full article shape so the down-sync can reconstruct it losslessly.
            val articleMap = hashMapOf(
                "url" to article.url,
                "backendId" to article.backendId,
                "author" to article.author,
                "content" to article.content,
                "description" to article.description,
                "publishedAt" to article.publishedAt,
                "sourceId" to article.source.id,
                "sourceName" to article.source.name,
                "title" to article.title,
                "urlToImage" to article.urlToImage,
                "summary" to article.summary,
                "regionCode" to article.regionCode,
                "sourceTier" to article.sourceTier,
                "gravity_score" to article.gravity_score,
                "personalized_score" to article.personalized_score,
                "distance" to article.distance,
                "taxonomy" to article.taxonomy?.let {
                    hashMapOf(
                        "categories" to it.categories,
                        "tags" to it.tags,
                        "mlConfidence" to it.mlConfidence,
                        "id" to it.id,
                        "articleId" to it.articleId
                    )
                },
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

    override suspend fun pushArticleUnsave(url: String) = withContext(Dispatchers.IO) {
        val user = authManager.currentUser.value ?: return@withContext
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
        preferencesListener = firestore.collection("users").document(uid).collection("preferences").document("algorithm")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                // Downstream preference sync would go here, requiring AlgorithmPreferencesRepository injection.
                // To avoid circular dependency, it's better to expose a StateFlow of remote preferences
                // or just handle it here if we use a callback or event bus.
            }
    }

    override suspend fun pushPreferences(tech: Float, politics: Float, global: Float, business: Float, health: Float) = withContext(Dispatchers.IO) {
        val user = authManager.currentUser.value ?: return@withContext
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
        gamificationListener = firestore.collection("users").document(uid).collection("gamification").document("profile")
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

    override fun pushGamificationState(profile: GamificationProfile) {
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
