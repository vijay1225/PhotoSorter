package com.example.photosort.data.model

import android.net.Uri

/**
 * Represents an individual photo scanned from device MediaStore and stored locally.
 */
data class ImageItem(
    val id: Long,
    val contentUriString: String,
    val filePath: String,
    val displayName: String,
    val dateAdded: Long,
    val sizeBytes: Long,
    val bucketName: String,
    val status: SortStatus = SortStatus.UNCATEGORIZED,
    val updatedAt: Long = System.currentTimeMillis(),
    val isVideo: Boolean = false,
    val durationMs: Long = 0L
) {
    val contentUri: Uri
        get() = Uri.parse(contentUriString)

    /**
     * Formatted duration string for videos (e.g. "01:25", "00:08").
     */
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    /**
     * Human-friendly formatted size string (e.g. "3.4 MB", "850 KB").
     */
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    /**
     * Human-friendly formatted date string.
     */
    val formattedDate: String
        get() {
            if (dateAdded <= 0) return "Unknown date"
            val millis = if (dateAdded < 10_000_000_000L) dateAdded * 1000L else dateAdded
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(millis))
        }

    /**
     * Month and Year string for timeline clustering (e.g. "October 2024").
     */
    val monthYear: String
        get() {
            if (dateAdded <= 0) return "Older Media"
            val millis = if (dateAdded < 10_000_000_000L) dateAdded * 1000L else dateAdded
            val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(millis))
        }

    /**
     * Exact date for date-wise range selection (e.g. "Oct 28, 2024").
     */
    val dateOnly: String
        get() {
            if (dateAdded <= 0) return "Unknown Date"
            val millis = if (dateAdded < 10_000_000_000L) dateAdded * 1000L else dateAdded
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(millis))
        }
}
