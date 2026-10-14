package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskPriority
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import com.example.ui.theme.SoftBorderGrey

@Composable
fun SearchBarAndFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedPriority: TaskPriority?,
    onPrioritySelected: (TaskPriority?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFilterMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search by title or #tag...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGraphite
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SlateGraphite,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
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
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("task_search_field"),
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

        Spacer(modifier = Modifier.width(8.dp))

        // Priority filter dropdown trigger
        Box {
            val isFilterActive = selectedPriority != null
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFilterActive) ObsidianBlack else PaperSurface)
                    .border(
                        1.dp,
                        if (isFilterActive) ObsidianBlack else HairlineBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { isFilterMenuOpen = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter Priority",
                    tint = if (isFilterActive) PaperSurface else SlateGraphite,
                    modifier = Modifier.size(18.dp)
                )
                if (selectedPriority != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedPriority.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperSurface,
                        fontSize = 10.sp
                    )
                }
            }

            DropdownMenu(
                expanded = isFilterMenuOpen,
                onDismissRequest = { isFilterMenuOpen = false },
                modifier = Modifier.background(PaperSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("All Priorities", style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onPrioritySelected(null)
                        isFilterMenuOpen = false
                    }
                )
                TaskPriority.entries.forEach { priority ->
                    DropdownMenuItem(
                        text = { Text(priority.label, style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            onPrioritySelected(priority)
                            isFilterMenuOpen = false
                        }
                    )
                }
            }
        }
    }
}
