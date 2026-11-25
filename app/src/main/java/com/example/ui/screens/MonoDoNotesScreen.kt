package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.components.CompactFormTextField
import com.example.ui.components.NoteEditorSheet
import com.example.ui.components.SwissEmptyArtType
import com.example.ui.components.SwissEmptyStateArt
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoDoNotesScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val noteSearchQuery by viewModel.noteSearchQuery.collectAsState()
    val noteSelectedCategory by viewModel.noteSelectedCategory.collectAsState()

    var showNoteSheet by remember { mutableStateOf(false) }
    var selectedNoteForEdit by remember { mutableStateOf<NoteEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val noteCategories = listOf("All", "Work", "Personal", "Health", "Focus", "Ideas", "General")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .testTag("mono_do_notes_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Compact Search box
            CompactFormTextField(
                value = noteSearchQuery,
                onValueChange = { viewModel.setNoteSearchQuery(it) },
                placeholder = "Search notes...",
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Note Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                noteCategories.forEach { cat ->
                    val isSelected = noteSelectedCategory.equals(cat, ignoreCase = true)
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
                                viewModel.setNoteSelectedCategory(cat)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("notes_filter_${cat.lowercase()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!cat.equals("All", ignoreCase = true)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.canvas else style.accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
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

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        SwissEmptyStateArt(
                            type = SwissEmptyArtType.NOTES,
                            size = 88.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (noteSearchQuery.isBlank() && noteSelectedCategory.equals("All", ignoreCase = true)) {
                                "Your notebook is blank"
                            } else {
                                "No matching notes"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryInk,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (noteSearchQuery.isBlank() && noteSelectedCategory.equals("All", ignoreCase = true)) {
                                "Tap + to capture ideas, checklists, or quick thoughts offline."
                            } else {
                                "Try clearing your search or switching note categories."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryInk,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        Box(modifier = Modifier.animateItem()) {
                            NoteCardItem(
                                note = note,
                                onTogglePin = { viewModel.togglePinNote(note) },
                                onConvertToTask = { viewModel.convertNoteToTask(note) },
                                onDelete = { viewModel.deleteNote(note) },
                                onClick = {
                                    selectedNoteForEdit = note
                                    showNoteSheet = true
                                }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }

        // Add Note FAB
        FloatingActionButton(
            onClick = {
                selectedNoteForEdit = null
                showNoteSheet = true
            },
            containerColor = colors.accent,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .size(54.dp)
                .testTag("add_note_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Note")
        }

        // Upgraded NoteEditorSheet (Create / Edit + Category + Pin + Word Count + 1-Tap Note-to-Task)
        if (showNoteSheet) {
            NoteEditorSheet(
                sheetState = sheetState,
                noteToEdit = selectedNoteForEdit,
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showNoteSheet = false
                        selectedNoteForEdit = null
                    }
                },
                onSaveNote = { title, content, category, isPinned ->
                    val existing = selectedNoteForEdit
                    if (existing != null) {
                        viewModel.updateNote(
                            existing.copy(
                                title = title.trim(),
                                content = content.trim(),
                                category = category,
                                isPinned = isPinned
                            )
                        )
                    } else {
                        viewModel.createNote(
                            title = title,
                            content = content,
                            category = category,
                            isPinned = isPinned
                        )
                    }
                },
                onDeleteNote = { note -> viewModel.deleteNote(note) },
                onConvertToTask = { note -> viewModel.convertNoteToTask(note) }
            )
        }
    }
}

@Composable
private fun NoteCardItem(
    note: NoteEntity,
    onTogglePin: () -> Unit,
    onConvertToTask: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = sdf.format(Date(note.updatedAt))
    val catStyle = CategoryTheme.getStyle(note.category.ifBlank { "General" }, colors.isDark)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(
                width = 1.dp,
                color = if (note.isPinned) colors.accent else colors.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryInk,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                            onConvertToTask()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddTask,
                            contentDescription = "Convert Note to Task",
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            onTogglePin()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (note.isPinned) colors.accent else colors.secondaryInk,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                            onDelete()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = colors.secondaryInk,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryInk,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(catStyle.backgroundColor)
                        .border(1.dp, catStyle.borderColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = note.category.ifBlank { "General" },
                        style = MaterialTheme.typography.labelSmall,
                        color = catStyle.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = colors.secondaryInk,
                    fontSize = 10.sp
                )
            }
        }
    }
}
