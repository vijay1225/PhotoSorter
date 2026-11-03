package com.example.photosort.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photosort.data.model.ImageItem
import com.example.photosort.data.model.SortStatus
import com.example.photosort.ui.theme.DeleteRed
import com.example.photosort.ui.theme.KeepGreen
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Immersive, edge-to-edge Filmstrip Triage Card:
 *  - Maximized viewport taking up over 85% of screen height
 *  - Moving filmstrip carousel physics (side-to-side)
 *  - Swipe LEFT: Categorize later (skip forward)
 *  - Swipe RIGHT: Previous image (re-categorize / undo)
 *  - Swipe UP: KEEP (Green indicator)
 *  - Swipe DOWN: DELETE (Red indicator)
 *  - Prominent glassmorphic Play Button for videos
 *  - Tap to toggle between Full Fit (unclipped) and Fill Zoom
 *  - Instant Share buttons before deleting/sorting
 */
@Composable
fun ImageTriageCard(
    currentImage: ImageItem,
    prevImage: ImageItem? = null,
    nextImage: ImageItem? = null,
    onSortAction: (SortStatus) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onPlayVideo: (ImageItem) -> Unit = {},
    onShareMedia: ((ImageItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val verticalThresholdPx = 180f
    val horizontalThresholdPx = 150f

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Maximize card size to fill available space
        val cardWidth = maxWidth * 0.98f
        val cardHeight = maxHeight * 0.98f
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val cardSpacingPx = with(density) { 10.dp.toPx() }
        val stridePx = cardWidthPx + cardSpacingPx

        // Visual drag badge state
        val dragHint: String? = when {
            abs(offsetY.value) > abs(offsetX.value) -> {
                if (offsetY.value < -60f) "KEEP"
                else if (offsetY.value > 60f) "DELETE"
                else null
            }
            // Dragging left (negative x) = Categorize later
            offsetX.value < -50f -> "LATER"
            // Dragging right (positive x) = Previous image
            offsetX.value > 50f -> "PREVIOUS"
            else -> null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentImage.id) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val x = offsetX.value
                                val y = offsetY.value

                                when {
                                    // Vertical UP: Keep
                                    y < -verticalThresholdPx && abs(y) > abs(x) -> {
                                        offsetY.animateTo(-1600f, tween(250, easing = FastOutSlowInEasing))
                                        onSortAction(SortStatus.KEEP)
                                        offsetY.snapTo(0f)
                                        offsetX.snapTo(0f)
                                    }
                                    // Vertical DOWN: Delete
                                    y > verticalThresholdPx && abs(y) > abs(x) -> {
                                        offsetY.animateTo(1600f, tween(250, easing = FastOutSlowInEasing))
                                        onSortAction(SortStatus.DELETE)
                                        offsetY.snapTo(0f)
                                        offsetX.snapTo(0f)
                                    }
                                    // Horizontal LEFT: Categorize Later (skip forward)
                                    x < -horizontalThresholdPx -> {
                                        offsetX.animateTo(-stridePx, tween(250, easing = FastOutSlowInEasing))
                                        onSwipeLeft()
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                    }
                                    // Horizontal RIGHT: Previous image (re-categorize)
                                    x > horizontalThresholdPx -> {
                                        offsetX.animateTo(stridePx, tween(250, easing = FastOutSlowInEasing))
                                        onSwipeRight()
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                    }
                                    else -> {
                                        // Spring back smoothly to center
                                        launch {
                                            offsetX.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                        launch {
                                            offsetY.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring())
                                offsetY.animateTo(0f, spring())
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. LEFT CARD (Previous Frame - Peeking from left)
            if (prevImage != null) {
                val leftCardOffsetXPx = -stridePx + offsetX.value
                val leftCardProgress = ((offsetX.value) / stridePx).coerceIn(0f, 1f)
                val leftScale = 0.92f + (leftCardProgress * 0.08f)
                val leftAlpha = 0.40f + (leftCardProgress * 0.60f)

                FilmFrameCard(
                    item = prevImage,
                    onPlayVideo = {},
                    isInteractive = false,
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .offset { IntOffset(leftCardOffsetXPx.roundToInt(), 0) }
                        .scale(leftScale)
                        .alpha(leftAlpha)
                )
            }

            // 2. RIGHT CARD (Next Frame - Peeking from right)
            if (nextImage != null) {
                val rightCardOffsetXPx = stridePx + offsetX.value
                val rightCardProgress = ((-offsetX.value) / stridePx).coerceIn(0f, 1f)
                val rightScale = 0.92f + (rightCardProgress * 0.08f)
                val rightAlpha = 0.40f + (rightCardProgress * 0.60f)

                FilmFrameCard(
                    item = nextImage,
                    onPlayVideo = {},
                    isInteractive = false,
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .offset { IntOffset(rightCardOffsetXPx.roundToInt(), 0) }
                        .scale(rightScale)
                        .alpha(rightAlpha)
                )
            }

            // 3. CENTER CARD (Active Frame)
            val centerHorizontalDrag = abs(offsetX.value) / stridePx
            val centerScale = (1.0f - (centerHorizontalDrag * 0.06f)).coerceIn(0.94f, 1.0f)

            FilmFrameCard(
                item = currentImage,
                onPlayVideo = { onPlayVideo(currentImage) },
                onShareMedia = { onShareMedia?.invoke(currentImage) },
                isInteractive = true,
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .scale(centerScale)
            )

            // 4. OVERLAY BADGES (Up = Keep, Down = Delete, Left = Later, Right = Previous)
            when (dragHint) {
                "KEEP" -> {
                    val alpha = (abs(offsetY.value) / 120f).coerceIn(0.3f, 1f)
                    Surface(
                        color = KeepGreen.copy(alpha = alpha),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("KEEP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp)
                        }
                    }
                }
                "DELETE" -> {
                    val alpha = (abs(offsetY.value) / 120f).coerceIn(0.3f, 1f)
                    Surface(
                        color = DeleteRed.copy(alpha = alpha),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DELETE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp)
                        }
                    }
                }
                "LATER" -> {
                    val alpha = (abs(offsetX.value) / 100f).coerceIn(0.3f, 1f)
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Later", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                "PREVIOUS" -> {
                    val alpha = (abs(offsetX.value) / 100f).coerceIn(0.3f, 1f)
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Previous", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual film-frame card displaying photo or video.
 * Uses ContentScale.Fit with deep dark background so 100% of the media is visible at full size,
 * with tap-to-toggle full crop mode.
 */
@Composable
private fun FilmFrameCard(
    item: ImageItem,
    onPlayVideo: () -> Unit,
    onShareMedia: (() -> Unit)? = null,
    isInteractive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCropMode by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F0F12)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInteractive) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F12))
        ) {
            // Media Image / Video Frame
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.contentUri)
                    .crossfade(true)
                    .build(),
                contentDescription = item.displayName,
                contentScale = if (isCropMode) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = isInteractive && !item.isVideo) {
                        isCropMode = !isCropMode
                    }
            )

            // Top Subtle Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
            )

            // Bottom Subtle Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            // Top Info Header: Folder Chip & Later badge on left, Video duration, Fit/Fill toggle & Share on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Folder / Bucket
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = item.bucketName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    // Later Queue Indicator
                    if (item.status == SortStatus.LATER) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFFB300).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⏳ Later",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Video Duration Badge
                    if (item.isVideo) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                if (item.formattedDuration.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.formattedDuration,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Fit vs Fill Scale Toggle
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { isCropMode = !isCropMode }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Toggle Aspect Ratio",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCropMode) "Fill" else "Fit",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // CENTER: Prominent Glassmorphic Play Button for Videos
            if (item.isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                        .clickable(enabled = isInteractive) { onPlayVideo() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            // Bottom Badges: Name, Size, Date and prominent thumb-reachable Share Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.formattedSize,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " · ",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = item.formattedDate,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
