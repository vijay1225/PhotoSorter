package com.example.photosort.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photosort.data.model.AlbumFilter
import com.example.photosort.data.model.ImageItem
import com.example.photosort.data.model.SortStats
import com.example.photosort.data.model.SortStatus
import com.example.photosort.ui.components.FolderChipsRow
import com.example.photosort.ui.theme.DeleteRed
import com.example.photosort.ui.theme.KeepGreen
import com.example.photosort.ui.theme.LaterAmber
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Minimalist Sections screen: Unlabeled, Keep, Delete, Later.
 * Allows filtering by folder, multi-selecting photos, sharing, and re-labeling them with 1 tap.
 */
@Composable
fun LibrarySectionsScreen(
    currentSection: SortStatus,
    items: List<ImageItem>,
    stats: SortStats,
    albums: List<AlbumFilter> = emptyList(),
    selectedAlbum: String? = null,
    onSelectAlbum: (String?) -> Unit = {},
    onSelectSection: (SortStatus) -> Unit,
    onRelabelSelected: (List<Long>, SortStatus) -> Unit,
    onShareSelected: ((List<ImageItem>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedIds = remember { mutableStateListOf<Long>() }
    var inspectingItem by remember { mutableStateOf<ImageItem?>(null) }

    // 4 Clean Sections: Unlabeled, Keep, Delete, Later
    val sections = listOf(
        SortStatus.UNCATEGORIZED to "Unlabeled (${stats.uncategorizedCount})",
        SortStatus.KEEP to "Keep (${stats.keepCount})",
        SortStatus.DELETE to "Delete (${stats.deleteCount})",
        SortStatus.LATER to "Later (${stats.laterCount})"
    )

    val selectedSectionIndex = sections.indexOfFirst { it.first == currentSection }.coerceAtLeast(0)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Folder category selector (All Images, DCIM, Screenshots, etc.)
            if (albums.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    FolderChipsRow(
                        albums = albums,
                        selectedAlbum = selectedAlbum,
                        totalCount = stats.totalCount,
                        onSelectAlbum = { album ->
                            selectedIds.clear()
                            inspectingItem = null
                            onSelectAlbum(album)
                        },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            // 4-Tab Primary Header
            PrimaryTabRow(
                selectedTabIndex = selectedSectionIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                sections.forEachIndexed { index, (status, title) ->
                    Tab(
                        selected = selectedSectionIndex == index,
                        onClick = {
                            selectedIds.clear()
                            inspectingItem = null
                            onSelectSection(status)
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedSectionIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // Top action & select all bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedIds.isEmpty()) {
                        "${items.size} photos"
                    } else {
                        "${selectedIds.size} of ${items.size} selected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onShareSelected != null && selectedIds.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                val selectedPhotos = items.filter { it.id in selectedIds }
                                onShareSelected(selectedPhotos)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (items.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedIds.size == items.size) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(items.map { it.id })
                                }
                            }
                        ) {
                            Text(if (selectedIds.size == items.size) "Deselect All" else "Select All")
                        }
                    }
                }
            }

            // Photo Grid
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Photos in ${currentSection.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Photos labeled as ${currentSection.displayName} will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 85.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(items, key = { it.id }) { image ->
                        val isSelected = selectedIds.contains(image.id)
                        SelectableGridItem(
                            image = image,
                            isSelected = isSelected,
                            onClickItem = {
                                if (selectedIds.isNotEmpty()) {
                                    if (isSelected) selectedIds.remove(image.id) else selectedIds.add(image.id)
                                } else {
                                    inspectingItem = image
                                }
                            },
                            onToggleSelect = {
                                if (isSelected) selectedIds.remove(image.id) else selectedIds.add(image.id)
                            }
                        )
                    }
                }
            }
        }

        // FLOATING RE-LABEL BAR (Appears when 1 or more images are selected)
        AnimatedVisibility(
            visible = selectedIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Re-label ${selectedIds.size} photo(s) to:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        // Re-label to Keep
                        if (currentSection != SortStatus.KEEP) {
                            RelabelButton(
                                label = "Keep",
                                color = KeepGreen,
                                icon = Icons.Default.Check,
                                onClick = {
                                    onRelabelSelected(selectedIds.toList(), SortStatus.KEEP)
                                    selectedIds.clear()
                                }
                            )
                        }

                        // Re-label to Delete
                        if (currentSection != SortStatus.DELETE) {
                            RelabelButton(
                                label = "Delete",
                                color = DeleteRed,
                                icon = Icons.Default.Delete,
                                onClick = {
                                    onRelabelSelected(selectedIds.toList(), SortStatus.DELETE)
                                    selectedIds.clear()
                                }
                            )
                        }

                        // Re-label to Later
                        if (currentSection != SortStatus.LATER) {
                            RelabelButton(
                                label = "Later",
                                color = LaterAmber,
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                onClick = {
                                    onRelabelSelected(selectedIds.toList(), SortStatus.LATER)
                                    selectedIds.clear()
                                }
                            )
                        }

                        // Re-label to Unlabeled
                        if (currentSection != SortStatus.UNCATEGORIZED) {
                            RelabelButton(
                                label = "Unlabeled",
                                color = MaterialTheme.colorScheme.primary,
                                icon = Icons.Default.RestartAlt,
                                onClick = {
                                    onRelabelSelected(selectedIds.toList(), SortStatus.UNCATEGORIZED)
                                    selectedIds.clear()
                                }
                            )
                        }
                    }
                }
            }
        }
        // Single Image Quick Swipe Decision Dialog (Opens when an image is tapped in grid)
        inspectingItem?.let { item ->
            SingleImageSwipeDecisionDialog(
                item = item,
                onDismiss = { inspectingItem = null },
                onDecision = { newStatus ->
                    val idToRelabel = item.id
                    inspectingItem = null
                    onRelabelSelected(listOf(idToRelabel), newStatus)
                }
            )
        }
    }
}

@Composable
private fun RelabelButton(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = color.copy(alpha = 0.18f),
            contentColor = color
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SelectableGridItem(
    image: ImageItem,
    isSelected: Boolean,
    onClickItem: () -> Unit,
    onToggleSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClickItem() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.contentUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(24.dp)
                    .clickable { onToggleSelect() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Unselected",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
            ) {
                Text(
                    text = image.formattedSize,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (image.isVideo) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = if (image.formattedDuration.isNotEmpty()) "▶ ${image.formattedDuration}" else "▶",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated modal dialog for inspecting an individual image from Sections
 * and making a quick swipe decision (Swipe UP -> Keep, Swipe DOWN -> Delete, Swipe LEFT -> Later).
 * Dismisses immediately back to the same section upon decision!
 */
@Composable
private fun SingleImageSwipeDecisionDialog(
    item: ImageItem,
    onDismiss: () -> Unit,
    onDecision: (SortStatus) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val verticalThresholdPx = 150f
    val horizontalThresholdPx = 120f

    val dragHint: String? = when {
        abs(offsetY) > abs(offsetX) -> {
            if (offsetY < -50f) "KEEP"
            else if (offsetY > 50f) "DELETE"
            else null
        }
        offsetX < -50f -> "LATER"
        else -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.82f)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .clickable(enabled = false) {}
                    .pointerInput(item.id) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    offsetY < -verticalThresholdPx && abs(offsetY) > abs(offsetX) -> {
                                        onDecision(SortStatus.KEEP)
                                    }
                                    offsetY > verticalThresholdPx && abs(offsetY) > abs(offsetX) -> {
                                        onDecision(SortStatus.DELETE)
                                    }
                                    offsetX < -horizontalThresholdPx -> {
                                        onDecision(SortStatus.LATER)
                                    }
                                    else -> {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        )
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Center Media Preview
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.contentUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Header Scrim & Details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.bucketName} · ${item.formattedSize} · ${item.formattedDate}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    // Swipe Hint Badges
                    when (dragHint) {
                        "KEEP" -> {
                            Surface(
                                color = KeepGreen,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("KEEP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                        "DELETE" -> {
                            Surface(
                                color = DeleteRed,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 75.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("DELETE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                        "LATER" -> {
                            Surface(
                                color = LaterAmber,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LATER", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    // Bottom Quick Decision Buttons Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = Color.Black.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Later button (Swipe Left)
                            FilledTonalButton(
                                onClick = { onDecision(SortStatus.LATER) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = LaterAmber.copy(alpha = 0.25f),
                                    contentColor = LaterAmber
                                )
                            ) {
                                Text("⏳ Later", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Delete button (Swipe Down)
                            FilledTonalButton(
                                onClick = { onDecision(SortStatus.DELETE) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = DeleteRed.copy(alpha = 0.25f),
                                    contentColor = DeleteRed
                                )
                            ) {
                                Text("🗑️ Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Keep button (Swipe Up)
                            FilledTonalButton(
                                onClick = { onDecision(SortStatus.KEEP) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = KeepGreen.copy(alpha = 0.25f),
                                    contentColor = KeepGreen
                                )
                            ) {
                                Text("✅ Keep", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

