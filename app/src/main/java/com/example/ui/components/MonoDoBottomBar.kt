package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.NavigationTab
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper

@Composable
fun MonoDoBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPomodoroRunning: Boolean = false
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = if (colors.isDark) 0.dp else 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Crisp top hairline divider only
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavTabItem(
                    tab = NavigationTab.TASKS,
                    isSelected = currentTab == NavigationTab.TASKS,
                    onClick = {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        onTabSelected(NavigationTab.TASKS)
                    },
                    modifier = Modifier.weight(1f)
                )

                NavTabItem(
                    tab = NavigationTab.STATS,
                    isSelected = currentTab == NavigationTab.STATS,
                    onClick = {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        onTabSelected(NavigationTab.STATS)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Center Royal Blue '+' Action Pill
                Box(
                    modifier = Modifier.weight(1.1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = if (colors.isDark) 2.dp else 6.dp,
                                shape = CircleShape,
                                spotColor = colors.accent.copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .background(colors.accent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White)
                            ) {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                onAddClick()
                            }
                            .testTag("center_fab_add_task"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create New Task",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                NavTabItem(
                    tab = NavigationTab.TOOLS,
                    isSelected = currentTab == NavigationTab.TOOLS,
                    showActiveDot = isPomodoroRunning && currentTab != NavigationTab.TOOLS,
                    onClick = {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        onTabSelected(NavigationTab.TOOLS)
                    },
                    modifier = Modifier.weight(1f)
                )

                NavTabItem(
                    tab = NavigationTab.SETTINGS,
                    isSelected = currentTab == NavigationTab.SETTINGS,
                    onClick = {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        onTabSelected(NavigationTab.SETTINGS)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun NavTabItem(
    tab: NavigationTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showActiveDot: Boolean = false
) {
    val colors = MonoDoThemeTokens.colors
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.secondaryInk,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navScale"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 26.dp)
            ) { onClick() }
            .padding(vertical = 6.dp)
            .scale(scale)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title,
                tint = animatedColor,
                modifier = Modifier.size(22.dp)
            )
            if (showActiveDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall,
            color = animatedColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}
