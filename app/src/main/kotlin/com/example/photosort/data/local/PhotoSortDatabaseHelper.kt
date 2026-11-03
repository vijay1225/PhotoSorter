package com.example.photosort.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.photosort.data.model.AlbumFilter
import com.example.photosort.data.model.ImageItem
import com.example.photosort.data.model.SortStats
import com.example.photosort.data.model.SortStatus
import com.example.photosort.data.model.TimelineMonth
import com.example.photosort.data.model.TriageMode

/**
 * Local SQLite storage helper to persist triage choices (Keep, Delete, Later, Unsorted)
 * and track undo history without relying on external cloud services.
 */
class PhotoSortDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "photosort.db"
        const val DATABASE_VERSION = 3

        const val TABLE_PHOTOS = "photos"
        const val COL_ID = "id"
        const val COL_URI = "uri"
        const val COL_FILE_PATH = "file_path"
        const val COL_DISPLAY_NAME = "display_name"
        const val COL_DATE_ADDED = "date_added"
        const val COL_SIZE_BYTES = "size_bytes"
        const val COL_BUCKET_NAME = "bucket_name"
        const val COL_STATUS = "status"
        const val COL_UPDATED_AT = "updated_at"
        const val COL_IS_VIDEO = "is_video"
        const val COL_DURATION_MS = "duration_ms"

        const val TABLE_HISTORY = "triage_history"
        const val COL_HIST_ID = "history_id"
        const val COL_HIST_PHOTO_ID = "photo_id"
        const val COL_HIST_PREV_STATUS = "previous_status"
        const val COL_HIST_NEW_STATUS = "new_status"
        const val COL_HIST_TIMESTAMP = "timestamp"

        @Volatile
        private var instance: PhotoSortDatabaseHelper? = null

        fun getInstance(context: Context): PhotoSortDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: PhotoSortDatabaseHelper(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PHOTOS (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_URI TEXT NOT NULL,
                $COL_FILE_PATH TEXT NOT NULL,
                $COL_DISPLAY_NAME TEXT NOT NULL,
                $COL_DATE_ADDED INTEGER NOT NULL,
                $COL_SIZE_BYTES INTEGER NOT NULL,
                $COL_BUCKET_NAME TEXT NOT NULL,
                $COL_STATUS TEXT NOT NULL DEFAULT 'UNCATEGORIZED',
                $COL_UPDATED_AT INTEGER NOT NULL,
                $COL_IS_VIDEO INTEGER NOT NULL DEFAULT 0,
                $COL_DURATION_MS INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX idx_photos_status ON $TABLE_PHOTOS ($COL_STATUS)")
        db.execSQL("CREATE INDEX idx_photos_bucket ON $TABLE_PHOTOS ($COL_BUCKET_NAME)")
        db.execSQL("CREATE INDEX idx_photos_date ON $TABLE_PHOTOS ($COL_DATE_ADDED DESC)")
        db.execSQL("CREATE INDEX idx_photos_video ON $TABLE_PHOTOS ($COL_IS_VIDEO)")

        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COL_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HIST_PHOTO_ID INTEGER NOT NULL,
                $COL_HIST_PREV_STATUS TEXT NOT NULL,
                $COL_HIST_NEW_STATUS TEXT NOT NULL,
                $COL_HIST_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ensureColumnsExist(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        ensureColumnsExist(db)
    }

    private fun ensureColumnsExist(db: SQLiteDatabase) {
        try {
            val cursor = db.rawQuery("PRAGMA table_info($TABLE_PHOTOS)", null)
            val columns = mutableSetOf<String>()
            cursor.use { c ->
                val nameIndex = c.getColumnIndex("name")
                while (c.moveToNext()) {
                    if (nameIndex != -1) {
                        columns.add(c.getString(nameIndex))
                    }
                }
            }
            if (!columns.contains(COL_IS_VIDEO)) {
                db.execSQL("ALTER TABLE $TABLE_PHOTOS ADD COLUMN $COL_IS_VIDEO INTEGER NOT NULL DEFAULT 0")
            }
            if (!columns.contains(COL_DURATION_MS)) {
                db.execSQL("ALTER TABLE $TABLE_PHOTOS ADD COLUMN $COL_DURATION_MS INTEGER NOT NULL DEFAULT 0")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_photos_video ON $TABLE_PHOTOS ($COL_IS_VIDEO)")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Synchronize newly scanned device photos into local DB.
     * Preserves existing sorting statuses for known photos.
     */
    @Synchronized
    fun syncScannedPhotos(scannedPhotos: List<ImageItem>, isVideo: Boolean = false) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Collect existing IDs for this media type
            val existingIds = mutableSetOf<Long>()
            db.rawQuery("SELECT $COL_ID FROM $TABLE_PHOTOS WHERE $COL_IS_VIDEO = ?", arrayOf(if (isVideo) "1" else "0")).use { cursor ->
                while (cursor.moveToNext()) {
                    existingIds.add(cursor.getLong(0))
                }
            }

            val scannedIds = HashSet<Long>(scannedPhotos.size)
            val now = System.currentTimeMillis()

            for (photo in scannedPhotos) {
                scannedIds.add(photo.id)
                if (!existingIds.contains(photo.id)) {
                    val values = ContentValues().apply {
                        put(COL_ID, photo.id)
                        put(COL_URI, photo.contentUriString)
                        put(COL_FILE_PATH, photo.filePath)
                        put(COL_DISPLAY_NAME, photo.displayName)
                        put(COL_DATE_ADDED, photo.dateAdded)
                        put(COL_SIZE_BYTES, photo.sizeBytes)
                        put(COL_BUCKET_NAME, photo.bucketName)
                        put(COL_STATUS, SortStatus.UNCATEGORIZED.name)
                        put(COL_UPDATED_AT, now)
                        put(COL_IS_VIDEO, if (photo.isVideo) 1 else 0)
                        put(COL_DURATION_MS, photo.durationMs)
                    }
                    db.insertWithOnConflict(TABLE_PHOTOS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                } else {
                    // Update is_video, duration, and bucket in case it was stored with older defaults
                    val updateValues = ContentValues().apply {
                        put(COL_IS_VIDEO, if (photo.isVideo) 1 else 0)
                        put(COL_DURATION_MS, photo.durationMs)
                        put(COL_BUCKET_NAME, photo.bucketName)
                    }
                    db.update(TABLE_PHOTOS, updateValues, "$COL_ID = ?", arrayOf(photo.id.toString()))
                }
            }

            // Remove items from DB that were deleted externally from device, ONLY if scan was successful
            if (scannedPhotos.isNotEmpty()) {
                val deletedIds = existingIds - scannedIds
                if (deletedIds.isNotEmpty()) {
                    val placeholders = deletedIds.joinToString(",") { "?" }
                    val args = (deletedIds.map { it.toString() } + (if (isVideo) "1" else "0")).toTypedArray()
                    db.delete(TABLE_PHOTOS, "$COL_ID IN ($placeholders) AND $COL_IS_VIDEO = ?", args)
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Updates an image's triage label and logs it in the undo history stack.
     */
    @Synchronized
    fun updateStatus(photoId: Long, newStatus: SortStatus, recordHistory: Boolean = true) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var prevStatus = SortStatus.UNCATEGORIZED.name
            db.rawQuery("SELECT $COL_STATUS FROM $TABLE_PHOTOS WHERE $COL_ID = ?", arrayOf(photoId.toString())).use { cursor ->
                if (cursor.moveToNext()) {
                    prevStatus = cursor.getString(0)
                }
            }

            val now = System.currentTimeMillis()
            val values = ContentValues().apply {
                put(COL_STATUS, newStatus.name)
                put(COL_UPDATED_AT, now)
            }
            db.update(TABLE_PHOTOS, values, "$COL_ID = ?", arrayOf(photoId.toString()))

            if (recordHistory) {
                val histValues = ContentValues().apply {
                    put(COL_HIST_PHOTO_ID, photoId)
                    put(COL_HIST_PREV_STATUS, prevStatus)
                    put(COL_HIST_NEW_STATUS, newStatus.name)
                    put(COL_HIST_TIMESTAMP, now)
                }
                db.insert(TABLE_HISTORY, null, histValues)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Reverts the most recent triage action.
     * Returns Pair(photoId, restoredStatus) or null if history is empty.
     */
    @Synchronized
    fun undoLastAction(): Pair<Long, SortStatus>? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var lastHistoryId: Long = -1
            var photoId: Long = -1
            var prevStatusStr: String? = null

            db.rawQuery(
                "SELECT $COL_HIST_ID, $COL_HIST_PHOTO_ID, $COL_HIST_PREV_STATUS FROM $TABLE_HISTORY ORDER BY $COL_HIST_ID DESC LIMIT 1",
                null
            ).use { cursor ->
                if (cursor.moveToNext()) {
                    lastHistoryId = cursor.getLong(0)
                    photoId = cursor.getLong(1)
                    prevStatusStr = cursor.getString(2)
                }
            }

            if (lastHistoryId != -1L && prevStatusStr != null) {
                val restoredStatus = SortStatus.fromString(prevStatusStr)
                val values = ContentValues().apply {
                    put(COL_STATUS, restoredStatus.name)
                    put(COL_UPDATED_AT, System.currentTimeMillis())
                }
                db.update(TABLE_PHOTOS, values, "$COL_ID = ?", arrayOf(photoId.toString()))
                db.delete(TABLE_HISTORY, "$COL_HIST_ID = ?", arrayOf(lastHistoryId.toString()))

                db.setTransactionSuccessful()
                return Pair(photoId, restoredStatus)
            }
            return null
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun canUndo(): Boolean {
        readableDatabase.rawQuery("SELECT 1 FROM $TABLE_HISTORY LIMIT 1", null).use {
            return it.moveToNext()
        }
    }

    /**
     * Fetches queue of images for sorting:
     * Unlabeled/Uncategorized images first, followed by images labeled LATER.
     * Orders by date_added DESC (Newest First) or ASC (Oldest First).
     */
    @Synchronized
    fun getQueue(
        bucketName: String? = null,
        isVideo: Boolean = false,
        isOldestFirst: Boolean = false,
        limit: Int = 10000
    ): List<ImageItem> {
        val db = readableDatabase
        val whereClause = StringBuilder("($COL_STATUS = 'UNCATEGORIZED' OR $COL_STATUS = 'UNSORTED' OR $COL_STATUS = 'LATER')")
        whereClause.append(" AND $COL_IS_VIDEO = ${if (isVideo) 1 else 0}")
        val args = mutableListOf<String>()

        if (!bucketName.isNullOrBlank() && bucketName != "All Images" && bucketName != "All Folders" && bucketName != "All Videos") {
            whereClause.append(" AND $COL_BUCKET_NAME = ?")
            args.add(bucketName)
        }

        val dateDirection = if (isOldestFirst) "ASC" else "DESC"
        val query = """
            SELECT $COL_ID, $COL_URI, $COL_FILE_PATH, $COL_DISPLAY_NAME, 
               $COL_DATE_ADDED, $COL_SIZE_BYTES, $COL_BUCKET_NAME, $COL_STATUS, $COL_UPDATED_AT,
               $COL_IS_VIDEO, $COL_DURATION_MS
            FROM $TABLE_PHOTOS
            WHERE $whereClause
            ORDER BY 
                CASE WHEN $COL_STATUS = 'UNCATEGORIZED' OR $COL_STATUS = 'UNSORTED' THEN 0 ELSE 1 END ASC,
                $COL_DATE_ADDED $dateDirection
            LIMIT $limit
        """.trimIndent()

        val results = mutableListOf<ImageItem>()
        db.rawQuery(query, if (args.isEmpty()) null else args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                results.add(cursorToImageItem(cursor))
            }
        }
        return results
    }

    /**
     * Queries all distinct years and months across the entire library/folder,
     * along with item counts and timestamps, for a comprehensive all-time timeline picker.
     */
    @Synchronized
    fun getAllTimelineMonths(bucketName: String? = null, isVideo: Boolean = false): List<TimelineMonth> {
        val db = readableDatabase
        val whereClause = StringBuilder("($COL_STATUS = 'UNCATEGORIZED' OR $COL_STATUS = 'UNSORTED' OR $COL_STATUS = 'LATER')")
        whereClause.append(" AND $COL_IS_VIDEO = ${if (isVideo) 1 else 0}")
        val args = mutableListOf<String>()

        if (!bucketName.isNullOrBlank() && bucketName != "All Images" && bucketName != "All Folders" && bucketName != "All Videos") {
            whereClause.append(" AND $COL_BUCKET_NAME = ?")
            args.add(bucketName)
        }

        val query = """
            SELECT $COL_DATE_ADDED
            FROM $TABLE_PHOTOS
            WHERE $whereClause
            ORDER BY $COL_DATE_ADDED DESC
        """.trimIndent()

        val sdfYear = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())
        val sdfMonth = java.text.SimpleDateFormat("MM", java.util.Locale.getDefault())
        val sdfMonthYear = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())

        val map = linkedMapOf<String, Triple<Int, Long, Int>>() // key: "2024-10" -> Triple(year, minTimestamp, count)

        db.rawQuery(query, if (args.isEmpty()) null else args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                val dateAdded = cursor.getLong(0)
                if (dateAdded <= 0) continue
                val millis = if (dateAdded < 10_000_000_000L) dateAdded * 1000L else dateAdded
                val date = java.util.Date(millis)
                val yrStr = sdfYear.format(date)
                val moStr = sdfMonth.format(date)
                val key = "$yrStr-$moStr"
                val existing = map[key]
                if (existing == null) {
                    map[key] = Triple(yrStr.toIntOrNull() ?: 2024, dateAdded, 1)
                } else {
                    map[key] = Triple(existing.first, dateAdded, existing.third + 1)
                }
            }
        }

        val results = mutableListOf<TimelineMonth>()
        map.forEach { (key, data) ->
            val parts = key.split("-")
            val yr = parts.getOrNull(0)?.toIntOrNull() ?: 2024
            val mo = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.YEAR, yr)
            cal.set(java.util.Calendar.MONTH, mo - 1)
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val label = sdfMonthYear.format(cal.time)
            results.add(
                TimelineMonth(
                    year = yr,
                    month = mo,
                    monthYearLabel = label,
                    count = data.third,
                    minTimestampSeconds = data.second
                )
            )
        }
        return results
    }

    /**
     * Fetches all images/videos currently marked as DELETE for review before permanent removal.
     */
    @Synchronized
    fun getPendingDeletes(isVideo: Boolean? = null): List<ImageItem> {
        val db = readableDatabase
        val where = if (isVideo == null) {
            "$COL_STATUS = '${SortStatus.DELETE.name}'"
        } else {
            "$COL_STATUS = '${SortStatus.DELETE.name}' AND $COL_IS_VIDEO = ${if (isVideo) 1 else 0}"
        }
        val query = """
            SELECT $COL_ID, $COL_URI, $COL_FILE_PATH, $COL_DISPLAY_NAME, 
                   $COL_DATE_ADDED, $COL_SIZE_BYTES, $COL_BUCKET_NAME, $COL_STATUS, $COL_UPDATED_AT,
                   $COL_IS_VIDEO, $COL_DURATION_MS
            FROM $TABLE_PHOTOS
            WHERE $where
            ORDER BY $COL_UPDATED_AT DESC
        """.trimIndent()

        val results = mutableListOf<ImageItem>()
        db.rawQuery(query, null).use { cursor ->
            while (cursor.moveToNext()) {
                results.add(cursorToImageItem(cursor))
            }
        }
        return results
    }

    /**
     * Updates multiple images' triage label in a single transaction (re-labeling).
     */
    @Synchronized
    fun updateMultipleStatus(photoIds: List<Long>, newStatus: SortStatus) {
        if (photoIds.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            val placeholders = photoIds.joinToString(",") { "?" }
            val args = (listOf(newStatus.name, now.toString()) + photoIds.map { it.toString() }).toTypedArray()
            db.execSQL("UPDATE $TABLE_PHOTOS SET $COL_STATUS = ?, $COL_UPDATED_AT = ? WHERE $COL_ID IN ($placeholders)", args)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Fetches photos or videos filtered by status and optional bucket/album.
     */
    @Synchronized
    fun getPhotosByStatus(status: SortStatus, bucketName: String? = null, isVideo: Boolean = false, limit: Int = 500): List<ImageItem> {
        val db = readableDatabase
        val whereClause = StringBuilder()
        val args = mutableListOf<String>()

        if (status == SortStatus.UNCATEGORIZED) {
            whereClause.append("($COL_STATUS = 'UNCATEGORIZED' OR $COL_STATUS = 'UNSORTED')")
        } else {
            whereClause.append("$COL_STATUS = ?")
            args.add(status.name)
        }

        whereClause.append(" AND $COL_IS_VIDEO = ${if (isVideo) 1 else 0}")

        if (!bucketName.isNullOrBlank() && bucketName != "All Images" && bucketName != "All Folders" && bucketName != "All Videos") {
            whereClause.append(" AND $COL_BUCKET_NAME = ?")
            args.add(bucketName)
        }

        val query = """
            SELECT $COL_ID, $COL_URI, $COL_FILE_PATH, $COL_DISPLAY_NAME, 
                   $COL_DATE_ADDED, $COL_SIZE_BYTES, $COL_BUCKET_NAME, $COL_STATUS, $COL_UPDATED_AT,
                   $COL_IS_VIDEO, $COL_DURATION_MS
            FROM $TABLE_PHOTOS
            WHERE $whereClause
            ORDER BY $COL_DATE_ADDED DESC
            LIMIT $limit
        """.trimIndent()

        val results = mutableListOf<ImageItem>()
        db.rawQuery(query, if (args.isEmpty()) null else args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                results.add(cursorToImageItem(cursor))
            }
        }
        return results
    }

    /**
     * Returns total statistics filtered by media type (Photos vs Videos).
     */
    @Synchronized
    fun getStats(bucketName: String? = null, isVideo: Boolean = false): SortStats {
        val db = readableDatabase
        val whereClause = StringBuilder("WHERE $COL_IS_VIDEO = ${if (isVideo) 1 else 0}")
        val args = mutableListOf<String>()

        if (!bucketName.isNullOrBlank() && bucketName != "All Images" && bucketName != "All Folders" && bucketName != "All Videos") {
            whereClause.append(" AND $COL_BUCKET_NAME = ?")
            args.add(bucketName)
        }

        val query = """
            SELECT 
                COUNT(*) AS total,
                SUM(CASE WHEN $COL_STATUS = 'KEEP' THEN 1 ELSE 0 END) AS keeps,
                SUM(CASE WHEN $COL_STATUS = 'DELETE' THEN 1 ELSE 0 END) AS deletes,
                SUM(CASE WHEN $COL_STATUS = 'LATER' THEN 1 ELSE 0 END) AS laters,
                SUM(CASE WHEN $COL_STATUS = 'UNCATEGORIZED' OR $COL_STATUS = 'UNSORTED' THEN 1 ELSE 0 END) AS uncategorized,
                SUM(CASE WHEN $COL_STATUS = 'DELETE' THEN $COL_SIZE_BYTES ELSE 0 END) AS delete_size
            FROM $TABLE_PHOTOS $whereClause
        """.trimIndent()

        db.rawQuery(query, if (args.isEmpty()) null else args.toTypedArray()).use { cursor ->
            if (cursor.moveToNext()) {
                return SortStats(
                    totalCount = cursor.getInt(0),
                    keepCount = cursor.getInt(1),
                    deleteCount = cursor.getInt(2),
                    laterCount = cursor.getInt(3),
                    uncategorizedCount = cursor.getInt(4),
                    deleteSizeBytes = cursor.getLong(5)
                )
            }
        }
        return SortStats()
    }

    /**
     * Lists all albums/folders with their media count for the specified media type.
     */
    @Synchronized
    fun getAlbums(isVideo: Boolean = false): List<AlbumFilter> {
        val db = readableDatabase
        val query = """
            SELECT $COL_BUCKET_NAME, COUNT(*) AS cnt 
            FROM $TABLE_PHOTOS 
            WHERE $COL_IS_VIDEO = ${if (isVideo) 1 else 0}
            GROUP BY $COL_BUCKET_NAME 
            ORDER BY cnt DESC
        """.trimIndent()

        val list = mutableListOf<AlbumFilter>()
        db.rawQuery(query, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(AlbumFilter(cursor.getString(0) ?: "Unsorted", cursor.getInt(1)))
            }
        }
        return list
    }

    /**
     * Called after successfully executing Android system delete to permanently clean up local DB.
     */
    @Synchronized
    fun removePermanentlyDeletedPhotos(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val placeholders = ids.joinToString(",") { "?" }
            val args = ids.map { it.toString() }.toTypedArray()
            db.delete(TABLE_PHOTOS, "$COL_ID IN ($placeholders)", args)
            db.delete(TABLE_HISTORY, "$COL_HIST_PHOTO_ID IN ($placeholders)", args)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Reverts all images with a particular status (e.g. resets LATER back to UNCATEGORIZED).
     */
    @Synchronized
    fun resetStatusToUncategorized(targetStatus: SortStatus) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, SortStatus.UNCATEGORIZED.name)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        db.update(TABLE_PHOTOS, values, "$COL_STATUS = ?", arrayOf(targetStatus.name))
    }

    private fun cursorToImageItem(cursor: Cursor): ImageItem {
        val isVideo = if (cursor.columnCount > 9) cursor.getInt(9) == 1 else false
        val durationMs = if (cursor.columnCount > 10) cursor.getLong(10) else 0L
        return ImageItem(
            id = cursor.getLong(0),
            contentUriString = cursor.getString(1),
            filePath = cursor.getString(2) ?: "",
            displayName = cursor.getString(3) ?: "Media",
            dateAdded = cursor.getLong(4),
            sizeBytes = cursor.getLong(5),
            bucketName = cursor.getString(6) ?: "Gallery",
            status = SortStatus.fromString(cursor.getString(7)),
            updatedAt = cursor.getLong(8),
            isVideo = isVideo,
            durationMs = durationMs
        )
    }
}
