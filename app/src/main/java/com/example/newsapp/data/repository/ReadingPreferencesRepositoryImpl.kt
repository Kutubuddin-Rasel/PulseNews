package com.example.newsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.newsapp.Hilt.ReadingDataStore
import com.example.newsapp.domain.reader.LineHeightOption
import com.example.newsapp.domain.reader.ReaderTheme
import com.example.newsapp.domain.reader.ReadingPreferences
import com.example.newsapp.domain.reader.WidthOption
import com.example.newsapp.domain.repository.ReadingPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingPreferencesRepositoryImpl @Inject constructor(
    @ReadingDataStore private val dataStore: DataStore<Preferences>,
) : ReadingPreferencesRepository {
    private companion object {
        val FONT = floatPreferencesKey("reader_font_scale")
        val LINE = stringPreferencesKey("reader_line_height")
        val WIDTH = stringPreferencesKey("reader_measure_width")
        val THEME = stringPreferencesKey("reader_theme")
        val BIONIC = booleanPreferencesKey("reader_bionic")
        val FOCUS = booleanPreferencesKey("reader_focus")
    }

    override val preferences: Flow<ReadingPreferences> = dataStore.data.map { p ->
        ReadingPreferences(
            fontScale = p[FONT] ?: 1.0f,
            lineHeight = p[LINE]?.let { runCatching { LineHeightOption.valueOf(it) }.getOrNull() } ?: LineHeightOption.NORMAL,
            measureWidth = p[WIDTH]?.let { runCatching { WidthOption.valueOf(it) }.getOrNull() } ?: WidthOption.MEDIUM,
            theme = p[THEME]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: ReaderTheme.LIGHT,
            bionicEnabled = p[BIONIC] ?: false,
            focusEnabled = p[FOCUS] ?: false,
        )
    }

    override suspend fun setFontScale(scale: Float) = write {
        it[FONT] = scale.coerceIn(ReadingPreferences.MIN_FONT_SCALE, ReadingPreferences.MAX_FONT_SCALE)
    }
    override suspend fun setLineHeight(option: LineHeightOption) = write { it[LINE] = option.name }
    override suspend fun setMeasureWidth(option: WidthOption) = write { it[WIDTH] = option.name }
    override suspend fun setTheme(theme: ReaderTheme) = write { it[THEME] = theme.name }
    override suspend fun setBionicEnabled(enabled: Boolean) = write { it[BIONIC] = enabled }
    override suspend fun setFocusEnabled(enabled: Boolean) = write { it[FOCUS] = enabled }

    private suspend fun write(block: (MutablePreferences) -> Unit) {
        withContext(Dispatchers.IO) { dataStore.edit(block) }
    }
}
