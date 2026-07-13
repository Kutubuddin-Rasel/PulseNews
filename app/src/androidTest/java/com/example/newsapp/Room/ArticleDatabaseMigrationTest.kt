package com.example.newsapp.Room

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * O4 — migration test harness for [ArticleDatabase].
 *
 * [MigrationTestHelper] builds a database from a committed `app/schemas/…/<version>.json`, lets
 * you apply a [androidx.room.migration.Migration], then validates the result against the next
 * version's schema. This is an **instrumented** test (it runs on a device/emulator, not the JVM),
 * so it executes under `connectedDebugAndroidTest`, not `testDebugUnitTest`.
 *
 * The schema JSONs are exposed to this test as androidTest assets via the `room.schemaLocation`
 * KSP arg + the `androidTest` assets source set (see app/build.gradle.kts).
 */
@RunWith(AndroidJUnit4::class)
class ArticleDatabaseMigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ArticleDatabase::class.java
    )

    /**
     * Infrastructure smoke test: the exported v17 baseline schema must build cleanly. This guards
     * against the schema export drifting from the entities and confirms the helper is wired to the
     * committed JSONs. Uses the authentic generated 17.json (no fabricated schema).
     */
    @Test
    fun baselineV17_schemaOpens() {
        helper.createDatabase(testDb, 17).close()
    }

    /**
     * v16 → v17 round-trip: a seeded saved_articles row must survive [DatabaseMigrations.MIGRATION_16_17]
     * (O3 adds feed_remote_keys, O6 drops the url index — neither touches saved_articles data).
     *
     * @Ignore until a v16 schema JSON exists. v16 predates exportSchema, so Room never emitted
     * `16.json`. To enable this test, generate it once: on a scratch branch set
     * [ArticleDatabase] `version = 16`, remove the `FeedRemoteKey` entity + `feedRemoteKeyDao()`,
     * re-add `@Index(value = ["url"], unique = true)` on [com.example.newsapp.module.Article], build
     * (KSP writes `16.json`), then restore v17 and commit only the JSON. After that, drop @Ignore.
     */
    @Test
    @Ignore("Needs app/schemas/.../16.json — v16 predates exportSchema; see KDoc for one-time generation.")
    fun migrate16To17_preservesSavedArticles() {
        helper.createDatabase(testDb, 16).apply {
            execSQL(
                "INSERT INTO saved_articles " +
                    "(url, backendId, source, title) VALUES " +
                    "('https://example.com/a', 'b1', '{\"id\":null,\"name\":\"Example\"}', 'Hello')"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            testDb, 17, /* validateDroppedTables = */ true, DatabaseMigrations.MIGRATION_16_17
        )

        migrated.query("SELECT title FROM saved_articles WHERE url = 'https://example.com/a'").use {
            check(it.moveToFirst()) { "seeded saved_articles row was lost across the migration" }
            check(it.getString(0) == "Hello")
        }
        migrated.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_saved_articles_url'").use {
            check(!it.moveToFirst()) { "O6 url index should be gone after migration" }
        }
        migrated.close()
    }

    /**
     * v17 → v18: keyset pagination adds the nullable `nextCursor` column to feed_remote_keys.
     * A pre-existing v17 row (page-based paging key) must survive [DatabaseMigrations.MIGRATION_17_18]
     * — a nullable ADD COLUMN never drops data — and read back `nextCursor == NULL`.
     */
    @Test
    fun migrate17To18_addsNextCursorColumn_preservesRows() {
        helper.createDatabase(testDb, 17).apply {
            execSQL("INSERT INTO feed_remote_keys (feedKey, nextPage) VALUES ('firehose', 2)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            testDb, 18, /* validateDroppedTables = */ true, DatabaseMigrations.MIGRATION_17_18
        )

        migrated.query("SELECT nextPage, nextCursor FROM feed_remote_keys WHERE feedKey = 'firehose'").use {
            check(it.moveToFirst()) { "seeded feed_remote_keys row was lost across the migration" }
            check(it.getInt(0) == 2) { "nextPage must be preserved across the migration" }
            check(it.isNull(1)) { "pre-existing row's nextCursor must default to NULL" }
        }
        migrated.close()
    }
}
