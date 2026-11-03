package com.example.photosort.data.model

/**
 * Represents a month group across the user's entire photo/video library.
 * Used for timeline clustering, all-time browsing, and jump-to-date navigation.
 */
data class TimelineMonth(
    val year: Int,
    val month: Int,
    val monthYearLabel: String,
    val count: Int,
    val minTimestampSeconds: Long
)
