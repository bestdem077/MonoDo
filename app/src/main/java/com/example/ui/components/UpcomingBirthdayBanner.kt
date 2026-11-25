package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BirthdayDateCalculator
import com.example.data.model.BirthdayEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens

/**
 * Clean, zero-clutter birthday highlight banner shown on the Tasks home screen
 * ONLY when a friend or family member's birthday is Today or within the next 3 days.
 */
@Composable
fun UpcomingBirthdayBanner(
    birthdays: List<BirthdayEntity>,
    onOpenBirthdays: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val soonest = remember(birthdays) {
        birthdays
            .map { it to BirthdayDateCalculator.daysUntilBirthday(it.month, it.day) }
            .filter { it.second in 0..3 }
            .sortedBy { it.second }
    }

    if (soonest.isEmpty()) return

    val (topBirthday, daysUntil) = soonest.first()
    val extraCount = soonest.size - 1
    val age = remember(topBirthday) {
        BirthdayDateCalculator.turningAge(topBirthday.month, topBirthday.day, topBirthday.birthYear)
    }
    val dateLabel = remember(topBirthday) {
        BirthdayDateCalculator.formatShortDate(topBirthday.month, topBirthday.day)
    }

    val badgeLabel = when (daysUntil) {
        0 -> "TODAY"
        1 -> "TONIGHT 12 AM"
        else -> "IN $daysUntil DAYS"
    }

    val headline = when (daysUntil) {
        0 -> if (age != null) "${topBirthday.name} turns $age today!" else "${topBirthday.name}'s Birthday Today!"
        1 -> if (age != null) "${topBirthday.name} turns $age at 12 AM" else "${topBirthday.name}'s Birthday Tonight"
        else -> "${topBirthday.name}'s Birthday • $dateLabel"
    }

    val subtitle = buildString {
        append(BirthdayDateCalculator.formatReminderLeadLabel(topBirthday.hoursBeforeMidnight))
        if (extraCount > 0) {
            append(" • +$extraCount more soon")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.accentSoft)
            .border(1.dp, colors.accentBorder, RoundedCornerShape(14.dp))
            .clickable { onOpenBirthdays() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("upcoming_birthday_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = "Upcoming Birthday",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.accent)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = topBirthday.relationship,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryInk,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open Birthdays",
            tint = colors.accent,
            modifier = Modifier.size(18.dp)
        )
    }
}
