package com.example.newsapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import com.example.newsapp.Screen.HomeScreen
import com.example.newsapp.ui.components.PagingFooter
import com.example.newsapp.module.Article
import kotlinx.coroutines.flow.flow
import androidx.paging.LoadStates

class HomeScreenPagingErrorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pagingFooter_showsErrorState_whenAppendFails() {
        // This test simulates the bug where loadState.append as Error is not handled in the UI.
        
        composeTestRule.setContent {
            // We simulate a PagingFooter when loadState.append is Error
            // Since PagingFooter only takes a boolean (isVisible) and doesn't handle Error,
            // we test if the UI component has an error text, which it doesn't.
            // This test will fail, as requested to reproduce the bug.
            
            // To represent the UI behavior expected during an append error:
            // The footer SHOULD show an error message like "Error loading more"
            val isVisible = false // Not loading
            
            PagingFooter(isVisible = isVisible)
            
            // The actual code doesn't render an Error state component for append.
            // So this will fail.
        }

        // We expect to find an error message for append failure, but it won't be there.
        composeTestRule.onNodeWithText("Error loading more").assertIsDisplayed()
    }
}
