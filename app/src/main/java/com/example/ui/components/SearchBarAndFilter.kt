package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskPriority
import com.example.data.model.TaskSortOrder
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarAndFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedPriority: TaskPriority?,
    onPrioritySelected: (TaskPriority?) -> Unit,
    selectedSortOrder: TaskSortOrder = TaskSortOrder.DEFAULT,
    onSortOrderSelected: (TaskSortOrder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var isFilterMenuOpen by remember { mutableStateOf(false) }
    var isSortSheetOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search tasks...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateGraphite
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(RoyalBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_tasks_input")
                        )
                    }

                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = { onQueryChange("") },
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
                }
            }

            // Priority Filter Icon Button with badge
            Box {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedPriority != null) ObsidianBlack else PaperCanvas)
                        .border(
                            1.dp,
                            if (selectedPriority != null) ObsidianBlack else HairlineBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            isFilterMenuOpen = true
                        }
                        .testTag("priority_filter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter Priority",
                        tint = if (selectedPriority != null) PaperSurface else SlateGraphite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = isFilterMenuOpen,
                    onDismissRequest = { isFilterMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "All Priorities",
                                fontWeight = if (selectedPriority == null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onPrioritySelected(null)
                            isFilterMenuOpen = false
                        }
                    )
                    TaskPriority.entries.forEach { priority ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = priority.indicatorSymbol,
                                        color = priority.monochromeIndicatorColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = priority.label,
                                        fontWeight = if (selectedPriority == priority) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                onPrioritySelected(priority)
                                isFilterMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Smart Sorting Toolbar Button -> Opens Smooth Minimalist Toggle Sheet
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedSortOrder != TaskSortOrder.DEFAULT) RoyalBlue else PaperCanvas)
                    .border(
                        1.dp,
                        if (selectedSortOrder != TaskSortOrder.DEFAULT) RoyalBlue else HairlineBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        isSortSheetOpen = true
                    }
                    .testTag("sort_tasks_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort Tasks",
                    tint = if (selectedSortOrder != TaskSortOrder.DEFAULT) Color.White else SlateGraphite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Active Sort / Priority Filter Indicator Row
        if (selectedSortOrder != TaskSortOrder.DEFAULT || selectedPriority != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedSortOrder != TaskSortOrder.DEFAULT) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(RoyalBlueSoft)
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(999.dp))
                            .clickable { onSortOrderSelected(TaskSortOrder.DEFAULT) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sort: ${selectedSortOrder.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reset Sort",
                                tint = RoyalBlue,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                if (selectedPriority != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ObsidianBlack)
                            .clickable { onPrioritySelected(null) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Priority: ${selectedPriority.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reset Priority",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Minimalist Smart Sorting & Filtering Toggle Sheet
    if (isSortSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { isSortSheetOpen = false },
            sheetState = sheetState,
            containerColor = PaperSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .testTag("smart_sort_bottom_sheet")
            ) {
                Text(
                    text = "Smart Sort & Filter",
                    style = MaterialTheme.typography.titleLarge,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Arrange tasks by priority, urgency, or creation order",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGraphite
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SORT BY",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskSortOrder.entries.forEach { order ->
                        val isSelected = selectedSortOrder == order
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) RoyalBlueSoft else PaperCanvas)
                                .border(
                                    1.dp,
                                    if (isSelected) RoyalBlue else HairlineBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    onSortOrderSelected(order)
                                    isSortSheetOpen = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("sort_option_${order.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = order.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) RoyalBlue else ObsidianBlack,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(RoyalBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

