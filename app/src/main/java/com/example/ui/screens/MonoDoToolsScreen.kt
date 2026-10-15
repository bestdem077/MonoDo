package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteEntity
import com.example.ui.components.CategoryTag
import com.example.ui.components.NoteCard
import com.example.ui.components.NoteEditorSheet
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import com.example.ui.theme.SoftBorderGrey
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class ToolSubTab {
    FOCUS_TIMER,
    NOTES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoDoToolsScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedTool by remember { mutableStateOf(ToolSubTab.FOCUS_TIMER) }

    // Focus Timer States (25m default)
    var selectedDurationMinutes by remember { mutableIntStateOf(25) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var completedSessions by remember { mutableIntStateOf(0) }

    // Countdown Coroutine
    LaunchedEffect(isTimerRunning, remainingSeconds) {
        if (isTimerRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        } else if (isTimerRunning && remainingSeconds == 0) {
            isTimerRunning = false
            completedSessions += 1
            HapticFeedbackHelper.performTaskCompletionHaptic(view, haptic)
            Toast.makeText(context, "Focus session complete! Take a breather.", Toast.LENGTH_LONG).show()
        }
    }

    // Notes State directly observed from Room DAO
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    var noteSearchQuery by remember { mutableStateOf("") }
    var selectedNoteCategory by remember { mutableStateOf("All") }

    // Note Editor Sheet states
    var isNoteEditorOpen by remember { mutableStateOf(false) }
    var noteBeingEdited by remember { mutableStateOf<NoteEntity?>(null) }
    val noteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val noteCategories = listOf("All", "Work", "Personal", "Health", "Focus", "Ideas", "General")

    // Filter notes by search query and category
    val filteredNotes = allNotes.filter { note ->
        val matchesCategory = selectedNoteCategory == "All" || note.category.equals(selectedNoteCategory, ignoreCase = true)
        val query = noteSearchQuery.trim().lowercase()
        val matchesQuery = query.isBlank() ||
                note.title.lowercase().contains(query) ||
                note.content.lowercase().contains(query) ||
                note.category.lowercase().contains(query)
        matchesCategory && matchesQuery
    }

    val pinnedNotes = filteredNotes.filter { it.isPinned }
    val unpinnedNotes = filteredNotes.filter { !it.isPinned }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Title: "Tools" as requested by user
        item {
            Column {
                Text(
                    text = "UTILITIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tools",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // Tool Selector Pills: Focus Timer & Notes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Focus Timer Tab Button
                val isTimer = selectedTool == ToolSubTab.FOCUS_TIMER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTimer) ObsidianBlack else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = PaperSurface)
                        ) {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            selectedTool = ToolSubTab.FOCUS_TIMER
                        }
                        .padding(vertical = 12.dp)
                        .testTag("tab_focus_timer"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Focus Timer",
                            tint = if (isTimer) PaperSurface else SlateGraphite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Focus Timer",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isTimer) PaperSurface else SlateGraphite,
                            fontWeight = if (isTimer) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Notes Tab Button (replaces scratch pad)
                val isNotes = selectedTool == ToolSubTab.NOTES
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isNotes) ObsidianBlack else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = PaperSurface)
                        ) {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            selectedTool = ToolSubTab.NOTES
                        }
                        .padding(vertical = 12.dp)
                        .testTag("tab_notes"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Notes",
                            tint = if (isNotes) PaperSurface else SlateGraphite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isNotes) PaperSurface else SlateGraphite,
                            fontWeight = if (isNotes) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ==========================================
        // SubTab 1: Focus Pomodoro Timer
        // ==========================================
        if (selectedTool == ToolSubTab.FOCUS_TIMER) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(PaperSurface)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode selector (25m Focus / 5m Break / 15m Long Break)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf(25 to "25m Focus", 5 to "5m Break", 15 to "15m Break").forEach { (mins, label) ->
                            val isSelected = selectedDurationMinutes == mins
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) ObsidianBlack else DateTileBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) ObsidianBlack else HairlineBorder,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                        selectedDurationMinutes = mins
                                        remainingSeconds = mins * 60
                                        isTimerRunning = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) PaperSurface else ObsidianBlack,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Circular Animated Timer
                    val totalDurationSec = selectedDurationMinutes * 60
                    val timerProgress = if (totalDurationSec > 0) {
                        remainingSeconds.toFloat() / totalDurationSec.toFloat()
                    } else 0f

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(210.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { timerProgress },
                            modifier = Modifier.size(200.dp),
                            color = if (isTimerRunning) RoyalBlue else ObsidianBlack,
                            trackColor = if (isTimerRunning) RoyalBlueSoft else DateTileBackground,
                            strokeWidth = 10.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val minutes = remainingSeconds / 60
                            val seconds = remainingSeconds % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.displayMedium,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isTimerRunning) "In Deep Focus" else "Paused",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGraphite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Timer Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Reset Button
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(DateTileBackground)
                                .border(1.dp, HairlineBorder, CircleShape)
                                .clickable {
                                    HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                                    isTimerRunning = false
                                    remainingSeconds = selectedDurationMinutes * 60
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Timer",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Play / Pause Main Button
                        Button(
                            onClick = {
                                HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                                isTimerRunning = !isTimerRunning
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTimerRunning) ObsidianBlack else RoyalBlue,
                                contentColor = PaperSurface
                            )
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pause" else "Start",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isTimerRunning) "Pause Focus" else "Start Focus",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Daily streak indicator badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DateTileBackground)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Completed Sessions",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$completedSessions session${if (completedSessions != 1) "s" else ""} completed today",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ==========================================
        // SubTab 2: Notes Application (Replaces Scratchpad)
        // ==========================================
        if (selectedTool == ToolSubTab.NOTES) {
            // Search field & + New Note button row
            item(key = "notes_header_controls") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search bar
                    OutlinedTextField(
                        value = noteSearchQuery,
                        onValueChange = { noteSearchQuery = it },
                        placeholder = {
                            Text("Search notes by title or content...", style = MaterialTheme.typography.bodySmall, color = SlateGraphite)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Notes",
                                tint = SlateGraphite,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = noteSearchQuery.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(
                                    onClick = { noteSearchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = SlateGraphite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("notes_search_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = ObsidianBlack),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ObsidianBlack,
                            unfocusedTextColor = ObsidianBlack,
                            cursorColor = ObsidianBlack,
                            focusedBorderColor = SoftBorderGrey,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = FocusedBackground,
                            unfocusedContainerColor = PaperSurface
                        )
                    )

                    // Color-coded category filter pills for notes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        noteCategories.forEach { cat ->
                            val isSelected = cat.equals(selectedNoteCategory, ignoreCase = true)
                            val isAll = cat == "All"
                            val style = if (!isAll) CategoryTheme.getStyle(cat) else null

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) ObsidianBlack else if (style != null) style.backgroundColor else PaperSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) ObsidianBlack else if (style != null) style.borderColor else HairlineBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                        selectedNoteCategory = cat
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("note_filter_${cat.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (style != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(style.accentColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) PaperSurface else if (style != null) style.textColor else SlateGraphite,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Section title + "+ New Note" action button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOTES (${filteredNotes.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGraphite,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Button(
                            onClick = {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                noteBeingEdited = null
                                isNoteEditorOpen = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = PaperSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("create_new_note_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Note",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Note",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Empty state if no notes found
            if (filteredNotes.isEmpty()) {
                item(key = "empty_notes_view") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PaperSurface)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Empty Notes",
                            tint = Color(0xFFC0C0B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (noteSearchQuery.isNotBlank() || selectedNoteCategory != "All") "No matching notes found" else "No notes yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (noteSearchQuery.isNotBlank() || selectedNoteCategory != "All") "Try adjusting your search query or category filter." else "Tap \"New Note\" above to organize your thoughts and memos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGraphite,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Pinned Notes section
            if (pinnedNotes.isNotEmpty()) {
                item(key = "pinned_notes_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PINNED",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                items(
                    items = pinnedNotes,
                    key = { "pinned_${it.id}" }
                ) { note ->
                    NoteCard(
                        note = note,
                        onClick = {
                            noteBeingEdited = note
                            isNoteEditorOpen = true
                        },
                        onTogglePin = { viewModel.togglePinNote(note) },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }

            // Other Unpinned Notes section
            if (unpinnedNotes.isNotEmpty()) {
                if (pinnedNotes.isNotEmpty()) {
                    item(key = "other_notes_header") {
                        Text(
                            text = "OTHER NOTES",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGraphite,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                }

                items(
                    items = unpinnedNotes,
                    key = { "unpinned_${it.id}" }
                ) { note ->
                    NoteCard(
                        note = note,
                        onClick = {
                            noteBeingEdited = note
                            isNoteEditorOpen = true
                        },
                        onTogglePin = { viewModel.togglePinNote(note) },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
    }

    // Modal Note Editor Sheet (Add or Edit)
    if (isNoteEditorOpen) {
        NoteEditorSheet(
            sheetState = noteSheetState,
            noteToEdit = noteBeingEdited,
            onDismiss = {
                scope.launch {
                    noteSheetState.hide()
                    isNoteEditorOpen = false
                    noteBeingEdited = null
                }
            },
            onSaveNote = { title, content, category, isPinned ->
                val current = noteBeingEdited
                if (current != null) {
                    viewModel.updateNote(
                        current.copy(
                            title = title,
                            content = content,
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
                scope.launch {
                    noteSheetState.hide()
                    isNoteEditorOpen = false
                    noteBeingEdited = null
                }
            },
            onDeleteNote = { noteToDelete ->
                viewModel.deleteNote(noteToDelete)
                scope.launch {
                    noteSheetState.hide()
                    isNoteEditorOpen = false
                    noteBeingEdited = null
                }
            }
        )
    }
}
