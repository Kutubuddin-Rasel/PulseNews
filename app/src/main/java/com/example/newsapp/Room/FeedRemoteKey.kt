package com.example.newsapp.Room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * O3: explicit paging key for a feed partition.
 *
 * Previously [com.example.newsapp.data.repository.ArticleRemoteMediator] derived the next page
 * from the last cached item's `sortOrder` (`(sortOrder / pageSize) + 2`). That heuristic breaks
 * whenever the cached rows aren't a contiguous `0..n` run — e.g. after `toDomainOrNull()` drops a
 * malformed article, or after a dedup/REPLACE collapses two rows — because `sortOrder` then no
 * longer maps cleanly back to a page index, causing pages to be skipped or re-fetched.
 *
 * This table stores the page to fetch next as authoritative state, keyed by `feedKey`. One row per
 * feed (category / for_you / firehose / search). `nextPage == null` means the end of pagination has
 * been reached for that feed.
 */
@Entity(tableName = "feed_remote_keys")
data class FeedRemoteKey(
    @PrimaryKey val feedKey: String,
    val nextPage: Int?
)
