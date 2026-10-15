package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import com.example.ui.theme.SoftBorderGrey
import com.example.ui.util.HapticFeedbackHelper

/**
 * Filter bar on the Tasks screen displaying color-coded category tags.
 * Provides active category selection, count indicators, category color dots,
 * and quick-add functionality for custom categories.
 */
@Composable
fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categoryCounts: Map<String, Int> = emptyMap(),
    onAddCustomCategory: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("category_filter_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)
            val isAll = category.equals("All", ignoreCase = true)
            val style = if (!isAll) CategoryTheme.getStyle(category) else null
            val count = categoryCounts[category] ?: 0

            val backgroundColor by animateColorAsState(
                targetValue = when {
                    isSelected -> ObsidianBlack
                    style != null -> style.backgroundColor.copy(alpha = 0.85f)
                    else -> PaperSurface
                },
                animationSpec = spring(),
                label = "chipBg"
            )

            val textColor by animateColorAsState(
                targetValue = when {
                    isSelected -> PaperSurface
                    style != null -> style.textColor
                    else -> SlateGraphite
                },
                animationSpec = spring(),
                label = "chipText"
            )

            val borderColor = when {
                isSelected -> ObsidianBlack
                style != null -> style.borderColor
                else -> HairlineBorder
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable {
                        HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                        onCategorySelected(category)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("category_chip_${category.lowercase().replace(" ", "_").removePrefix("#")}"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Color-coded category indicator dot
                    if (style != null) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(style.accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Category label
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )

                    // Count badge (if available and positive)
                    if (count > 0 || (isAll && count > 0)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF2E2E2E) else if (style != null) style.backgroundColor else Color(0xFFECEBE5))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PaperSurface else if (style != null) style.textColor else SlateGraphite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick button to add or filter by a new custom category
        if (onAddCustomCategory != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                        showAddDialog = true
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("add_custom_category_chip"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category",
                        tint = SlateGraphite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Modal Dialog to create a custom category
    if (showAddDialog && onAddCustomCategory != null) {
        val previewStyle = if (newCategoryInput.isNotBlank()) CategoryTheme.getStyle(newCategoryInput) else null

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newCategoryInput = ""
            },
            title = {
                Text(
                    text = "New Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = "Enter a category name. MonoDo will automatically generate an accent color tag for it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGraphite
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        placeholder = { Text("e.g. Health, Finance, Study...", color = SlateGraphite) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_category_input"),
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

                    // Live preview of generated color tag
                    if (previewStyle != null) {
                        Spacer(modifier = Modifier.size(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Preview: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGraphite
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CategoryTag(category = newCategoryInput.trim())
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newCategoryInput.trim()
                        if (clean.isNotBlank()) {
                            onAddCustomCategory(clean)
                            onCategorySelected(clean)
                            newCategoryInput = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newCategoryInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianBlack,
                        contentColor = PaperSurface
                    ),
                    modifier = Modifier.testTag("confirm_add_category_btn")
                ) {
                    Text("Add & Filter")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newCategoryInput = ""
                }) {
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface
        )
    }
}
