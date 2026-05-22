package com.example.newsapp.Room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey
    val url: String,
    val prevKey: Int?,
    val nextKey: Int?
)
