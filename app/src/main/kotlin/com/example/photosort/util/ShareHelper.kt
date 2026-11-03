package com.example.photosort.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.photosort.data.model.ImageItem

/**
 * Utility helper to dispatch Android system share sheets for single or multiple
 * photos and videos while maintaining proper URI read permissions.
 */
object ShareHelper {

    /**
     * Share a single photo or video using Android's native share sheet.
     */
    fun shareSingleMedia(context: Context, item: ImageItem) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (item.isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, item.contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val title = if (item.isVideo) "Share Video" else "Share Photo"
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share multiple photos and/or videos in a single batch share sheet.
     */
    fun shareMultipleMedia(context: Context, items: List<ImageItem>) {
        if (items.isEmpty()) return
        if (items.size == 1) {
            shareSingleMedia(context, items.first())
            return
        }
        try {
            val uris = ArrayList(items.map { it.contentUri })
            val hasVideo = items.any { it.isVideo }
            val hasImage = items.any { !it.isVideo }
            val mimeType = when {
                hasVideo && !hasImage -> "video/*"
                !hasVideo && hasImage -> "image/*"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${items.size} items"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }
}
