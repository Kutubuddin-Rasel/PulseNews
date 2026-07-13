package com.example.newsapp.ViewModel

import com.example.newsapp.domain.util.reader.ReaderMode
import com.example.newsapp.domain.util.reader.initialModeFor
import com.example.newsapp.domain.util.reader.toggled
import org.junit.Assert.assertEquals
import org.junit.Test

class WebScreenReaderModeTest {
    @Test fun thin_content_opens_in_web() {
        assertEquals(ReaderMode.Web, initialModeFor(isThin = true))
    }

    @Test fun rich_content_opens_in_reader() {
        assertEquals(ReaderMode.Reader, initialModeFor(isThin = false))
    }

    @Test fun toggle_flips_the_mode() {
        assertEquals(ReaderMode.Web, ReaderMode.Reader.toggled())
        assertEquals(ReaderMode.Reader, ReaderMode.Web.toggled())
    }
}
