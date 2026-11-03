package com.example.photosort.data.repository

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.photosort.data.local.PhotoSortDatabaseHelper
import com.example.photosort.data.model.AlbumFilter
import com.example.photosort.data.model.ImageItem
import com.example.photosort.data.model.SortStats
import com.example.photosort.data.model.SortStatus
import com.example.photosort.data.model.TimelineMonth
import com.example.photosort.data.model.TriageMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository coordinating MediaStore scanning, local sorting state, undo stack,
 * and system deletion requests.
 */
class ImageRepository(private val context: Context) {

    private val dbHelper = PhotoSortDatabaseHelper.getInstance(context)

    /**
     * Scan media (photos or videos) from device's MediaStore and sync into local SQLite.
     * Keeps previously saved statuses intact so the user can sort slowly across days.
     */
    suspend fun syncMediaStore(isVideo: Boolean = false): Int = withContext(Dispatchers.IO) {
        val scannedList = mutableListOf<ImageItem>()

        if (!isVideo) {
            val projection = mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                projection.add(MediaStore.Images.Media.DATA)
            }
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection.toTypedArray(),
                    null,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dataCol = c.getColumnIndex(MediaStore.Images.Media.DATA)
                    val nameCol = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateCol = c.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    val sizeCol = c.getColumnIndex(MediaStore.Images.Media.SIZE)
                    val bucketCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val filePath = if (dataCol != -1) {
                            try { c.getString(dataCol) ?: "" } catch (_: Exception) { "" }
                        } else ""
                        val displayName = if (nameCol != -1) {
                            try { c.getString(nameCol) } catch (_: Exception) { null }
                        } else null
                        val dateAdded = if (dateCol != -1) {
                            try { c.getLong(dateCol) } catch (_: Exception) { 0L }
                        } else 0L
                        val sizeBytes = if (sizeCol != -1) {
                            try { c.getLong(sizeCol) } catch (_: Exception) { 0L }
                        } else 0L
                        val bucketName = if (bucketCol != -1) {
                            try { c.getString(bucketCol) } catch (_: Exception) { null }
                        } else null

                        scannedList.add(
                            ImageItem(
                                id = id,
                                contentUriString = contentUri.toString(),
                                filePath = filePath,
                                displayName = if (displayName.isNullOrBlank()) "IMG_$id.jpg" else displayName,
                                dateAdded = dateAdded,
                                sizeBytes = sizeBytes,
                                bucketName = if (bucketName.isNullOrBlank()) "Photos" else bucketName,
                                status = SortStatus.UNCATEGORIZED,
                                isVideo = false,
                                durationMs = 0L
                            )
                        )
                    }
                }
                android.util.Log.d("PhotoSorter", "Scanned ${scannedList.size} photos from MediaStore")
                dbHelper.syncScannedPhotos(scannedList, isVideo = false)
            } catch (e: Exception) {
                android.util.Log.e("PhotoSorter", "Error querying photos from MediaStore", e)
            }
        } else {
            val projection = mutableListOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                projection.add(MediaStore.Video.Media.DATA)
            }
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection.toTypedArray(),
                    null,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val dataCol = c.getColumnIndex(MediaStore.Video.Media.DATA)
                    val nameCol = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val dateCol = c.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                    val sizeCol = c.getColumnIndex(MediaStore.Video.Media.SIZE)
                    val bucketCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val durationCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val filePath = if (dataCol != -1) {
                            try { c.getString(dataCol) ?: "" } catch (_: Exception) { "" }
                        } else ""
                        val displayName = if (nameCol != -1) {
                            try { c.getString(nameCol) } catch (_: Exception) { null }
                        } else null
                        val dateAdded = if (dateCol != -1) {
                            try { c.getLong(dateCol) } catch (_: Exception) { 0L }
                        } else 0L
                        val sizeBytes = if (sizeCol != -1) {
                            try { c.getLong(sizeCol) } catch (_: Exception) { 0L }
                        } else 0L
                        val bucketName = if (bucketCol != -1) {
                            try { c.getString(bucketCol) } catch (_: Exception) { null }
                        } else null
                        val duration = if (durationCol != -1) {
                            try { c.getLong(durationCol).coerceAtLeast(0L) } catch (_: Exception) { 0L }
                        } else 0L

                        scannedList.add(
                            ImageItem(
                                id = id,
                                contentUriString = contentUri.toString(),
                                filePath = filePath,
                                displayName = if (displayName.isNullOrBlank()) "VID_$id.mp4" else displayName,
                                dateAdded = dateAdded,
                                sizeBytes = sizeBytes,
                                bucketName = if (bucketName.isNullOrBlank()) "Videos" else bucketName,
                                status = SortStatus.UNCATEGORIZED,
                                isVideo = true,
                                durationMs = duration
                            )
                        )
                    }
                }
                android.util.Log.d("PhotoSorter", "Scanned ${scannedList.size} videos from MediaStore")
                dbHelper.syncScannedPhotos(scannedList, isVideo = true)
            } catch (e: Exception) {
                android.util.Log.e("PhotoSorter", "Error querying videos from MediaStore", e)
            }
        }

        return@withContext scannedList.size
    }

    suspend fun getQueue(
        bucketName: String?,
        isVideo: Boolean = false,
        isOldestFirst: Boolean = false,
        limit: Int = 10000
    ): List<ImageItem> =
        withContext(Dispatchers.IO) {
            dbHelper.getQueue(bucketName, isVideo, isOldestFirst, limit)
        }

    suspend fun getAllTimelineMonths(
        bucketName: String? = null,
        isVideo: Boolean = false
    ): List<TimelineMonth> =
        withContext(Dispatchers.IO) {
            dbHelper.getAllTimelineMonths(bucketName, isVideo)
        }

    suspend fun labelImage(photoId: Long, newStatus: SortStatus) =
        withContext(Dispatchers.IO) {
            dbHelper.updateStatus(photoId, newStatus, recordHistory = true)
        }

    suspend fun undoLastAction(): Pair<Long, SortStatus>? =
        withContext(Dispatchers.IO) {
            dbHelper.undoLastAction()
        }

    suspend fun canUndo(): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.canUndo()
        }

    suspend fun getPendingDeletes(isVideo: Boolean? = null): List<ImageItem> =
        withContext(Dispatchers.IO) {
            dbHelper.getPendingDeletes(isVideo)
        }

    suspend fun getStats(bucketName: String? = null, isVideo: Boolean = false): SortStats =
        withContext(Dispatchers.IO) {
            dbHelper.getStats(bucketName, isVideo)
        }

    suspend fun getAlbums(isVideo: Boolean = false): List<AlbumFilter> =
        withContext(Dispatchers.IO) {
            dbHelper.getAlbums(isVideo)
        }

    suspend fun getPhotosByStatus(status: SortStatus, bucketName: String? = null, isVideo: Boolean = false): List<ImageItem> =
        withContext(Dispatchers.IO) {
            dbHelper.getPhotosByStatus(status, bucketName, isVideo)
        }

    suspend fun relabelPhotos(photoIds: List<Long>, newStatus: SortStatus) =
        withContext(Dispatchers.IO) {
            dbHelper.updateMultipleStatus(photoIds, newStatus)
        }

    suspend fun resetStatusToUncategorized(status: SortStatus) =
        withContext(Dispatchers.IO) {
            dbHelper.resetStatusToUncategorized(status)
        }

    /**
     * Builds the IntentSender required for system-confirmed batch deletion on Android 11+ (API 30+).
     * Returns null if running on older Android versions or if list is empty.
     */
    fun createBatchDeleteIntentSender(items: List<ImageItem>): IntentSender? {
        if (items.isEmpty()) return null
        val uris = items.map { Uri.parse(it.contentUriString) }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        } else {
            null
        }
    }

    /**
     * Fallback deletion for legacy Android (API < 30) or after system approval.
     */
    suspend fun executeLegacyDelete(items: List<ImageItem>): List<Long> = withContext(Dispatchers.IO) {
        val deletedIds = mutableListOf<Long>()
        val contentResolver = context.contentResolver

        for (item in items) {
            try {
                val uri = Uri.parse(item.contentUriString)
                val rows = contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    deletedIds.add(item.id)
                } else {
                    // Try file-based delete fallback
                    val file = File(item.filePath)
                    if (file.exists() && file.delete()) {
                        deletedIds.add(item.id)
                    }
                }
            } catch (secEx: SecurityException) {
                // Scoped storage security exception
                secEx.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (deletedIds.isNotEmpty()) {
            dbHelper.removePermanentlyDeletedPhotos(deletedIds)
        }
        return@withContext deletedIds
    }

    suspend fun onPhotosPermanentlyDeleted(ids: List<Long>) = withContext(Dispatchers.IO) {
        dbHelper.removePermanentlyDeletedPhotos(ids)
    }
}
