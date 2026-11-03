package com.example.photosort.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photosort.data.model.AlbumFilter
import com.example.photosort.data.model.MediaType
import com.example.photosort.data.model.SortStats
import com.example.photosort.ui.theme.DeleteRed
import com.example.photosort.ui.theme.KeepGreen

/**
 * Minimalist top header featuring Photos / Videos toggle,
 * folder chips (All, DCIM, Screenshots, etc.), and 3-category live counts.
 */
@Composable
fun ProgressStatsHeader(
    stats: SortStats,
    albums: List<AlbumFilter>,
    selectedAlbum: String?,
    onSelectAlbum: (String?) -> Unit,
    mediaType: MediaType = MediaType.PHOTOS,
    onMediaTypeChange: (MediaType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = stats.progressFraction,
        label = "progress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Media Type Selector: Photos vs Videos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        FilterChip(
                            selected = mediaType == MediaType.PHOTOS,
                            onClick = { onMediaTypeChange(MediaType.PHOTOS) },
                            label = {
                                Text(
                                    "📸 Photos",
                                    fontWeight = if (mediaType == MediaType.PHOTOS) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = mediaType == MediaType.VIDEOS,
                            onClick = { onMediaTypeChange(MediaType.VIDEOS) },
                            label = {
                                Text(
                                    "🎬 Videos",
                                    fontWeight = if (mediaType == MediaType.VIDEOS) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontal folder chips
            FolderChipsRow(
                albums = albums,
                selectedAlbum = selectedAlbum,
                totalCount = stats.totalCount,
                onSelectAlbum = onSelectAlbum
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3-Category Minimalist Counter (Uncategorized, Keep, Delete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Uncategorized: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.uncategorizedCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Keep: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.keepCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KeepGreen
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Delete: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.deleteCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeleteRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Slim progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Reusable horizontal scrollable chips for folder/album selection.
 */
@Composable
fun FolderChipsRow(
    albums: List<AlbumFilter>,
    selectedAlbum: String?,
    totalCount: Int,
    onSelectAlbum: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // All Images
        item {
            val isAllSelected = selectedAlbum == null || selectedAlbum == "All Images" || selectedAlbum == "All Folders"
            FilterChip(
                selected = isAllSelected,
                onClick = { onSelectAlbum(null) },
                label = {
                    Text(
                        text = "All Images ($totalCount)",
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Individual folders (DCIM, Screenshots, WhatsApp, etc.)
        items(albums) { album ->
            val isSelected = selectedAlbum == album.bucketName
            val icon = when {
                album.bucketName.contains("camera", ignoreCase = true) || album.bucketName.contains("dcim", ignoreCase = true) -> Icons.Default.CameraAlt
                album.bucketName.contains("screenshot", ignoreCase = true) -> Icons.Default.Smartphone
                else -> Icons.Default.Folder
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSelectAlbum(album.bucketName) },
                label = {
                    Text(
                        text = "${album.bucketName} (${album.count})",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
