package com.example.photosort.data.model

/**
 * 4-state categorization: Unlabeled (Uncategorized), Keep, Delete, Later.
 */
enum class SortStatus(val displayName: String) {
    /** Images not sorted yet. */
    UNCATEGORIZED("Unlabeled"),

    /** Images to keep (Swipe UP). */
    KEEP("Keep"),

    /** Images to delete (Swipe DOWN). */
    DELETE("Delete"),

    /** Images to categorize later (Swipe LEFT). */
    LATER("Later");

    companion object {
        fun fromString(value: String?): SortStatus {
            return when (value?.uppercase()) {
                "KEEP" -> KEEP
                "DELETE" -> DELETE
                "LATER" -> LATER
                else -> UNCATEGORIZED
            }
        }
    }
}
