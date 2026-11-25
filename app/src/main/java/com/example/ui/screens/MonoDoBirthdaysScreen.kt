package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.BirthdayDateCalculator
import com.example.data.model.BirthdayEntity
import com.example.data.model.BirthdayWishTemplates
import com.example.data.model.WishCategory
import com.example.data.reminder.ReminderScheduler
import com.example.ui.components.BirthdayEditorSheet
import com.example.ui.components.CompactFormTextField
import com.example.ui.components.SwissEmptyArtType
import com.example.ui.components.SwissEmptyStateArt
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

private val MONTH_FULL_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoDoBirthdaysScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val birthdays by viewModel.allBirthdays.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var groupByMonth by remember { mutableStateOf(false) }

    var isEditorOpen by remember { mutableStateOf(false) }
    var editingBirthday by remember { mutableStateOf<BirthdayEntity?>(null) }
    var wishTargetBirthday by remember { mutableStateOf<BirthdayEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val wishSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var exactAlarmsReady by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }
    var batteryUnrestricted by remember { mutableStateOf(ReminderScheduler.isIgnoringBatteryOptimizations(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsReady = ReminderScheduler.canScheduleExactAlarms(context)
                batteryUnrestricted = ReminderScheduler.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sortedBirthdays = remember(birthdays) {
        birthdays
            .map { it to BirthdayDateCalculator.daysUntilBirthday(it.month, it.day) }
            .sortedWith(compareBy<Pair<BirthdayEntity, Int>> { it.second }.thenBy { it.first.name })
    }

    val filteredBirthdays = remember(sortedBirthdays, selectedFilter, searchQuery) {
        sortedBirthdays.filter { (bday, _) ->
            val matchesRel = selectedFilter == "All" || bday.relationship.equals(selectedFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                bday.name.contains(searchQuery, ignoreCase = true) ||
                bday.note.contains(searchQuery, ignoreCase = true) ||
                bday.relationship.contains(searchQuery, ignoreCase = true)
            matchesRel && matchesQuery
        }
    }

    val monthGroupedBirthdays = remember(filteredBirthdays) {
        filteredBirthdays
            .sortedWith(compareBy<Pair<BirthdayEntity, Int>> { it.first.month }.thenBy { it.first.day }.thenBy { it.first.name })
            .groupBy { it.first.month }
            .toSortedMap()
    }

    val filters = listOf("All", "Family", "Friends", "Partner", "Work", "Other")
    val nextUp = sortedBirthdays.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .testTag("mono_do_birthdays_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Title + Add Action
            item(key = "birthdays_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Birthdays",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = "Friends & family with 3–4h advance eve alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryInk
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.accent)
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                editingBirthday = null
                                isEditorOpen = true
                            }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                            .testTag("add_birthday_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Birthday",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Next Celebration Hero Card
            if (nextUp != null && searchQuery.isBlank()) {
                item(key = "next_birthday_hero") {
                    val (bday, daysUntil) = nextUp
                    val age = BirthdayDateCalculator.turningAge(bday.month, bday.day, bday.birthYear)
                    val dateLabel = BirthdayDateCalculator.formatShortDate(bday.month, bday.day)
                    val countdown = BirthdayDateCalculator.formatCountdownLabel(daysUntil)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surface)
                            .border(1.5.dp, colors.accentBorder, RoundedCornerShape(18.dp))
                            .clickable {
                                editingBirthday = bday
                                isEditorOpen = true
                            }
                            .padding(16.dp)
                            .testTag("next_birthday_hero_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.accent)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = countdown,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NEXT CELEBRATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryInk,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp
                                )
                            }

                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = colors.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (age != null) "${bday.name} • Turns $age" else bday.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.primaryInk,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.accentSoft)
                                    .border(1.dp, colors.accentBorder, RoundedCornerShape(999.dp))
                                    .clickable {
                                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                        wishTargetBirthday = bday
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("hero_wish_templates_btn"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Wish",
                                    tint = colors.accent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Wish",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accentSoft)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = buildString {
                                        append(BirthdayDateCalculator.formatReminderLeadLabel(bday.hoursBeforeMidnight))
                                        if (bday.notifyAtMidnight) append(" + 12 AM")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.tileBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = bday.relationship,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryInk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        if (bday.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Note: ${bday.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryInk,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 6-Month Background Reliability Shield
            item(key = "birthday_reliability_shield") {
                val allOptimal = exactAlarmsReady && batteryUnrestricted
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (allOptimal) colors.successSoft else colors.surface)
                        .border(
                            1.dp,
                            if (allOptimal) colors.successBorder else colors.border,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = !allOptimal) {
                            if (!exactAlarmsReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    )
                                }
                            } else if (!batteryUnrestricted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("birthday_reliability_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (allOptimal) colors.success else colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (allOptimal) {
                                    "Long-Term Alerts Protected"
                                } else if (!exactAlarmsReady) {
                                    "Allow Exact Alarms for Birthdays"
                                } else {
                                    "Set Battery to Unrestricted (6-Month Safe)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primaryInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (allOptimal) {
                                    "Fires even after months unopened, phone reboots & Doze"
                                } else if (!exactAlarmsReady) {
                                    "Tap to enable exact eve & 12 AM birthday alerts"
                                } else {
                                    "Prevents Android from pausing MonoDo if unopened for months"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryInk,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    if (!allOptimal) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fix",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Search + Countdown / By Month Toggle + Relationship Filters
            if (birthdays.isNotEmpty()) {
                item(key = "birthday_search_and_view_mode") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactFormTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search ${birthdays.size} saved birthdays by name or note...",
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("birthdays_search_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf(false to "Countdown", true to "By Month").forEach { (isMonthMode, label) ->
                                    val active = groupByMonth == isMonthMode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) colors.accent else Color.Transparent)
                                            .clickable {
                                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                                groupByMonth = isMonthMode
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("birthday_sort_${label.lowercase().replace(" ", "_")}")
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (active) Color.White else colors.secondaryInk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${filteredBirthdays.size} shown",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = colors.secondaryInk,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                item(key = "birthday_filter_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filters.forEach { rel ->
                            val isSelected = selectedFilter == rel
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isSelected) colors.primaryInk else colors.surface)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.primaryInk else colors.border,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable {
                                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                        selectedFilter = rel
                                    }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = rel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) colors.canvas else colors.secondaryInk,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Empty State or Birthday Cards (Countdown vs Month-Wise Bulk View)
            if (filteredBirthdays.isEmpty()) {
                item(key = "birthdays_empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SwissEmptyStateArt(
                            type = SwissEmptyArtType.BIRTHDAYS,
                            size = 86.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when {
                                birthdays.isEmpty() -> "Never miss a birthday"
                                searchQuery.isNotBlank() -> "No matching birthdays"
                                else -> "No $selectedFilter birthdays yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (birthdays.isEmpty()) {
                                "Add friends & family dates to get reliable alerts 3–4 hours before 12 AM, even if you don't open MonoDo for months."
                            } else {
                                "Try clearing your search or choosing a different relationship filter."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryInk,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        if (birthdays.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    editingBirthday = null
                                    isEditorOpen = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Birthday", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (groupByMonth) {
                monthGroupedBirthdays.forEach { (monthIndex, listForMonth) ->
                    val monthName = MONTH_FULL_NAMES.getOrElse(monthIndex - 1) { "Month $monthIndex" }
                    item(key = "month_header_$monthIndex") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = monthName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${listForMonth.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = colors.secondaryInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(listForMonth, key = { it.first.id }) { (birthday, daysUntil) ->
                        Box(modifier = Modifier.animateItem()) {
                            BirthdayItemCard(
                                birthday = birthday,
                                daysUntil = daysUntil,
                                onEdit = {
                                    editingBirthday = birthday
                                    isEditorOpen = true
                                },
                                onOpenWishes = {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    wishTargetBirthday = birthday
                                },
                                onPreviewAlert = {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    viewModel.sendTestBirthdayNotification(birthday)
                                },
                                onDelete = {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    viewModel.deleteBirthday(birthday)
                                }
                            )
                        }
                    }
                }
            } else {
                items(filteredBirthdays, key = { it.first.id }) { (birthday, daysUntil) ->
                    Box(modifier = Modifier.animateItem()) {
                        BirthdayItemCard(
                            birthday = birthday,
                            daysUntil = daysUntil,
                            onEdit = {
                                editingBirthday = birthday
                                isEditorOpen = true
                            },
                            onOpenWishes = {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                wishTargetBirthday = birthday
                            },
                            onPreviewAlert = {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                viewModel.sendTestBirthdayNotification(birthday)
                            },
                            onDelete = {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                viewModel.deleteBirthday(birthday)
                            }
                        )
                    }
                }
            }
        }

        if (isEditorOpen) {
            BirthdayEditorSheet(
                sheetState = sheetState,
                birthdayToEdit = editingBirthday,
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        isEditorOpen = false
                        editingBirthday = null
                    }
                },
                onSave = { name, month, day, birthYear, relationship, note, hoursBeforeMidnight, notifyAtMidnight ->
                    val existing = editingBirthday
                    if (existing != null) {
                        viewModel.updateBirthday(
                            existing = existing,
                            name = name,
                            month = month,
                            day = day,
                            birthYear = birthYear,
                            relationship = relationship,
                            note = note,
                            hoursBeforeMidnight = hoursBeforeMidnight,
                            notifyAtMidnight = notifyAtMidnight
                        )
                    } else {
                        viewModel.addBirthday(
                            name = name,
                            month = month,
                            day = day,
                            birthYear = birthYear,
                            relationship = relationship,
                            note = note,
                            hoursBeforeMidnight = hoursBeforeMidnight,
                            notifyAtMidnight = notifyAtMidnight
                        )
                    }
                }
            )
        }

        // 1-Tap Birthday Wish Templates Sheet (Friend, Family, Formal + Copy & Share)
        wishTargetBirthday?.let { target ->
            val turningAge = BirthdayDateCalculator.turningAge(target.month, target.day, target.birthYear)
            val allWishes = remember(target.id, target.name, turningAge) {
                BirthdayWishTemplates.wishesFor(target.name, turningAge)
            }
            var selectedCategory by remember(target.id) {
                mutableStateOf(
                    when {
                        target.relationship.equals("Family", ignoreCase = true) ||
                            target.relationship.equals("Partner", ignoreCase = true) -> WishCategory.FAMILY
                        target.relationship.equals("Work", ignoreCase = true) -> WishCategory.FORMAL
                        else -> WishCategory.FRIEND
                    }
                )
            }

            ModalBottomSheet(
                onDismissRequest = { wishTargetBirthday = null },
                sheetState = wishSheetState,
                containerColor = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .testTag("birthday_wish_templates_sheet")
                ) {
                    Text(
                        text = "Wish ${target.name}",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "1-tap ready-made wishes — copy or share directly to WhatsApp/SMS",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryInk
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WishCategory.entries.forEach { cat ->
                            val active = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) colors.accent else colors.canvas)
                                    .border(
                                        1.dp,
                                        if (active) colors.accent else colors.border,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                        selectedCategory = cat
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) Color.White else colors.primaryInk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    allWishes.filter { it.category == selectedCategory }.forEach { option ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = option.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = option.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.primaryInk,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surface)
                                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                        .clickable {
                                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Birthday Wish", option.message))
                                            Toast.makeText(context, "Wish copied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Wish",
                                        tint = colors.primaryInk,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Copy",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primaryInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.accent)
                                        .clickable {
                                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, option.message)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Birthday Wish"))
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Wish",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Share",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun BirthdayItemCard(
    birthday: BirthdayEntity,
    daysUntil: Int,
    onEdit: () -> Unit,
    onOpenWishes: () -> Unit,
    onPreviewAlert: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MonoDoThemeTokens.colors
    val dateLabel = remember(birthday.month, birthday.day) {
        BirthdayDateCalculator.formatShortDate(birthday.month, birthday.day)
    }
    val age = remember(birthday.month, birthday.day, birthday.birthYear) {
        BirthdayDateCalculator.turningAge(birthday.month, birthday.day, birthday.birthYear)
    }
    val isUrgent = daysUntil <= 1
    val initial = birthday.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "B"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(
                width = if (isUrgent) 1.5.dp else 1.dp,
                color = if (isUrgent) colors.accentBorder else colors.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onEdit() }
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("birthday_card_${birthday.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Initial Monogram Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isUrgent) colors.accent else colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = if (isUrgent) Color.White else colors.accent,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = birthday.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isUrgent) colors.accentSoft else colors.tileBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = BirthdayDateCalculator.formatCountdownLabel(daysUntil),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (isUrgent) colors.accent else colors.secondaryInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildString {
                    append(dateLabel)
                    if (age != null) append(" • Turns $age")
                    append(" • ${birthday.relationship}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = buildString {
                    append("Alert: ")
                    append(BirthdayDateCalculator.formatReminderLeadLabel(birthday.hoursBeforeMidnight))
                    if (birthday.notifyAtMidnight) append(" + 12 AM")
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 1-Tap Wish Templates Action
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.accentSoft)
                .clickable { onOpenWishes() }
                .testTag("wish_birthday_${birthday.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Wish Templates",
                tint = colors.accent,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        // Quick Test Alert Bell
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.tileBg)
                .clickable { onPreviewAlert() }
                .testTag("preview_birthday_alert_${birthday.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Test Birthday Alert",
                tint = colors.accent,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        // Delete Action
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.tileBg)
                .clickable { onDelete() }
                .testTag("delete_birthday_${birthday.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Birthday",
                tint = colors.secondaryInk,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
