package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categoryCounts: Map<String, Int> = emptyMap(),
    onAddCustomCategory: ((name: String, colorHex: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val scrollState = rememberScrollState()
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#2563EB") }

    val paletteOptions = listOf(
        "#2563EB", "#16A34A", "#D97706", "#DC2626",
        "#7C3AED", "#0D9488", "#E11D48", "#4B5563"
    )

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
            val style = if (!isAll) CategoryTheme.getStyle(category, colors.isDark) else null
            val count = categoryCounts[category] ?: 0

            val backgroundColor by animateColorAsState(
                targetValue = when {
                    isSelected -> colors.primaryInk
                    style != null -> style.backgroundColor
                    else -> colors.surface
                },
                animationSpec = spring(),
                label = "chipBg"
            )

            val textColor by animateColorAsState(
                targetValue = when {
                    isSelected -> colors.canvas
                    style != null -> style.textColor
                    else -> colors.secondaryInk
                },
                animationSpec = spring(),
                label = "chipText"
            )

            val borderColor = when {
                isSelected -> colors.primaryInk
                style != null -> style.borderColor
                else -> colors.border
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
                    if (style != null) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.canvas else style.accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )

                    if (count > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) colors.canvas.copy(alpha = 0.18f)
                                    else colors.tileBg
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = if (isSelected) colors.canvas else colors.secondaryInk,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (onAddCustomCategory != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.canvas)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .clickable {
                        HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                        showAddDialog = true
                    }
                    .padding(horizontal = 11.dp, vertical = 7.dp)
                    .testTag("add_custom_category_chip"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category",
                        tint = colors.secondaryInk,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondaryInk,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showAddDialog && onAddCustomCategory != null) {
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
                    color = colors.primaryInk
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a category name and pick a subtle accent dot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryInk
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    CompactFormTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        placeholder = "e.g. Design, Reading...",
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_category_input")
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "Accent Color Dot",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        paletteOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex == hex

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) colors.primaryInk else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
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
                        val clean = newCategoryInput.trim()
                        if (clean.isNotBlank()) {
                            onAddCustomCategory(clean, selectedColorHex)
                            onCategorySelected(clean)
                            newCategoryInput = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newCategoryInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.White
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
                    Text("Cancel", color = colors.secondaryInk)
                }
            },
            containerColor = colors.surface
        )
    }
}
