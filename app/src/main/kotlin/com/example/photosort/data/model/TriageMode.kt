package com.example.photosort.data.model

/**
 * Filter mode determining which images appear in the sorting card queue.
 */
enum class TriageMode(val title: String) {
    /** Primary flow: triage all images that haven't been reviewed yet. */
    UNSORTED_QUEUE("Unsorted"),

    /** Revisit items that were postponed previously for later decision. */
    REVIEW_LATER("Review Later"),

    /** Inspect or change mind on images previously marked to Keep. */
    REVIEW_KEPT("Kept Photos")
}

/**
 * Represents an album/folder detected on the device.
 */
data class AlbumFilter(
    val bucketName: String,
    val count: Int
)
