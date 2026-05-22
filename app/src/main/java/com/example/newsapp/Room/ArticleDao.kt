package com.example.newsapp.Room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Upsert
    suspend fun upsertArticle(article: Article)

    @Query("SELECT * FROM saved_articles ORDER BY title ASC")
    fun allArticle(): Flow<List<Article>>

    @Delete
    suspend fun delete(article: Article)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_articles WHERE url = :url)")
    suspend fun isSaved(url: String): Boolean

    @Query("SELECT * FROM saved_articles WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): Article?
}
