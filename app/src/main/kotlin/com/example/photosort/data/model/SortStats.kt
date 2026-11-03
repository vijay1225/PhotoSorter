package com.example.photosort.data.model

/**
 * Summary statistics for the 4 categories: Unlabeled (Uncategorized), Keep, Delete, Later.
 */
data class SortStats(
    val totalCount: Int = 0,
    val keepCount: Int = 0,
    val deleteCount: Int = 0,
    val laterCount: Int = 0,
    val uncategorizedCount: Int = 0,
    val deleteSizeBytes: Long = 0L
) {
    val sortedCount: Int
        get() = keepCount + deleteCount + laterCount

    val progressFraction: Float
        get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt().coerceIn(0, 100)

    val formattedReclaimableSpace: String
        get() {
            if (deleteSizeBytes <= 0) return "0 MB"
            val mb = deleteSizeBytes / (1024.0 * 1024.0)
            val gb = mb / 1024.0
            return if (gb >= 1.0) {
                String.format("%.2f GB", gb)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
