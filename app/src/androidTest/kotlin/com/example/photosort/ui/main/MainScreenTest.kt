package com.example.photosort.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.photosort.data.model.AlbumFilter
import com.example.photosort.data.model.SortStats
import com.example.photosort.ui.components.ProgressStatsHeader
import com.example.photosort.ui.theme.PhotoSortTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun progressStatsHeader_displaysProgressCorrectly() {
        val testStats = SortStats(
            totalCount = 200,
            keepCount = 50,
            deleteCount = 50,
            uncategorizedCount = 100
        )

        composeTestRule.setContent {
            PhotoSortTheme {
                ProgressStatsHeader(
                    stats = testStats,
                    albums = listOf(AlbumFilter("Camera", 150), AlbumFilter("Screenshots", 50)),
                    selectedAlbum = null,
                    onSelectAlbum = {}
                )
            }
        }

        composeTestRule.onNodeWithText("All Images (200)").assertExists()
    }
}
