package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskPriority
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LightPill
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import com.example.ui.theme.SoftBorderGrey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSaveTask: (
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        priority: TaskPriority,
        dueDate: Long?,
        subtasks: List<String>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Focus") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var selectedDueDate by remember { mutableStateOf<Long?>(null) }
    var newSubtaskText by remember { mutableStateOf("") }
    val subtasksList = remember { mutableStateListOf<String>() }
    var newTagText by remember { mutableStateOf("") }
    val tagsList = remember { mutableStateListOf<String>() }

    val categories = listOf("Focus", "Work", "Personal", "Ideas", "General")

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                }
                selectedDueDate = cal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Task",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SlateGraphite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                placeholder = { Text("What needs to be done?", color = SlateGraphite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ObsidianBlack),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Description / notes
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes (Optional)") },
                placeholder = { Text("Add details, links, or context...", color = SlateGraphite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input"),
                minLines = 3,
                maxLines = 5,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selector
            Text(
                text = "PRIORITY",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { priority ->
                    val isSelected = priority == selectedPriority
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ObsidianBlack else PaperCanvas)
                            .border(
                                1.dp,
                                if (isSelected) ObsidianBlack else HairlineBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedPriority = priority }
                            .padding(vertical = 8.dp)
                            .testTag("priority_option_${priority.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = priority.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) PaperSurface else ObsidianBlack,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) ObsidianBlack else LightPill)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) PaperSurface else ObsidianBlack,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tags Selector / Builder
            Text(
                text = "TAGS",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (tagsList.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tagsList.forEachIndexed { index, tag ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaperCanvas)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = if (tag.startsWith("#")) tag else "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianBlack,
                                fontSize = 11.sp
                            )
                            IconButton(
                                onClick = { tagsList.removeAt(index) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Tag",
                                    tint = SlateGraphite,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    placeholder = { Text("Add tag (e.g. #design, #sprint)...", color = SlateGraphite) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_tag_input"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ObsidianBlack,
                        unfocusedTextColor = ObsidianBlack,
                        cursorColor = ObsidianBlack,
                        focusedBorderColor = SoftBorderGrey,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = FocusedBackground,
                        unfocusedContainerColor = PaperCanvas
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val clean = newTagText.trim()
                        if (clean.isNotBlank() && !tagsList.contains(clean)) {
                            tagsList.add(clean)
                            newTagText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ObsidianBlack)
                        .testTag("add_tag_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Tag",
                        tint = PaperSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Due Date Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .clickable { datePickerDialog.show() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Pick Due Date",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (selectedDueDate != null) {
                            val format = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                            format.format(Date(selectedDueDate!!))
                        } else "Add Due Date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedDueDate != null) ObsidianBlack else SlateGraphite,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (selectedDueDate != null) {
                    IconButton(
                        onClick = { selectedDueDate = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Due Date",
                            tint = SlateGraphite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtask builder
            Text(
                text = "SUBTASKS / CHECKLIST",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Subtask items already added
            subtasksList.forEachIndexed { index, subtask ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.5.dp, SlateGraphite, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtask,
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianBlack,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { subtasksList.removeAt(index) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Subtask",
                            tint = SlateGraphite,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Input to add a new subtask
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add subtask item...", color = SlateGraphite) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("modal_subtask_input"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ObsidianBlack,
                        unfocusedTextColor = ObsidianBlack,
                        cursorColor = ObsidianBlack,
                        focusedBorderColor = SoftBorderGrey,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = FocusedBackground,
                        unfocusedContainerColor = PaperCanvas
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            subtasksList.add(newSubtaskText.trim())
                            newSubtaskText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ObsidianBlack)
                        .testTag("modal_subtask_add_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subtask",
                        tint = PaperSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSaveTask(
                            title,
                            description,
                            selectedCategory,
                            tagsList.toList(),
                            selectedPriority,
                            selectedDueDate,
                            subtasksList.toList()
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianBlack,
                    contentColor = PaperSurface,
                    disabledContainerColor = Color(0xFFD6D6D0),
                    disabledContentColor = Color(0xFF8E8E8A)
                )
            ) {
                Text(
                    text = "Create Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
