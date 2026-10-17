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
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper

@Composable
fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categoryCounts: Map<String, Int> = emptyMap(),
    onAddCustomCategory: ((name: String, colorHex: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#2563EB") }

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
                    style != null -> style.backgroundColor
                    else -> PaperSurface
                },
                animationSpec = spring(),
                label = "chipBg"
            )

            val textColor by animateColorAsState(
                targetValue = when {
                    isSelected -> Color.White
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
                    if (style != null) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(style.accentColor)
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

                    if (count > 0 || (isAll && count > 0)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF2E2E2E) else Color(0xFFECEBE5))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else SlateGraphite,
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
                    color = ObsidianBlack
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a category name. MonoDo will generate a subtle accent tag for it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGraphite
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        placeholder = { Text("e.g. Design, Reading...", color = SlateGraphite) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_category_input"),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = ObsidianBlack),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ObsidianBlack,
                            unfocusedTextColor = ObsidianBlack,
                            cursorColor = RoyalBlue,
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = FocusedBackground,
                            unfocusedContainerColor = PaperCanvas
                        )
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "Accent Color Dot",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paletteOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex == hex

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
                                    .clickable { selectedColorHex = hex },
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
                        containerColor = ObsidianBlack,
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
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface
        )
    }
}
