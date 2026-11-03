package com.example.photosort.ui.main

import com.example.photosort.data.model.ImageItem
import com.example.photosort.data.model.SortOrder
import com.example.photosort.data.model.SortStats
import com.example.photosort.data.model.SortStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelUnitTest {
    @Test
    fun sortStats_progressCalculation_isAccurate() {
        val stats = SortStats(
            totalCount = 100,
            keepCount = 25,
            deleteCount = 15,
            laterCount = 10,
            uncategorizedCount = 50,
            deleteSizeBytes = 1024 * 1024 * 150 // 150 MB
        )

        assertEquals(50, stats.sortedCount)
        assertEquals(0.50f, stats.progressFraction, 0.001f)
        assertEquals(50, stats.progressPercent)
        assertEquals("150.0 MB", stats.formattedReclaimableSpace)
    }

    @Test
    fun sortStatus_fromString_parsesSafely() {
        assertEquals(SortStatus.KEEP, SortStatus.fromString("KEEP"))
        assertEquals(SortStatus.DELETE, SortStatus.fromString("delete"))
        assertEquals(SortStatus.LATER, SortStatus.fromString("LATER"))
        assertEquals(SortStatus.UNCATEGORIZED, SortStatus.fromString("unknown_val"))
    }

    @Test
    fun sortOrder_values_areCorrect() {
        assertEquals("Newest First", SortOrder.NEWEST_FIRST.displayName)
        assertEquals("Oldest First", SortOrder.OLDEST_FIRST.displayName)
    }

    @Test
    fun imageItem_dateGroupings_formatCorrectly() {
        // Fixed timestamp: 1700000000L in seconds (Nov 14, 2023)
        val item = ImageItem(
            id = 1L,
            contentUriString = "content://media/external/images/media/1",
            filePath = "/sdcard/DCIM/IMG_1.jpg",
            displayName = "IMG_1.jpg",
            dateAdded = 1700000000L,
            sizeBytes = 2048576L,
            bucketName = "DCIM"
        )

        assertTrue(item.monthYear.contains("2023"))
        assertTrue(item.formattedDate.contains("2023"))
        assertEquals("2.0 MB", item.formattedSize)
    }
}
