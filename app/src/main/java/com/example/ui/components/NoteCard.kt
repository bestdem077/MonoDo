package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-fidelity card representing an individual note in the Notes application.
 * Displays title, multiline content preview, color-coded category tag,
 * pin badge, last updated timestamp, and quick actions (copy, pin, delete).
 */
@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(note.updatedAt))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PaperSurface)
            .border(
                width = if (note.isPinned) 1.5.dp else 1.dp,
                color = if (note.isPinned) ObsidianBlack else HairlineBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("note_card_${note.id}")
    ) {
        // Top row: Category tag, Pin badge, and Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Color-coded category tag
                CategoryTag(
                    category = note.category.ifBlank { "General" },
                    fontSize = 10.sp,
                    dotSize = 6.dp
                )

                if (note.isPinned) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RoyalBlue)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned Note",
                            tint = PaperSurface,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "PINNED",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaperSurface,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick actions row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pin / Unpin Button
                IconButton(
                    onClick = {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        onTogglePin()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                        tint = if (note.isPinned) RoyalBlue else SlateGraphite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Copy note content
                IconButton(
                    onClick = {
                        val textToCopy = buildString {
                            if (note.title.isNotBlank()) appendLine(note.title)
                            append(note.content)
                        }
                        if (textToCopy.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(note.title.ifBlank { "Note" }, textToCopy))
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            Toast.makeText(context, "Note copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Note",
                        tint = SlateGraphite,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Delete note
                IconButton(
                    onClick = {
                        HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                        onDelete()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Note",
                        tint = SlateGraphite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Note Title
        if (note.title.isNotBlank()) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                color = ObsidianBlack,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Note Content snippet
        if (note.content.isNotBlank()) {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                color = SlateGraphite,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Timestamp
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA0A09B),
            fontSize = 10.sp
        )
    }
}
