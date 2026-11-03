package com.example.photosort.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.photosort.data.model.AlbumFilter
import com.example.photosort.data.model.ImageItem
import com.example.photosort.data.model.MediaType
import com.example.photosort.data.model.SortOrder
import com.example.photosort.data.model.SortStats
import com.example.photosort.data.model.SortStatus
import com.example.photosort.data.model.TimelineMonth
import com.example.photosort.ui.components.ActionButtonsBar
import com.example.photosort.ui.components.ImageTriageCard

/**
 * Minimalist, edge-to-edge Triage screen designed for maximum media visibility:
 *  - Ultra-compact floating top bar (Photos/Videos pill, folder dropdown, sort direction, date timeline)
 *  - Massive full-bleed filmstrip triage card filling over 85% of screen height
 *  - Ergonomic bottom thumb bar (Prev, Delete, Undo, Share, Keep, Later)
 */
@Composable
fun TriageScreen(
    currentImage: ImageItem?,
    prevImage: ImageItem? = null,
    nextImage: ImageItem? = null,
    queue: List<ImageItem> = emptyList(),
    timelineMonths: List<TimelineMonth> = emptyList(),
    stats: SortStats,
    albums: List<AlbumFilter>,
    selectedAlbum: String?,
    mediaType: MediaType = MediaType.PHOTOS,
    sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    jumpedDateLabel: String? = null,
    queueIndex: Int = 0,
    canUndo: Boolean,
    isLoading: Boolean,
    hasVideoPermission: Boolean = true,
    onRequestVideoPermission: () -> Unit = {},
    onSortAction: (SortStatus) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onUndoClick: () -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onMediaTypeChange: (MediaType) -> Unit,
    onToggleSortOrder: () -> Unit = {},
    onJumpToDate: (Int, String) -> Unit = { _, _ -> },
    onJumpToTimestamp: (Long, String) -> Unit = { _, _ -> },
    onClearDateJump: () -> Unit = {},
    onPlayVideo: (ImageItem) -> Unit,
    onResetToStart: () -> Unit,
    onRefreshScan: () -> Unit,
    onShowAboutDeveloper: () -> Unit = {},
    onShareMedia: ((ImageItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDateTimelineDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Ultra-compact single-row header (Only ~46dp)
        CompactTriageHeader(
            mediaType = mediaType,
            onMediaTypeChange = onMediaTypeChange,
            albums = albums,
            selectedAlbum = selectedAlbum,
            onSelectAlbum = onSelectAlbum,
            sortOrder = sortOrder,
            onToggleSortOrder = onToggleSortOrder,
            onOpenDateTimeline = { showDateTimelineDialog = true },
            hasDateJump = jumpedDateLabel != null,
            stats = stats,
            queueIndex = queueIndex,
            onResetToStart = onResetToStart,
            onRefreshScan = onRefreshScan,
            onShowAboutDeveloper = onShowAboutDeveloper
        )

        // Session-only Date Jump active notification banner
        if (jumpedDateLabel != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Swiping from $jumpedDateLabel (Session only)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onClearDateJump,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Return to Top",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Center Content: Maximum size Filmstrip Swipe Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (mediaType == MediaType.VIDEOS) "Scanning videos..." else "Scanning photos...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                currentImage != null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ImageTriageCard(
                            currentImage = currentImage,
                            prevImage = prevImage,
                            nextImage = nextImage,
                            onSortAction = onSortAction,
                            onSwipeLeft = onSwipeLeft,
                            onSwipeRight = onSwipeRight,
                            onPlayVideo = onPlayVideo,
                            onShareMedia = onShareMedia
                        )
                    }
                }

                else -> {
                    // Empty Queue State / Permission Needed State
                    EmptyQueueView(
                        mediaType = mediaType,
                        folderName = selectedAlbum ?: (if (mediaType == MediaType.VIDEOS) "All Videos" else "All Photos"),
                        uncategorizedCount = stats.uncategorizedCount,
                        laterCount = stats.laterCount,
                        hasVideoPermission = hasVideoPermission,
                        onRequestVideoPermission = onRequestVideoPermission,
                        onResetToStart = onResetToStart,
                        onRefreshScan = onRefreshScan
                    )
                }
            }
        }

        // Bottom Action Buttons matching gestures (Height ~52dp)
        ActionButtonsBar(
            canUndo = canUndo,
            onUndoClick = onUndoClick,
            onSortAction = onSortAction,
            onMoveAction = { dir ->
                if (dir < 0) onSwipeRight() else onSwipeLeft()
            },
            onShareClick = if (currentImage != null && onShareMedia != null) { { onShareMedia(currentImage) } } else null
        )

        // Date / Month Timeline Dialog
        if (showDateTimelineDialog) {
            DateJumpPickerDialog(
                timelineMonths = timelineMonths,
                onDismiss = { showDateTimelineDialog = false },
                onSelectTimestamp = { timestamp, label ->
                    onJumpToTimestamp(timestamp, label)
                    showDateTimelineDialog = false
                }
            )
        }
    }
}

/**
 * Ultra-compact single-row header:
 * Combines Photos/Videos toggle, Folder dropdown selector, Sort Order toggle, Date Jump, counters, and reset/refresh
 * in a sleek 46dp bar without consuming vertical space.
 */
@Composable
private fun CompactTriageHeader(
    mediaType: MediaType,
    onMediaTypeChange: (MediaType) -> Unit,
    albums: List<AlbumFilter>,
    selectedAlbum: String?,
    onSelectAlbum: (String?) -> Unit,
    sortOrder: SortOrder,
    onToggleSortOrder: () -> Unit,
    onOpenDateTimeline: () -> Unit,
    hasDateJump: Boolean,
    stats: SortStats,
    queueIndex: Int,
    onResetToStart: () -> Unit,
    onRefreshScan: () -> Unit,
    onShowAboutDeveloper: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isAlbumMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Photos vs Videos Toggle (Pill)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (mediaType == MediaType.PHOTOS) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.clickable { onMediaTypeChange(MediaType.PHOTOS) }
                    ) {
                        Text(
                            text = "📸 Photos",
                            fontSize = 11.sp,
                            fontWeight = if (mediaType == MediaType.PHOTOS) FontWeight.Bold else FontWeight.Medium,
                            color = if (mediaType == MediaType.PHOTOS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (mediaType == MediaType.VIDEOS) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.clickable { onMediaTypeChange(MediaType.VIDEOS) }
                    ) {
                        Text(
                            text = "🎬 Videos",
                            fontSize = 11.sp,
                            fontWeight = if (mediaType == MediaType.VIDEOS) FontWeight.Bold else FontWeight.Medium,
                            color = if (mediaType == MediaType.VIDEOS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. Folder Dropdown Chip (Compact)
            Box {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { isAlbumMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = (selectedAlbum ?: "All").take(10),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isAlbumMenuExpanded,
                    onDismissRequest = { isAlbumMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "All ${if (mediaType == MediaType.VIDEOS) "Videos" else "Photos"} (${stats.totalCount})",
                                fontWeight = if (selectedAlbum == null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onSelectAlbum(null)
                            isAlbumMenuExpanded = false
                        }
                    )
                    albums.forEach { album ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${album.bucketName} (${album.count})",
                                    fontWeight = if (selectedAlbum == album.bucketName) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                onSelectAlbum(album.bucketName)
                                isAlbumMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. Sort Order Toggle Chip (Newest vs Oldest First)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onToggleSortOrder() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (sortOrder == SortOrder.NEWEST_FIRST) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = "Toggle Sort Order",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (sortOrder == SortOrder.NEWEST_FIRST) "Newest" else "Oldest",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Quick Actions: Date Timeline Jump, Reset, Refresh, About Developer
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDateTimeline,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Jump to Date",
                        tint = if (hasDateJump) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }

                if (queueIndex > 0) {
                    IconButton(
                        onClick = onResetToStart,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Review from start ($queueIndex skipped)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshScan,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onShowAboutDeveloper,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About Developer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyQueueView(
    mediaType: MediaType,
    folderName: String,
    uncategorizedCount: Int,
    laterCount: Int = 0,
    hasVideoPermission: Boolean,
    onRequestVideoPermission: () -> Unit,
    onResetToStart: () -> Unit,
    onRefreshScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (mediaType == MediaType.VIDEOS && !hasVideoPermission) {
            // Video permission missing banner
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Video Permission Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To browse and sort videos, PhotoSorter needs access to videos on your device.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRequestVideoPermission,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Video Permission", fontWeight = FontWeight.Bold)
            }
            return
        }

        val totalRemaining = uncategorizedCount + laterCount

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (totalRemaining > 0) Icons.Default.RestartAlt else Icons.Default.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (totalRemaining > 0) {
            Text(
                text = "Queue Completed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            val detailsText = if (uncategorizedCount > 0 && laterCount > 0) {
                "You have $uncategorizedCount unlabeled and $laterCount 'Later' items in $folderName."
            } else if (laterCount > 0) {
                "All new images are sorted! You have $laterCount items saved in your 'Later' queue in $folderName."
            } else {
                "You have $uncategorizedCount unlabeled items remaining in $folderName."
            }

            Text(
                text = detailsText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            FilledTonalButton(
                onClick = onResetToStart,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Review From Start", fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "All Sorted in $folderName!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No uncategorized media remaining here. Select another folder above or check the Sections tab to review your sorted media.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onRefreshScan,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh Library")
            }
        }
    }
}

/**
 * Multi-year Calendar DatePicker & All-Time Timeline dialog.
 * Enables jumping directly to any exact date (via Calendar) or any month across all years (via All-Time Timeline).
 * Session-only: when the app is restarted, sorting automatically resumes from the usual top images.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateJumpPickerDialog(
    timelineMonths: List<TimelineMonth>,
    onDismiss: () -> Unit,
    onSelectTimestamp: (Long, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Calendar, 1: All-Time Timeline
    val datePickerState = rememberDatePickerState()
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Jump to Date",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Temporary jump: resets when app restarts",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Preset Chips (Recent, 3 Mo, 6 Mo, 1 Yr)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickPresetChip(label = "Recent", modifier = Modifier.weight(1f)) {
                        val nowSeconds = System.currentTimeMillis() / 1000L
                        onSelectTimestamp(nowSeconds, "Recent")
                    }
                    QuickPresetChip(label = "3 Months", modifier = Modifier.weight(1f)) {
                        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }
                        onSelectTimestamp(cal.timeInMillis / 1000L, "3 Months Ago")
                    }
                    QuickPresetChip(label = "6 Months", modifier = Modifier.weight(1f)) {
                        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }
                        onSelectTimestamp(cal.timeInMillis / 1000L, "6 Months Ago")
                    }
                    QuickPresetChip(label = "1 Year", modifier = Modifier.weight(1f)) {
                        val cal = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
                        onSelectTimestamp(cal.timeInMillis / 1000L, "1 Year Ago")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Calendar",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            val countText = if (timelineMonths.isNotEmpty()) " (${timelineMonths.size} Mo)" else ""
                            Text(
                                "All-Time$countText",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (selectedTab == 0) {
                        // Calendar DatePicker view
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                DatePicker(
                                    state = datePickerState,
                                    showModeToggle = true,
                                    title = null,
                                    headline = null
                                )
                            }

                            val selectedMillis = datePickerState.selectedDateMillis
                            val jumpLabel = if (selectedMillis != null) {
                                dateFormatter.format(Date(selectedMillis))
                            } else {
                                "Pick a Date Above"
                            }

                            Button(
                                onClick = {
                                    if (selectedMillis != null) {
                                        onSelectTimestamp(selectedMillis / 1000L, jumpLabel)
                                    }
                                },
                                enabled = selectedMillis != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedMillis != null) "Jump to $jumpLabel" else "Select a date to jump",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // All-Time Timeline grouped by year
                        if (timelineMonths.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No media timeline dates found in database.\nRefresh library or choose another folder.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            val groupedByYear = remember(timelineMonths) {
                                timelineMonths.groupBy { it.year }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groupedByYear.forEach { (year, months) ->
                                    val totalYearCount = months.sumOf { it.count }
                                    item(key = "year_$year") {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Year $year",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "$totalYearCount items",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    items(months, key = { "m_${it.year}_${it.month}_${it.monthYearLabel}" }) { monthItem ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onSelectTimestamp(monthItem.minTimestampSeconds, monthItem.monthYearLabel)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = monthItem.monthYearLabel,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surface
                                                ) {
                                                    Text(
                                                        text = "${monthItem.count} items",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

