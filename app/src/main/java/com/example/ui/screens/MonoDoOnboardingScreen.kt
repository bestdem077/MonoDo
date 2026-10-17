package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MonoDoOnboardingScreen(
    initialUserName: String = "",
    initialBirthDate: String = "",
    initialBirthTimestamp: Long = 0L,
    initialReminderHour: Int = 9,
    initialReminderMinute: Int = 0,
    onComplete: (
        name: String,
        birthDate: String,
        birthTimestamp: Long,
        isReminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) } // 0: Name, 1: Date of Birth, 2: Reminder Time

    var nameInput by remember { mutableStateOf(initialUserName) }
    var nameError by remember { mutableStateOf(false) }

    var birthDateText by remember { mutableStateOf(initialBirthDate) }
    var birthTimestamp by remember { mutableLongStateOf(initialBirthTimestamp) }

    var isReminderEnabled by remember { mutableStateOf(true) }
    var reminderHour by remember { mutableIntStateOf(initialReminderHour) }
    var reminderMinute by remember { mutableIntStateOf(initialReminderMinute) }

    // Intercept back button when past step 0
    BackHandler(enabled = currentStep > 0) {
        currentStep -= 1
    }

    val showDatePicker = {
        val cal = Calendar.getInstance()
        if (birthTimestamp > 0) cal.timeInMillis = birthTimestamp
        else cal.add(Calendar.YEAR, -24)

        val dialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }
                birthTimestamp = chosen.timeInMillis
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                birthDateText = sdf.format(Date(birthTimestamp))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    val showTimePicker = {
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                reminderHour = hour
                reminderMinute = minute
            },
            reminderHour,
            reminderMinute,
            false
        )
        dialog.show()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("monodo_onboarding_screen"),
        color = PaperCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: MonoDo Emblem Logo & Step Progress Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
            ) {
                // Emboldened MonoDo Emblem Vector Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(elevation = 8.dp, shape = CircleShape, spotColor = RoyalBlue.copy(alpha = 0.25f))
                        .clip(CircleShape)
                        .background(PaperSurface)
                        .border(1.5.dp, HairlineBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_monodo_logo),
                        contentDescription = "MonoDo Emblem",
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "MonoDo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = ObsidianBlack,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Minimalist Workspace",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Smooth Step Indicators (3 steps)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.55f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { stepIndex ->
                        val isActive = stepIndex == currentStep
                        val isDone = stepIndex < currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        isActive -> RoyalBlue
                                        isDone -> ObsidianBlack
                                        else -> HairlineBorder
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Step Content Card with Animated Transitions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "onboardingStepTransition"
                ) { targetStep ->
                    when (targetStep) {
                        0 -> NameStep(
                            name = nameInput,
                            onNameChange = {
                                nameInput = it
                                if (it.isNotBlank()) nameError = false
                            },
                            isError = nameError
                        )

                        1 -> BirthDateStep(
                            birthDateText = birthDateText,
                            onPickDate = showDatePicker
                        )

                        2 -> ReminderStep(
                            isReminderEnabled = isReminderEnabled,
                            onReminderToggle = { isReminderEnabled = it },
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute,
                            onPickTime = showTimePicker,
                            onSelectPreset = { hour, minute ->
                                reminderHour = hour
                                reminderMinute = minute
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Section: Continue / Enter MonoDo Button & Offline Privacy Assurance
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        when (currentStep) {
                            0 -> {
                                if (nameInput.trim().isBlank()) {
                                    nameError = true
                                } else {
                                    currentStep = 1
                                }
                            }
                            1 -> {
                                currentStep = 2
                            }
                            2 -> {
                                onComplete(
                                    nameInput.trim(),
                                    birthDateText.trim(),
                                    birthTimestamp,
                                    isReminderEnabled,
                                    reminderHour,
                                    reminderMinute
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(27.dp),
                            spotColor = RoyalBlue.copy(alpha = 0.35f)
                        )
                        .testTag("onboarding_continue_btn"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentStep == 2) "Enter MonoDo" else "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentStep == 2) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 100% Offline Privacy Reassurance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Offline Lock",
                        tint = SlateGraphite,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% Offline • Kept securely on your device",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    isError: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RoyalBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Welcome to MonoDo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What is your name? We'll personalize your daily workspace and progress cards.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGraphite,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        com.example.ui.components.CompactFormTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = "Enter your name",
            singleLine = true,
            fontWeight = FontWeight.SemiBold,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name_input")
        )

        if (isError) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Please enter your name to personalize your workspace",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BirthDateStep(
    birthDateText: String,
    onPickDate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RoyalBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "When is your birthday?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Used to celebrate your milestones and tailor your productivity cycle.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGraphite,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Date Picker Trigger Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PaperCanvas)
                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                .clickable { onPickDate() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("onboarding_dob_picker"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Choose Date",
                    tint = if (birthDateText.isNotBlank()) RoyalBlue else SlateGraphite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (birthDateText.isNotBlank()) birthDateText else "Select date of birth",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (birthDateText.isNotBlank()) ObsidianBlack else SlateGraphite,
                    fontWeight = if (birthDateText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DateTileBackground)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (birthDateText.isNotBlank()) "Change" else "Choose",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReminderStep(
    isReminderEnabled: Boolean,
    onReminderToggle: (Boolean) -> Unit,
    reminderHour: Int,
    reminderMinute: Int,
    onPickTime: () -> Unit,
    onSelectPreset: (hour: Int, minute: Int) -> Unit
) {
    val presets = listOf(
        Triple("Morning", 8, 0),
        Triple("Focus Start", 9, 0),
        Triple("Mid-day", 13, 0),
        Triple("Evening", 20, 0)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RoyalBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Daily Reminder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Stay consistent with a single distraction-free daily prompt.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGraphite,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PaperCanvas)
                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Enable Daily Prompt",
                style = MaterialTheme.typography.bodyMedium,
                color = ObsidianBlack,
                fontWeight = FontWeight.SemiBold
            )

            Switch(
                checked = isReminderEnabled,
                onCheckedChange = onReminderToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RoyalBlue,
                    uncheckedThumbColor = SlateGraphite,
                    uncheckedTrackColor = DateTileBackground
                )
            )
        }

        AnimatedVisibility(visible = isReminderEnabled) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                // Time Card Trigger
                val formattedTime = remember(reminderHour, reminderMinute) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, reminderHour)
                        set(Calendar.MINUTE, reminderMinute)
                    }
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    sdf.format(cal.time)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PaperCanvas)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        .clickable { onPickTime() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("onboarding_reminder_time_picker"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Time",
                            tint = RoyalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DateTileBackground)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Set Custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, h, m) ->
                        val isSelected = reminderHour == h && reminderMinute == m
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
                                .clickable { onSelectPreset(h, m) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else SlateGraphite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
