package com.example.photosort.data.model

/**
 * Triage queue sort order:
 *  - NEWEST_FIRST: Sort from recent incoming photos towards older ones (Descending)
 *  - OLDEST_FIRST: Sort from oldest photos towards newer ones (Ascending)
 */
enum class SortOrder(val displayName: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First")
}
