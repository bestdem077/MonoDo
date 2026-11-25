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
import androidx.compose.material.icons.filled.AddTask
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    sheetState: SheetState,
    noteToEdit: NoteEntity? = null,
    onDismiss: () -> Unit,
    onSaveNote: (title: String, content: String, category: String, isPinned: Boolean) -> Unit,
    onDeleteNote: ((NoteEntity) -> Unit)? = null,
    onConvertToTask: ((NoteEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var title by remember(noteToEdit?.id) { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember(noteToEdit?.id) { mutableStateOf(noteToEdit?.content ?: "") }
    var selectedCategory by remember(noteToEdit?.id) { mutableStateOf(noteToEdit?.category ?: "Personal") }
    var isPinned by remember(noteToEdit?.id) { mutableStateOf(noteToEdit?.isPinned ?: false) }

    val categories = listOf("Work", "Personal", "Health", "Focus", "Ideas", "General")

    val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    val charCount = content.length

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(34.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.softBorder)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (noteToEdit != null) "Edit Note" else "New Note",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (noteToEdit != null && onConvertToTask != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.accentSoft)
                                .border(1.dp, colors.accentBorder, RoundedCornerShape(999.dp))
                                .clickable {
                                    HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                                    onConvertToTask(
                                        noteToEdit.copy(
                                            title = title,
                                            content = content,
                                            category = selectedCategory
                                        )
                                    )
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("note_convert_to_task_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddTask,
                                    contentDescription = "Convert to Task",
                                    tint = colors.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "To Task",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (noteToEdit != null && onDeleteNote != null) {
                        IconButton(
                            onClick = {
                                HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                                onDeleteNote(noteToEdit)
                                onDismiss()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Note",
                                tint = colors.danger
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.secondaryInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CompactFormTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Note title (e.g. Weekly Review)...",
                singleLine = true,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
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
                    val style = CategoryTheme.getStyle(cat, colors.isDark)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.primaryInk else style.backgroundColor)
                            .border(
                                1.dp,
                                if (isSelected) colors.primaryInk else style.borderColor,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) colors.canvas else style.accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) colors.canvas else style.textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CompactFormTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = "Write your thoughts, meeting notes, or checklists...",
                singleLine = false,
                minLines = 6,
                maxLines = 12,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$wordCount words • $charCount chars",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = colors.secondaryInk,
                fontSize = 10.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Pin Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.canvas)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin Note",
                        tint = if (isPinned) colors.accent else colors.secondaryInk,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pin to Top",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.primaryInk,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Keep this note pinned above other notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isPinned,
                    onCheckedChange = { isPinned = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.secondaryInk,
                        uncheckedTrackColor = colors.tileBg
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                    .height(50.dp)
                    .testTag("save_note_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    disabledContainerColor = colors.softBorder,
                    disabledContentColor = colors.secondaryInk
                )
            ) {
                Text(
                    text = if (noteToEdit != null) "Update Note" else "Create Note",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
