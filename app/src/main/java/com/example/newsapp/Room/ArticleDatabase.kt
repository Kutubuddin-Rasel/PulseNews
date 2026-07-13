package com.example.newsapp.Room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.newsapp.module.Article

@Database(
    entities = [Article::class, CachedFeedArticleEntity::class, InteractionEventEntity::class, TrendingTopicEntity::class, FeedRemoteKey::class],
    // v17: O3 adds the feed_remote_keys table (explicit paging keys); O6 drops the redundant
    // unique index on saved_articles.url.
    //
    // O4 — schema export + real migrations. exportSchema = true makes Room emit a versioned
    // schema JSON (app/schemas/) on every build; v17 is the migration BASELINE. v1–v16 were never
    // exported (so they are un-migratable) and the app is unreleased (versionCode 1, no production
    // DBs below v17), so adopting migrations from this baseline forward is the correct retrofit.
    // The concrete v16→v17 step is implemented in DatabaseMigrations.MIGRATION_16_17; upgrades are
    // no longer destructive (see DatabaseModule). Every future version bump MUST ship a Migration +
    // a MigrationTest validated against the exported JSON.
    //
    // v18: keyset pagination adds the nullable feed_remote_keys.nextCursor column
    // (DatabaseMigrations.MIGRATION_17_18) so firehose/category feeds seek by opaque cursor.
    version = 18,
    exportSchema = true
)
@TypeConverters(TypeConverter::class)
abstract class ArticleDatabase : RoomDatabase() {
    abstract fun articledao(): ArticleDao
    abstract fun cachedFeedDao(): CachedFeedDao
    abstract fun interactionEventDao(): InteractionEventDao
    abstract fun trendingTopicDao(): TrendingTopicDao
    abstract fun feedRemoteKeyDao(): FeedRemoteKeyDao
}
