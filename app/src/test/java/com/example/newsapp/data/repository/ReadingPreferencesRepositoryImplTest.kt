package com.example.newsapp.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.newsapp.domain.reader.ReaderTheme
import com.example.newsapp.domain.reader.ReadingPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ReadingPreferencesRepositoryImplTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(): ReadingPreferencesRepositoryImpl {
        val store = PreferenceDataStoreFactory.create { File(tmp.newFolder(), "reading.preferences_pb") }
        return ReadingPreferencesRepositoryImpl(store)
    }

    @Test fun defaults_then_persisted_writes() = runTest {
        val r = repo()
        assertEquals(ReadingPreferences(), r.preferences.first())
        r.setFontScale(2.0f)               // above max -> clamps to 1.6
        r.setTheme(ReaderTheme.SEPIA)
        r.setBionicEnabled(true)
        val p = r.preferences.first()
        assertEquals(1.6f, p.fontScale, 0.001f)
        assertEquals(ReaderTheme.SEPIA, p.theme)
        assertEquals(true, p.bionicEnabled)
    }
}
