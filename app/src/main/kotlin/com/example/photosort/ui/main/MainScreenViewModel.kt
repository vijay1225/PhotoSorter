package com.example.photosort.ui.main

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photosort.data.model.AlbumFilter
import com.example.photosort.data.model.ImageItem
import com.example.photosort.data.model.MediaType
import com.example.photosort.data.model.SortOrder
import com.example.photosort.data.model.SortStats
import com.example.photosort.data.model.SortStatus
import com.example.photosort.data.model.TimelineMonth
import com.example.photosort.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Minimalist UI state: Photos vs Videos, 3 categories (Uncategorized, Keep, Delete),
 * swipe Up for Keep, Down for Delete, Left to categorize later, Right for previous.
 */
data class MainUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val selectedTab: Int = 0, // 0: Sort, 1: Sections, 2: Delete/Trash
    val mediaType: MediaType = MediaType.PHOTOS,
    val selectedAlbum: String? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val jumpedDateLabel: String? = null,
    val queue: List<ImageItem> = emptyList(),
    val queueIndex: Int = 0,
    val stats: SortStats = SortStats(),
    val albums: List<AlbumFilter> = emptyList(),
    val timelineMonths: List<TimelineMonth> = emptyList(),
    val pendingDeletes: List<ImageItem> = emptyList(),
    val canUndo: Boolean = false,
    val librarySection: SortStatus = SortStatus.UNCATEGORIZED,
    val libraryPhotos: List<ImageItem> = emptyList(),
    val pendingDeleteIntentSender: IntentSender? = null,
    val activeDeletingItems: List<ImageItem> = emptyList(),
    val selectedVideoPreview: ImageItem? = null
) {
    val prevImage: ImageItem?
        get() = queue.getOrNull(queueIndex - 1)

    val currentImage: ImageItem?
        get() = queue.getOrNull(queueIndex)

    val nextImage: ImageItem?
        get() = queue.getOrNull(queueIndex + 1)
}

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ImageRepository(application)
    private val prefs: SharedPreferences = application.getSharedPreferences("photosorter_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private fun getPositionPrefKey(mediaType: MediaType, bucketName: String?): String =
        "last_seen_id_${mediaType.name}_${bucketName ?: "ALL"}"

    private fun saveLastSeenId(mediaId: Long?) {
        // Do not persist position if in temporary date jump mode!
        // As requested: closing the app must always resume from top/recent incoming images.
        if (_uiState.value.jumpedDateLabel != null) return

        val key = getPositionPrefKey(_uiState.value.mediaType, _uiState.value.selectedAlbum)
        if (mediaId != null && mediaId > 0) {
            prefs.edit().putLong(key, mediaId).apply()
        }
    }

    private fun getLastSeenId(mediaType: MediaType, bucketName: String?): Long {
        val key = getPositionPrefKey(mediaType, bucketName)
        return prefs.getLong(key, -1L)
    }

    /**
     * Resets queue back to start so the user can review all skipped/later items again.
     */
    fun resetToStart() {
        val key = getPositionPrefKey(_uiState.value.mediaType, _uiState.value.selectedAlbum)
        prefs.edit().remove(key).apply()
        _uiState.update { it.copy(queueIndex = 0, jumpedDateLabel = null) }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true) }
        refreshAll()
    }

    fun setMediaType(mediaType: MediaType) {
        if (_uiState.value.mediaType == mediaType) return
        _uiState.update { it.copy(mediaType = mediaType, selectedAlbum = null, queueIndex = 0, jumpedDateLabel = null) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.syncMediaStore(isVideo = (mediaType == MediaType.VIDEOS))
            reloadQueueAndStats(restoreSavedPosition = true)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        if (_uiState.value.sortOrder == sortOrder) return
        _uiState.update { it.copy(sortOrder = sortOrder, queueIndex = 0, jumpedDateLabel = null) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            reloadQueueAndStats(restoreSavedPosition = false)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleSortOrder() {
        val newOrder = if (_uiState.value.sortOrder == SortOrder.NEWEST_FIRST) {
            SortOrder.OLDEST_FIRST
        } else {
            SortOrder.NEWEST_FIRST
        }
        setSortOrder(newOrder)
    }

    /**
     * Transient jump to a specific date/item in the queue.
     * Does NOT save to persistent storage so restarting app resumes from usual top images.
     */
    fun jumpToDate(index: Int, label: String) {
        val maxIndex = (_uiState.value.queue.size - 1).coerceAtLeast(0)
        val targetIndex = index.coerceIn(0, maxIndex)
        _uiState.update { it.copy(queueIndex = targetIndex, jumpedDateLabel = label) }
    }

    /**
     * Jumps to the photo closest to the given timestamp (seconds since epoch),
     * enabling precise jumping from Calendar DatePicker or All-Time Timeline across all years.
     */
    fun jumpToTimestamp(timestampSeconds: Long, label: String) {
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return

        var bestIndex = 0
        var minDiff = Long.MAX_VALUE
        queue.forEachIndexed { index, item ->
            val diff = kotlin.math.abs(item.dateAdded - timestampSeconds)
            if (diff < minDiff) {
                minDiff = diff
                bestIndex = index
            }
        }
        _uiState.update { it.copy(queueIndex = bestIndex, jumpedDateLabel = label) }
    }

    fun clearDateJump() {
        _uiState.update { it.copy(jumpedDateLabel = null, queueIndex = 0) }
        viewModelScope.launch {
            reloadQueueAndStats(restoreSavedPosition = true)
        }
    }

    fun selectVideoPreview(item: ImageItem?) {
        _uiState.update { it.copy(selectedVideoPreview = item) }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val isVideo = _uiState.value.mediaType == MediaType.VIDEOS
            repository.syncMediaStore(isVideo = false)
            repository.syncMediaStore(isVideo = true)
            reloadQueueAndStats(restoreSavedPosition = true)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun reloadQueueAndStats(restoreSavedPosition: Boolean = false) {
        val currentAlbum = _uiState.value.selectedAlbum
        val currentLibrarySection = _uiState.value.librarySection
        val isVideo = _uiState.value.mediaType == MediaType.VIDEOS
        val isOldestFirst = _uiState.value.sortOrder == SortOrder.OLDEST_FIRST

        val queue = repository.getQueue(currentAlbum, isVideo, isOldestFirst = isOldestFirst)
        val stats = repository.getStats(currentAlbum, isVideo)
        val albums = repository.getAlbums(isVideo)
        val timelineMonths = repository.getAllTimelineMonths(currentAlbum, isVideo)
        val pendingDeletes = repository.getPendingDeletes(isVideo)
        val canUndo = repository.canUndo()
        val libraryPhotos = repository.getPhotosByStatus(currentLibrarySection, currentAlbum, isVideo)

        _uiState.update { state ->
            val targetIndex = if (state.jumpedDateLabel != null) {
                state.queueIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
            } else if (restoreSavedPosition) {
                // If there are unlabeled images, always start from index 0 so new images are sorted first
                if (stats.uncategorizedCount > 0) {
                    0
                } else {
                    val savedId = getLastSeenId(_uiState.value.mediaType, currentAlbum)
                    if (savedId > 0) {
                        val idx = queue.indexOfFirst { it.id == savedId }
                        if (idx >= 0) idx else 0
                    } else {
                        0
                    }
                }
            } else {
                state.queueIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
            }

            state.copy(
                queue = queue,
                queueIndex = targetIndex,
                stats = stats,
                albums = albums,
                timelineMonths = timelineMonths,
                pendingDeletes = pendingDeletes,
                canUndo = canUndo,
                libraryPhotos = libraryPhotos
            )
        }
    }

    /**
     * UP = Keep, DOWN = Delete, LEFT = Later
     */
    fun triageCurrentImage(status: SortStatus) {
        val image = _uiState.value.currentImage ?: return

        viewModelScope.launch {
            repository.labelImage(image.id, status)
            reloadQueueAndStats(restoreSavedPosition = false)
            val nextItem = _uiState.value.currentImage
            saveLastSeenId(nextItem?.id)
        }
    }

    /**
     * Swipe LEFT: Categorize LATER.
     * Labels the image as LATER in SQLite, seamlessly queuing it after all unlabeled images.
     * If the image was already in the Later queue, skips forward in the queue.
     */
    fun swipeLeft() {
        val image = _uiState.value.currentImage ?: return
        if (image.status == SortStatus.LATER) {
            moveImage(+1)
        } else {
            triageCurrentImage(SortStatus.LATER)
        }
    }

    /**
     * Swipe RIGHT: Categorize previous image (uncategorized or already categorized)
     */
    fun swipeRight() {
        val state = _uiState.value
        if (state.queueIndex > 0) {
            moveImage(-1)
        } else if (state.canUndo) {
            undo()
        }
    }

    /**
     * Move index and persist position
     */
    fun moveImage(direction: Int) {
        _uiState.update { state ->
            val maxIndex = (state.queue.size - 1).coerceAtLeast(0)
            val newIndex = (state.queueIndex + direction).coerceIn(0, maxIndex)
            val nextItem = state.queue.getOrNull(newIndex)
            saveLastSeenId(nextItem?.id)
            state.copy(queueIndex = newIndex)
        }
    }

    fun undo() {
        viewModelScope.launch {
            val result = repository.undoLastAction()
            if (result != null) {
                reloadQueueAndStats(restoreSavedPosition = false)
            }
        }
    }

    fun selectAlbum(album: String?) {
        _uiState.update { it.copy(selectedAlbum = album, queueIndex = 0) }
        viewModelScope.launch {
            reloadQueueAndStats(restoreSavedPosition = true)
        }
    }

    fun setSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        viewModelScope.launch {
            reloadQueueAndStats()
        }
    }

    fun selectLibrarySection(status: SortStatus) {
        _uiState.update { it.copy(librarySection = status) }
        viewModelScope.launch {
            val isVideo = _uiState.value.mediaType == MediaType.VIDEOS
            val photos = repository.getPhotosByStatus(status, _uiState.value.selectedAlbum, isVideo)
            _uiState.update { it.copy(libraryPhotos = photos) }
        }
    }

    fun relabelPhotos(photoIds: List<Long>, newStatus: SortStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.relabelPhotos(photoIds, newStatus)
            reloadQueueAndStats()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun restorePendingDeleteItems(items: List<ImageItem>, newStatus: SortStatus) {
        val ids = items.map { it.id }
        relabelPhotos(ids, newStatus)
    }

    fun deleteSelectedItems(items: List<ImageItem>) {
        if (items.isEmpty()) return

        val intentSender = repository.createBatchDeleteIntentSender(items)
        if (intentSender != null) {
            _uiState.update {
                it.copy(
                    pendingDeleteIntentSender = intentSender,
                    activeDeletingItems = items
                )
            }
        } else {
            // Legacy delete fallback
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                repository.executeLegacyDelete(items)
                reloadQueueAndStats()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onBatchDeleteIntentConsumed() {
        _uiState.update { it.copy(pendingDeleteIntentSender = null) }
    }

    fun onBatchDeleteResult(success: Boolean) {
        val itemsToDelete = _uiState.value.activeDeletingItems
        onBatchDeleteIntentConsumed()

        if (success && itemsToDelete.isNotEmpty()) {
            val ids = itemsToDelete.map { it.id }
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                repository.onPhotosPermanentlyDeleted(ids)
                reloadQueueAndStats()
                _uiState.update { it.copy(isLoading = false, activeDeletingItems = emptyList()) }
            }
        } else {
            _uiState.update { it.copy(activeDeletingItems = emptyList()) }
        }
    }
}
