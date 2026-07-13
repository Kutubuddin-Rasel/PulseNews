package com.example.newsapp.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InteractionEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: InteractionEventEntity)

    @Query("SELECT * FROM interaction_events ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int): List<InteractionEventEntity>

    @Query("DELETE FROM interaction_events WHERE id IN (:ids)")
    suspend fun deleteEvents(ids: List<Long>)

    /** Bounds local storage (audit A7): keep the newest [maxRows] events, drop older unsent ones. */
    @Query(
        "DELETE FROM interaction_events WHERE id NOT IN " +
            "(SELECT id FROM interaction_events ORDER BY timestamp DESC LIMIT :maxRows)"
    )
    suspend fun trimToMostRecent(maxRows: Int)
}
