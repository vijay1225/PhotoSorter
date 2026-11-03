package com.example.photosort.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photosort.data.model.SortStatus
import com.example.photosort.ui.theme.DeleteRed
import com.example.photosort.ui.theme.KeepGreen
import com.example.photosort.ui.theme.OnDeleteRed
import com.example.photosort.ui.theme.OnKeepGreen
import com.example.photosort.ui.theme.UndoBlue
import com.example.photosort.ui.theme.UndoBlueContainer

/**
 * Sleek, ultra-compact thumb action bar (~52dp height) matching gestures:
 *  - Prev (Move back / re-categorize)
 *  - Delete (Down swipe shortcut)
 *  - Undo
 *  - Share (Native Android share sheet before deleting/sorting)
 *  - Keep (Up swipe shortcut)
 *  - Later (Move forward / skip)
 */
@Composable
fun ActionButtonsBar(
    canUndo: Boolean,
    onUndoClick: () -> Unit,
    onSortAction: (SortStatus) -> Unit,
    onMoveAction: (Int) -> Unit,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // MOVE PREVIOUS (RIGHT SWIPE SHORTCUT)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                onClick = { onMoveAction(-1) },
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous",
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text("Prev", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }

        // DELETE (DOWN SWIPE SHORTCUT)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledIconButton(
                onClick = { onSortAction(SortStatus.DELETE) },
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = DeleteRed,
                    contentColor = OnDeleteRed
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Delete",
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text("Delete", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeleteRed)
        }

        // UNDO
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                onClick = onUndoClick,
                enabled = canUndo,
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = UndoBlueContainer,
                    contentColor = UndoBlue
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "Undo",
                fontSize = 10.sp,
                color = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
        }

        // SHARE (Native share sheet before deleting/sorting)
        if (onShareClick != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "Share",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // KEEP (UP SWIPE SHORTCUT)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledIconButton(
                onClick = { onSortAction(SortStatus.KEEP) },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = KeepGreen,
                    contentColor = OnKeepGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Keep",
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text("Keep", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KeepGreen)
        }

        // MOVE NEXT (LEFT SWIPE SHORTCUT - LATER)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                onClick = { onMoveAction(1) },
                modifier = Modifier.size(42.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text("Later", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
