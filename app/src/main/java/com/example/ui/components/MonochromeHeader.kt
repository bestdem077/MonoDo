package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MonochromeHeader(
    userName: String,
    streakDays: Int,
    completedTodayCount: Int,
    totalTodayCount: Int,
    modifier: Modifier = Modifier,
    isCalendarView: Boolean = false,
    onToggleCalendarView: (() -> Unit)? = null,
    userBirthTimestamp: Long = 0L,
    dismissedSelfBirthdayYear: Int = -1,
    onDismissSelfBirthday: (() -> Unit)? = null
) {
    val colors = MonoDoThemeTokens.colors
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                currentTimeMillis = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val dateText = remember(currentTimeMillis) {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        sdf.format(Date(currentTimeMillis))
    }

    val isUsersBirthdayToday = remember(currentTimeMillis, userBirthTimestamp, dismissedSelfBirthdayYear) {
        if (userBirthTimestamp <= 0L) false
        else {
            val today = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
            val birth = Calendar.getInstance().apply { timeInMillis = userBirthTimestamp }
            val currentYear = today.get(Calendar.YEAR)
            dismissedSelfBirthdayYear != currentYear &&
                today.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == birth.get(Calendar.DAY_OF_MONTH)
        }
    }

    val greeting = remember(currentTimeMillis, userName) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val salutation = when (hour) {
            in 5..11 -> "Good morning"
            12 -> "Good noon"
            in 13..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
        if (userName.isNotBlank()) "$salutation, $userName" else salutation
    }

    val completionRatio = if (totalTodayCount > 0) {
        (completedTodayCount.toFloat() / totalTodayCount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = completionRatio,
        animationSpec = tween(durationMillis = 360),
        label = "headerProgress"
    )
    val isAllDone = totalTodayCount > 0 && completedTodayCount >= totalTodayCount

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Date row with streak badge and Calendar/List toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateText.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                color = colors.secondaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (streakDays > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentSoft)
                            .border(1.dp, colors.accentBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Streak",
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${streakDays}d",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = colors.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }

                if (onToggleCalendarView != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isCalendarView) colors.accent else colors.surface)
                            .border(
                                1.dp,
                                if (isCalendarView) colors.accent else colors.border,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                onToggleCalendarView()
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .testTag("toggle_calendar_view_button")
                    ) {
                        Icon(
                            imageVector = if (isCalendarView) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Default.CalendarMonth,
                            contentDescription = if (isCalendarView) "Switch to Task List" else "Switch to Monthly Calendar",
                            tint = if (isCalendarView) Color.White else colors.primaryInk,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCalendarView) "List" else "Calendar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCalendarView) Color.White else colors.primaryInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Headline Greeting with safe flex weight so completion pill never gets crushed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )

            if (totalTodayCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAllDone) colors.successSoft else colors.tileBg)
                        .border(
                            1.dp,
                            if (isAllDone) colors.success.copy(alpha = 0.35f) else colors.border,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$completedTodayCount/$totalTodayCount done",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = if (isAllDone) colors.success else colors.secondaryInk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    )
                }
            }
        }

        // User's Own Birthday Subtle Celebratory Moment
        AnimatedVisibility(visible = isUsersBirthdayToday) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentSoft)
                    .border(1.dp, colors.accentBorder, RoundedCornerShape(14.dp))
                    .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                    .testTag("user_self_birthday_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = "Happy Birthday",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Happy Birthday${if (userName.isNotBlank()) ", $userName" else ""}!",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wishing you a calm, joyful, and inspiring new year ahead.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryInk
                    )
                }
                if (onDismissSelfBirthday != null) {
                    IconButton(
                        onClick = onDismissSelfBirthday,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss birthday greeting",
                            tint = colors.secondaryInk,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        if (totalTodayCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isAllDone) colors.success else colors.accent)
                )
            }
        }
    }
}
