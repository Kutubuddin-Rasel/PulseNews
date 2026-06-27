package com.example.newsapp.Room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * O4 — real Room migrations.
 *
 * v17 is the exported migration baseline (see [ArticleDatabase], `exportSchema = true`). Every
 * schema change from this version forward MUST register a [Migration] here and add a
 * MigrationTest validated against the committed `app/schemas/…/<version>.json`, so upgrades
 * preserve the user's database instead of dropping it.
 *
 * v1–v16 were never exported (so they are un-migratable) and the app is unreleased
 * (`versionCode = 1` — no production databases exist below v17), which is why migrations are
 * adopted from the v16→v17 step onward rather than retroactively from v1.
 */
object DatabaseMigrations {

    /**
     * v16 → v17 — the "Second Wave" schema change, made non-destructive:
     *   - O3: add the explicit paging-key table `feed_remote_keys`.
     *   - O6: drop the redundant unique index on `saved_articles.url` (url is the PRIMARY KEY,
     *     which Room already backs with a unique index, so the explicit index was pure write cost).
     *
     * The `CREATE TABLE` is copied verbatim from the generated
     * `app/schemas/com.example.newsapp.Room.ArticleDatabase/17.json` so the post-migration schema
     * hashes identically to the compiled v17 schema (identityHash 545b7a2394ea2d09366f9cd2106eb4e2);
     * any drift would make Room throw "Migration didn't properly handle …" on a real upgrade.
     */
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `feed_remote_keys` " +
                    "(`feedKey` TEXT NOT NULL, `nextPage` INTEGER, PRIMARY KEY(`feedKey`))"
            )
            // Room's auto-generated name for @Index(value = ["url"], unique = true) on saved_articles.
            db.execSQL("DROP INDEX IF EXISTS `index_saved_articles_url`")
        }
    }

    /** All migrations in ascending order. Spread into the builder in [com.example.newsapp.Hilt.DatabaseModule]. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_16_17)
}
