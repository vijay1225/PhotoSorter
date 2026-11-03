package com.example.photosort.ui.main

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photosort.data.model.MediaType
import com.example.photosort.data.model.SortStatus
import com.example.photosort.ui.components.AboutDeveloperDialog
import com.example.photosort.ui.components.PermissionCard
import com.example.photosort.ui.components.VideoPreviewDialog
import com.example.photosort.ui.screens.LibrarySectionsScreen
import com.example.photosort.ui.screens.PendingDeletesScreen
import com.example.photosort.ui.screens.TriageScreen
import com.example.photosort.util.ShareHelper

/**
 * Root host screen providing permission checks, top app bar, 3-tab navigation bar,
 * and system delete intent sender dispatching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAboutDialog by remember { mutableStateOf(false) }

    // Permission Launcher: Include images and videos
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val anyGranted = results.values.any { it } || permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (anyGranted) {
            viewModel.onPermissionGranted()
        }
    }

    // Check permissions on start: if photos granted but video isn't, request missing permissions
    LaunchedEffect(Unit) {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        val anyGranted = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (anyGranted) {
            viewModel.onPermissionGranted()
        }
        if (!allGranted) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val requestVideoPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // System Deletion Request Launcher (Android 11+ MediaStore.createDeleteRequest)
    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val isConfirmed = result.resultCode == Activity.RESULT_OK
        viewModel.onBatchDeleteResult(isConfirmed)
    }

    // Trigger system deletion dialog if an IntentSender is ready
    LaunchedEffect(uiState.pendingDeleteIntentSender) {
        uiState.pendingDeleteIntentSender?.let { sender ->
            val request = IntentSenderRequest.Builder(sender).build()
            deleteIntentLauncher.launch(request)
        }
    }

    if (!uiState.hasPermission) {
        PermissionCard(
            onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
            modifier = modifier
        )
        return
    }

    val hasVideoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Maximize space: Hide bulky top app bar on the Sort tab so media fills entire screen
            if (uiState.selectedTab != 0) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (uiState.selectedTab) {
                                1 -> "Library & Sections"
                                else -> "Trash Review"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Developer Information"
                            )
                        }
                        IconButton(onClick = { viewModel.refreshAll() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Library"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                // TAB 0: SORT (Swipe Card & Folders)
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.stats.uncategorizedCount > 0) {
                                    Badge { Text(uiState.stats.uncategorizedCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Swipe, contentDescription = "Sort")
                        }
                    },
                    label = { Text("Sort") }
                )

                // TAB 1: LIBRARY & SECTIONS (Sorted vs Unsorted & Re-label)
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Sections")
                    },
                    label = { Text("Sections") }
                )

                // TAB 2: TRASH (Pending Deletions & Selective Delete)
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.pendingDeletes.isNotEmpty()) {
                                    Badge { Text(uiState.pendingDeletes.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Trash")
                        }
                    },
                    label = { Text("Trash") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> {
                    TriageScreen(
                        currentImage = uiState.currentImage,
                        prevImage = uiState.prevImage,
                        nextImage = uiState.nextImage,
                        queue = uiState.queue,
                        timelineMonths = uiState.timelineMonths,
                        stats = uiState.stats,
                        albums = uiState.albums,
                        selectedAlbum = uiState.selectedAlbum,
                        mediaType = uiState.mediaType,
                        sortOrder = uiState.sortOrder,
                        jumpedDateLabel = uiState.jumpedDateLabel,
                        queueIndex = uiState.queueIndex,
                        canUndo = uiState.canUndo,
                        isLoading = uiState.isLoading,
                        hasVideoPermission = hasVideoPermission,
                        onRequestVideoPermission = requestVideoPermission,
                        onSortAction = { viewModel.triageCurrentImage(it) },
                        onSwipeLeft = { viewModel.swipeLeft() },
                        onSwipeRight = { viewModel.swipeRight() },
                        onUndoClick = { viewModel.undo() },
                        onSelectAlbum = { viewModel.selectAlbum(it) },
                        onMediaTypeChange = { newType ->
                            if (newType == MediaType.VIDEOS && !hasVideoPermission) {
                                requestVideoPermission()
                            }
                            viewModel.setMediaType(newType)
                        },
                        onToggleSortOrder = { viewModel.toggleSortOrder() },
                        onJumpToDate = { idx, label -> viewModel.jumpToDate(idx, label) },
                        onJumpToTimestamp = { ts, label -> viewModel.jumpToTimestamp(ts, label) },
                        onClearDateJump = { viewModel.clearDateJump() },
                        onPlayVideo = { viewModel.selectVideoPreview(it) },
                        onResetToStart = { viewModel.resetToStart() },
                        onRefreshScan = { viewModel.refreshAll() },
                        onShowAboutDeveloper = { showAboutDialog = true },
                        onShareMedia = { item -> ShareHelper.shareSingleMedia(context, item) }
                    )
                }
                1 -> {
                    LibrarySectionsScreen(
                        currentSection = uiState.librarySection,
                        items = uiState.libraryPhotos,
                        stats = uiState.stats,
                        albums = uiState.albums,
                        selectedAlbum = uiState.selectedAlbum,
                        onSelectAlbum = { viewModel.selectAlbum(it) },
                        onSelectSection = { viewModel.selectLibrarySection(it) },
                        onRelabelSelected = { ids, status -> viewModel.relabelPhotos(ids, status) },
                        onShareSelected = { items -> ShareHelper.shareMultipleMedia(context, items) }
                    )
                }
                2 -> {
                    PendingDeletesScreen(
                        pendingDeletes = uiState.pendingDeletes,
                        reclaimableSpace = uiState.stats.formattedReclaimableSpace,
                        onRestoreItems = { items, status -> viewModel.restorePendingDeleteItems(items, status) },
                        onDeleteSelected = { items -> viewModel.deleteSelectedItems(items) },
                        onShareItems = { items -> ShareHelper.shareMultipleMedia(context, items) }
                    )
                }
            }

            // Video Preview Dialog overlay
            uiState.selectedVideoPreview?.let { videoItem ->
                VideoPreviewDialog(
                    item = videoItem,
                    onDismiss = { viewModel.selectVideoPreview(null) },
                    onKeep = { viewModel.triageCurrentImage(SortStatus.KEEP) },
                    onDelete = { viewModel.triageCurrentImage(SortStatus.DELETE) },
                    onShare = { ShareHelper.shareSingleMedia(context, videoItem) }
                )
            }

            // Developer Info & Privacy Dialog overlay
            if (showAboutDialog) {
                AboutDeveloperDialog(onDismiss = { showAboutDialog = false })
            }
        }
    }
}
