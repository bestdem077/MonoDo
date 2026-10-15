package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SlateGraphite
import com.example.ui.theme.SoftBorderGrey
import com.example.ui.util.HapticFeedbackHelper

/**
 * Bottom sheet editor for creating a new note or updating an existing note.
 * Includes note title, multi-line content, color category selector, and pin toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    sheetState: SheetState,
    noteToEdit: NoteEntity? = null,
    onDismiss: () -> Unit,
    onSaveNote: (title: String, content: String, category: String, isPinned: Boolean) -> Unit,
    onDeleteNote: ((NoteEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
    var selectedCategory by remember { mutableStateOf(noteToEdit?.category ?: "Personal") }
    var isPinned by remember { mutableStateOf(noteToEdit?.isPinned ?: false) }

    val categories = listOf("Work", "Personal", "Health", "Focus", "Ideas", "General")

    val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    val charCount = content.length

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaperSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp)
        ) {
            // Header: Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (noteToEdit != null) "Edit Note" else "New Note",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (noteToEdit != null && onDeleteNote != null) {
                        IconButton(
                            onClick = {
                                HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                                onDeleteNote(noteToEdit)
                                onDismiss()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Note",
                                tint = SlateGraphite
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateGraphite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Note Title") },
                placeholder = { Text("Enter a title (e.g. Weekly Review)...", color = SlateGraphite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = ObsidianBlack, fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ObsidianBlack,
                    unfocusedTextColor = ObsidianBlack,
                    cursorColor = ObsidianBlack,
                    focusedBorderColor = SoftBorderGrey,
                    unfocusedBorderColor = HairlineBorder,
                    focusedLabelColor = ObsidianBlack,
                    unfocusedLabelColor = SlateGraphite,
                    focusedContainerColor = FocusedBackground,
                    unfocusedContainerColor = PaperCanvas
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection Row
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { cat ->
                    val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                    val style = CategoryTheme.getStyle(cat)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) ObsidianBlack else style.backgroundColor)
                            .border(
                                1.dp,
                                if (isSelected) ObsidianBlack else style.borderColor,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                selectedCategory = cat
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("note_category_${cat.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(style.accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PaperSurface else style.textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Multi-line Input
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                placeholder = {
                    Text(
                        "Write your thoughts, checklists, meeting memos, or ideas here. Stored privately on device...",
                        color = SlateGraphite
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("note_content_input"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = ObsidianBlack),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ObsidianBlack,
                    unfocusedTextColor = ObsidianBlack,
                    cursorColor = ObsidianBlack,
                    focusedBorderColor = SoftBorderGrey,
                    unfocusedBorderColor = HairlineBorder,
                    focusedLabelColor = ObsidianBlack,
                    unfocusedLabelColor = SlateGraphite,
                    focusedContainerColor = FocusedBackground,
                    unfocusedContainerColor = PaperCanvas
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Counters Row
            Text(
                text = "$wordCount words • $charCount characters",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pin to Top Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin Note",
                        tint = if (isPinned) ObsidianBlack else SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pin to Top",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Keep this note at the top of your list",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGraphite,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isPinned,
                    onCheckedChange = { isPinned = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PaperSurface,
                        checkedTrackColor = RoyalBlue,
                        uncheckedThumbColor = SlateGraphite,
                        uncheckedTrackColor = PaperCanvas
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Note Button
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                        onSaveNote(title, content, selectedCategory, isPinned)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() || content.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_note_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = PaperSurface,
                    disabledContainerColor = Color(0xFFD6D6D0),
                    disabledContentColor = Color(0xFF8E8E8A)
                )
            ) {
                Text(
                    text = if (noteToEdit != null) "Update Note" else "Create Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
