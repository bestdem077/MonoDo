package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskPriority
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    availableCategories: List<String> = listOf("Work", "Personal", "Health", "Focus", "Finance", "General"),
    onAddCustomCategory: ((name: String, colorHex: String) -> Unit)? = null,
    onCreateTask: (
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Long?,
        subtasks: List<String>
    ) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Work") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueDate by remember {
        mutableStateOf<Long?>(QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TODAY))
    }
    var newSubtaskInput by remember { mutableStateOf("") }
    val subtasksList = remember { mutableStateListOf<String>() }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedCategoryColorHex by remember { mutableStateOf("#2563EB") }

    val categories = remember(availableCategories) {
        val filtered = availableCategories.filter { !it.equals("All", ignoreCase = true) }
        if (filtered.isEmpty()) listOf("Work", "Personal", "Health", "Focus", "Finance", "General") else filtered
    }

    val submitTaskNow = {
        if (title.isNotBlank()) {
            HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
            onCreateTask(
                title.trim(),
                description.trim(),
                selectedCategory,
                selectedPriority,
                dueDate,
                subtasksList.toList()
            )
            onDismiss()
        }
    }

    val showDatePicker = {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }
                dueDate = chosen.timeInMillis
                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaperSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(34.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD4D4CE))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Task",
                    style = MaterialTheme.typography.titleLarge,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )

                // Compact Quick-Save Pill in Header for 1-Tap Instant Creation
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (title.isNotBlank()) RoyalBlue else PaperCanvas)
                        .border(
                            1.dp,
                            if (title.isNotBlank()) RoyalBlue else HairlineBorder,
                            RoundedCornerShape(999.dp)
                        )
                        .clickable(enabled = title.isNotBlank()) { submitTaskNow() }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                        .testTag("quick_instant_save_task_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Quick Add",
                            tint = if (title.isNotBlank()) Color.White else SlateGraphite,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Quick Save",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (title.isNotBlank()) Color.White else SlateGraphite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Compact Task Title Input + Inline 1-Tap Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactFormTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "What needs to be done?",
                    singleLine = true,
                    fontWeight = FontWeight.SemiBold,
                    imeAction = ImeAction.Done,
                    onImeAction = { submitTaskNow() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_task_title_input")
                )

                // Small 1-Tap Instant Add Icon Button right next to Title Input
                IconButton(
                    onClick = { submitTaskNow() },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (title.isNotBlank()) RoyalBlue else PaperCanvas)
                        .border(
                            1.dp,
                            if (title.isNotBlank()) RoyalBlue else HairlineBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .testTag("inline_title_quick_add_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Instant Add Task",
                        tint = if (title.isNotBlank()) Color.White else SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 1-Tap Quick Due Date Chips Directly Below Text Field ("Today", "Tomorrow", "This Weekend", "Next Week")
            QuickDateChipsRow(
                selectedDueDate = dueDate,
                onDueDateChange = { dueDate = it },
                onCustomDateClick = { showDatePicker() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Compact Task Description / Notes (optional) - Slim single-line default height
            CompactFormTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Notes or details (optional)",
                singleLine = false,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_task_notes_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. PRIORITY LEVEL SELECTOR (Low, Medium, High) with subtle monochrome indicators
            Text(
                text = "PRIORITY LEVEL",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                TaskPriority.entries.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val animatedBg by animateColorAsState(
                        targetValue = if (isSelected) priority.monochromeBackgroundColor else Color.Transparent,
                        label = "priorityBg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(animatedBg)
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                selectedPriority = priority
                            }
                            .padding(vertical = 8.dp)
                            .testTag("priority_selector_${priority.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = priority.indicatorSymbol,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) {
                                    if (priority == TaskPriority.HIGH) Color.White else priority.monochromeIndicatorColor
                                } else priority.monochromeIndicatorColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = priority.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) {
                                    if (priority == TaskPriority.HIGH) Color.White else ObsidianBlack
                                } else SlateGraphite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Category Selector with Custom Category Semantic Palette Creator
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
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    val style = CategoryTheme.getStyle(category)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ObsidianBlack else style.backgroundColor)
                            .border(1.dp, if (isSelected) ObsidianBlack else style.borderColor, RoundedCornerShape(20.dp))
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                selectedCategory = category
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("create_category_chip_${category.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else style.accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else style.textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                // + New Custom Category Chip
                if (onAddCustomCategory != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PaperCanvas)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                showAddCategoryDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("sheet_add_custom_category_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom Category",
                                tint = RoyalBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Subtasks Checklist
            Text(
                text = "SUBTASKS (${subtasksList.size})",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            subtasksList.forEachIndexed { index, subtask ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SlateGraphite)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = subtask,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ObsidianBlack
                        )
                    }
                    IconButton(
                        onClick = { subtasksList.removeAt(index) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove subtask",
                            tint = SlateGraphite,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactFormTextField(
                    value = newSubtaskInput,
                    onValueChange = { newSubtaskInput = it },
                    placeholder = "Add subtask item...",
                    singleLine = true,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (newSubtaskInput.isNotBlank()) {
                            subtasksList.add(newSubtaskInput.trim())
                            newSubtaskInput = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newSubtaskInput.isNotBlank()) {
                            subtasksList.add(newSubtaskInput.trim())
                            newSubtaskInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (newSubtaskInput.isNotBlank()) RoyalBlue else PaperCanvas)
                        .border(1.dp, if (newSubtaskInput.isNotBlank()) RoyalBlue else HairlineBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add subtask",
                        tint = if (newSubtaskInput.isNotBlank()) Color.White else SlateGraphite,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Submit Button
            Button(
                onClick = { submitTaskNow() },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(
                        elevation = if (title.isNotBlank()) 5.dp else 0.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = RoyalBlue.copy(alpha = 0.35f)
                    )
                    .testTag("submit_create_task_btn"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFD4D4CE),
                    disabledContentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Create",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create Task",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (showAddCategoryDialog && onAddCustomCategory != null) {
        val paletteOptions = listOf(
            "#2563EB", // Royal Blue
            "#16A34A", // Emerald
            "#D97706", // Amber
            "#DC2626", // Crimson
            "#7C3AED", // Purple
            "#0D9488", // Teal
            "#E11D48", // Rose
            "#374151"  // Charcoal
        )
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryName = ""
            },
            title = {
                Text(
                    text = "Custom Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            },
            text = {
                Column {
                    CompactFormTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = "e.g. College, Side Project, Fitness",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Semantic Palette Accent Dot",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paletteOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedCategoryColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) ObsidianBlack else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedCategoryColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newCategoryName.trim()
                        if (clean.isNotBlank()) {
                            onAddCustomCategory(clean, selectedCategoryColorHex)
                            selectedCategory = clean
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create & Select", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface
        )
    }
}

/**
 * Sleek, compact text input field designed for mobile density (42.dp single-line height,
 * clean 10.dp vertical padding) to prevent oversized boxes in sheets, dialogs, and onboarding.
 */
@Composable
fun CompactFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 3,
    fontWeight: FontWeight = FontWeight.Normal,
    isError: Boolean = false,
    imeAction: ImeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
    onImeAction: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> RoyalBlue
        else -> HairlineBorder
    }
    val containerColor = if (isFocused) FocusedBackground else PaperCanvas

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = ObsidianBlack,
            fontWeight = fontWeight,
            fontSize = 14.sp,
            lineHeight = 19.sp
        ),
        cursorBrush = SolidColor(RoyalBlue),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction?.invoke() },
            onSend = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() }
        ),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SlateGraphite,
                            fontSize = 13.5.sp
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

