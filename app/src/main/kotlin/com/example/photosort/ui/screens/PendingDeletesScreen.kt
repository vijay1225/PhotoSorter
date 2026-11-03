package com.example.photosort.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photosort.data.model.ImageItem
import com.example.photosort.data.model.SortStatus
import com.example.photosort.ui.theme.DeleteRed
import com.example.photosort.ui.theme.DeleteRedContainer
import com.example.photosort.ui.theme.KeepGreen
import com.example.photosort.ui.theme.OnDeleteRed
import com.example.photosort.ui.theme.OnDeleteRedContainer

/**
 * Trash review screen where users can see all photos marked for deletion,
 * select ALL or select SOME, share before deleting, and permanently delete or restore selected photos.
 */
@Composable
fun PendingDeletesScreen(
    pendingDeletes: List<ImageItem>,
    reclaimableSpace: String,
    onRestoreItems: (List<ImageItem>, SortStatus) -> Unit,
    onDeleteSelected: (List<ImageItem>) -> Unit,
    onShareItems: ((List<ImageItem>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Selected IDs for batch deletion or restoration (defaults to all selected for convenience)
    val selectedIds = remember { mutableStateListOf<Long>() }

    // Keep selectedIds in sync when pendingDeletes changes
    LaunchedEffect(pendingDeletes) {
        val validIds = pendingDeletes.map { it.id }.toSet()
        selectedIds.removeAll { it !in validIds }
    }

    val selectedItems = pendingDeletes.filter { it.id in selectedIds }
    val selectedBytes = selectedItems.sumOf { it.sizeBytes }
    val selectedMbFormatted = String.format("%.1f MB", selectedBytes / (1024.0 * 1024.0))

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Header Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (pendingDeletes.isNotEmpty()) DeleteRedContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trash Review",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingDeletes.isNotEmpty()) OnDeleteRedContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (pendingDeletes.isNotEmpty()) {
                                "${pendingDeletes.size} items queued • $reclaimableSpace total"
                            } else {
                                "No photos marked for deletion"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pendingDeletes.isNotEmpty()) OnDeleteRedContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Select All / Deselect All Button
                    if (pendingDeletes.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedIds.size == pendingDeletes.size) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(pendingDeletes.map { it.id })
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedIds.size == pendingDeletes.size) "Deselect All" else "Select All",
                                fontWeight = FontWeight.Bold,
                                color = DeleteRed
                            )
                        }
                    }
                }

                // Batch Actions Bar: Delete Selected & Restore Selected
                if (pendingDeletes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Delete Selected Button
                        Button(
                            onClick = {
                                val target = if (selectedItems.isNotEmpty()) selectedItems else pendingDeletes
                                onDeleteSelected(target)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeleteRed,
                                contentColor = OnDeleteRed
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedIds.isEmpty()) "Delete All" else "Delete (${selectedIds.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Share Selected Button
                        if (onShareItems != null) {
                            FilledTonalButton(
                                onClick = {
                                    val target = if (selectedItems.isNotEmpty()) selectedItems else pendingDeletes
                                    onShareItems(target)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Restore Selected Button
                        FilledTonalButton(
                            onClick = {
                                val target = if (selectedItems.isNotEmpty()) selectedItems else pendingDeletes
                                onRestoreItems(target, SortStatus.KEEP)
                                selectedIds.clear()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedIds.isEmpty()) "Restore All" else "Restore (${selectedIds.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (pendingDeletes.isEmpty()) {
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
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = KeepGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Trash is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Swipe left on any photo in the Sort tab to queue it here for deletion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            // Grid of pending deletions with checkboxes
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(pendingDeletes, key = { it.id }) { item ->
                    val isSelected = item.id in selectedIds
                    SelectableDeleteGridItem(
                        image = item,
                        isSelected = isSelected,
                        onToggleSelect = {
                            if (isSelected) selectedIds.remove(item.id) else selectedIds.add(item.id)
                        },
                        onRestoreSingle = {
                            onRestoreItems(listOf(item), SortStatus.KEEP)
                            selectedIds.remove(item.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableDeleteGridItem(
    image: ImageItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onRestoreSingle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onToggleSelect() }
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

            // Dim overlay when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeleteRed.copy(alpha = 0.3f))
                )
            }

            // Selection Checkbox on Top-Right
            Surface(
                shape = CircleShape,
                color = if (isSelected) DeleteRed else Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Unselected",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quick Restore button on Top-Left
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .clickable { onRestoreSingle() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Size pill at bottom
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = image.formattedSize,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
