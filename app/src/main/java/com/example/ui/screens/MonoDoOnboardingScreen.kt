package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper

@Composable
fun MonoDoOnboardingScreen(
    initialUserName: String = "",
    initialWorkspaceTitle: String = "MonoDo",
    onComplete: (userName: String, workspaceTitle: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var userName by remember { mutableStateOf(initialUserName) }
    var workspaceTitle by remember { mutableStateOf(if (initialWorkspaceTitle.isNotBlank()) initialWorkspaceTitle else "MonoDo") }

    // Prevent repeated onboarding loop if user hits back button
    BackHandler {
        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
        onComplete(userName.trim(), workspaceTitle.trim().ifBlank { "MonoDo" })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Emboldened App Logo inside soft modern circular avatar container (Finora-inspired)
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(RoyalBlueSoft)
                    .border(1.5.dp, RoyalBlueBorder.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_monodo_logo),
                    contentDescription = "MonoDo Logo",
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Welcome to MonoDo",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = ObsidianBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Personalize your distraction-free workspace. 100% offline, private, and focused.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateGraphite,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Form Section Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                // Input 1: User's Name
                Text(
                    text = "YOUR NAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    placeholder = { Text("Enter your name (e.g. Alex)", color = SlateGraphite) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = "User Name",
                            tint = if (userName.isNotBlank()) RoyalBlue else SlateGraphite,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_user_name_input"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = ObsidianBlack),
                    shape = RoundedCornerShape(14.dp),
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

                Spacer(modifier = Modifier.height(18.dp))

                // Input 2: App / Workspace Title
                Text(
                    text = "APP / WORKSPACE NAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = workspaceTitle,
                    onValueChange = { workspaceTitle = it },
                    placeholder = { Text("Customize app name (e.g. MonoDo)", color = SlateGraphite) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Workspace Name",
                            tint = if (workspaceTitle != "MonoDo" && workspaceTitle.isNotBlank()) RoyalBlue else SlateGraphite,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_workspace_title_input"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = ObsidianBlack),
                    shape = RoundedCornerShape(14.dp),
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Value pillars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                FeaturePill(icon = Icons.Default.OfflineBolt, label = "100% Offline")
                FeaturePill(icon = Icons.Default.Shield, label = "Zero Tracking")
                FeaturePill(icon = Icons.Default.CheckCircleOutline, label = "Habit Streaks")
            }
        }

        // Bottom CTA Buttons (Finora-style pill button with Royal Blue color)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp)
        ) {
            Button(
                onClick = {
                    HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                    onComplete(userName.trim(), workspaceTitle.trim().ifBlank { "MonoDo" })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp), spotColor = RoyalBlue.copy(alpha = 0.35f))
                    .testTag("onboarding_continue_btn"),
                shape = RoundedCornerShape(28.dp),
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
                        text = if (workspaceTitle.isNotBlank() && workspaceTitle.trim() != "MonoDo") "Enter ${workspaceTitle.trim()}" else "Enter MonoDo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Proceed",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    onComplete("", "MonoDo")
                },
                modifier = Modifier.testTag("onboarding_skip_btn")
            ) {
                Text(
                    text = "Skip and enter MonoDo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RoyalBlueSoft)
            .border(1.dp, RoyalBlueBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = RoyalBlue,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ObsidianBlack,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}
