package com.example.newsapp.domain.repository

import com.example.newsapp.domain.reader.LineHeightOption
import com.example.newsapp.domain.reader.ReaderTheme
import com.example.newsapp.domain.reader.ReadingPreferences
import com.example.newsapp.domain.reader.WidthOption
import kotlinx.coroutines.flow.Flow

interface ReadingPreferencesRepository {
    val preferences: Flow<ReadingPreferences>
    suspend fun setFontScale(scale: Float)
    suspend fun setLineHeight(option: LineHeightOption)
    suspend fun setMeasureWidth(option: WidthOption)
    suspend fun setTheme(theme: ReaderTheme)
    suspend fun setBionicEnabled(enabled: Boolean)
    suspend fun setFocusEnabled(enabled: Boolean)
}
