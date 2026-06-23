package com.example.newsapp.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GeoLanguageRepositoryImplTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(): GeoLanguageRepositoryImpl {
        val store = PreferenceDataStoreFactory.create {
            File(tmp.newFolder(), "geo_language.preferences_pb")
        }
        return GeoLanguageRepositoryImpl(store)
    }

    @Test fun `applyDetected sets region and languages when no override`() = runTest {
        val r = repo()
        r.applyDetected(region = "DE", languages = listOf("en", "de"))
        val s = r.state.first()
        assertEquals("DE", s.homeRegion)
        assertEquals(listOf("en", "de"), s.readableLanguages)
    }

    @Test fun `applyDetected does NOT clobber a manual region`() = runTest {
        val r = repo()
        r.setManualRegion("FR")
        r.applyDetected(region = "DE", languages = listOf("en"))
        assertEquals("FR", r.state.first().homeRegion)
    }

    @Test fun `applyDetected does NOT clobber manual languages`() = runTest {
        val r = repo()
        r.setManualLanguages(listOf("fr"))
        r.applyDetected(region = "DE", languages = listOf("en", "de"))
        assertEquals(listOf("fr"), r.state.first().readableLanguages)
    }

    @Test fun `resetToAuto clears override flags so detection applies again`() = runTest {
        val r = repo()
        r.setManualRegion("FR")
        r.resetToAuto()
        r.applyDetected(region = "DE", languages = listOf("en"))
        assertEquals("DE", r.state.first().homeRegion)
    }
}
