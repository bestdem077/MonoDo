package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateState
import com.example.data.update.VersionComparator
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper

/**
 * Compact Swiss-styled banner shown at the top of Tasks when a newer GitHub Release is available,
 * downloading, or ready to install.
 */
@Composable
fun UpdateAvailableBanner(
    state: AppUpdateState,
    dismissedTag: String,
    onOpenSheet: () -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val releaseTag = when (state) {
        is AppUpdateState.UpdateAvailable -> state.release.tagName
        is AppUpdateState.Downloading -> state.release.tagName
        is AppUpdateState.ReadyToInstall -> state.release.tagName
        else -> null
    }

    val shouldShow = when (state) {
        is AppUpdateState.UpdateAvailable -> state.release.tagName != dismissedTag
        is AppUpdateState.Downloading -> true
        is AppUpdateState.ReadyToInstall -> true
        else -> false
    }

    AnimatedVisibility(visible = shouldShow && releaseTag != null) {
        val tag = releaseTag ?: return@AnimatedVisibility
        val isReady = state is AppUpdateState.ReadyToInstall
        val isDownloading = state is AppUpdateState.Downloading

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isReady) colors.successSoft else colors.accentSoft)
                .border(
                    1.dp,
                    if (isReady) colors.successBorder else colors.accentBorder,
                    RoundedCornerShape(14.dp)
                )
                .clickable { onOpenSheet() }
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                .testTag("app_update_banner"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isReady) colors.success else colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isReady -> Icons.Default.InstallMobile
                        isDownloading -> Icons.Default.Download
                        else -> Icons.Default.SystemUpdate
                    },
                    contentDescription = "MonoDo Update",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (state) {
                        is AppUpdateState.Downloading -> "Downloading ${VersionComparator.displayTag(tag)} (${state.progressPercent}%)"
                        is AppUpdateState.ReadyToInstall -> "Update ${VersionComparator.displayTag(tag)} Ready"
                        else -> "MonoDo ${VersionComparator.displayTag(tag)} Available"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (state) {
                        is AppUpdateState.Downloading -> "Saving APK to Downloads folder..."
                        is AppUpdateState.ReadyToInstall -> "Tap to install in-place without losing data"
                        else -> "Tap to download & install directly"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isReady) colors.success else colors.accent)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = when {
                        isReady -> "Install"
                        isDownloading -> "View"
                        else -> "Update"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state is AppUpdateState.UpdateAvailable) {
                IconButton(
                    onClick = { onDismiss(tag) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss update banner",
                        tint = colors.secondaryInk,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateModalSheet(
    sheetState: SheetState,
    state: AppUpdateState,
    onDismiss: () -> Unit,
    onCheckNow: () -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    val colors = MonoDoThemeTokens.colors
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var canInstallPackages by remember { mutableStateOf(AppUpdateManager.canInstallPackages(context)) }
    var awaitingPermissionReturn by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val allowedNow = AppUpdateManager.canInstallPackages(context)
                canInstallPackages = allowedNow
                if (allowedNow && awaitingPermissionReturn && state is AppUpdateState.ReadyToInstall) {
                    awaitingPermissionReturn = false
                    AppUpdateManager.launchApkInstaller(context, state.cacheFilePath)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
                .testTag("app_update_modal_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    is AppUpdateState.ReadyToInstall, is AppUpdateState.UpToDate -> colors.successSoft
                                    is AppUpdateState.Error -> colors.dangerSoft
                                    else -> colors.accentSoft
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (state) {
                                is AppUpdateState.ReadyToInstall -> Icons.Default.InstallMobile
                                is AppUpdateState.UpToDate -> Icons.Default.CheckCircle
                                is AppUpdateState.Error -> Icons.Default.ErrorOutline
                                else -> Icons.Default.SystemUpdate
                            },
                            contentDescription = null,
                            tint = when (state) {
                                is AppUpdateState.ReadyToInstall, is AppUpdateState.UpToDate -> colors.success
                                is AppUpdateState.Error -> colors.danger
                                else -> colors.accent
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MonoDo Updates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Text(
                            text = "github.com/${AppUpdateManager.getGitHubRepoSlug()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = colors.secondaryInk
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.secondaryInk
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is AppUpdateState.Idle, is AppUpdateState.Checking -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.canvas)
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = colors.accent,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Checking GitHub Releases...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryInk
                        )
                        Text(
                            text = "Installed: ${AppUpdateManager.getCurrentInstalledTag()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = colors.secondaryInk
                        )
                    }
                }

                is AppUpdateState.UpToDate -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.successSoft)
                            .border(1.dp, colors.successBorder, RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "You're up to date!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MonoDo ${state.currentVersion} is the latest release available on GitHub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryInk
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCheckNow,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Again")
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Done")
                        }
                    }
                }

                is AppUpdateState.UpdateAvailable -> {
                    val release = state.release
                    VersionComparisonCard(
                        currentVersion = state.currentVersion,
                        newVersion = VersionComparator.displayTag(release.tagName),
                        sizeLabel = AppUpdateManager.formatFileSize(release.apkSizeBytes)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "WHAT'S NEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.secondaryInk,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.canvas)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = release.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primaryInk,
                            lineHeight = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DataSafeReassurancePill()

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            onStartDownload()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_update_download_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Update (${AppUpdateManager.formatFileSize(release.apkSizeBytes)})",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Maybe Later", color = colors.secondaryInk)
                    }
                }

                is AppUpdateState.Downloading -> {
                    val animatedProgress by animateFloatAsState(
                        targetValue = (state.progressPercent / 100f).coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 180),
                        label = "apkDownloadProgress"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.canvas)
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading ${VersionComparator.displayTag(state.release.tagName)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryInk
                            )
                            Text(
                                text = "${state.progressPercent}%",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (state.totalBytes > 0L) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = colors.accent,
                                trackColor = colors.border
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = colors.accent,
                                trackColor = colors.border
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val progressDetail = if (state.totalBytes > 0L) {
                            "${AppUpdateManager.formatFileSize(state.downloadedBytes)} / ${AppUpdateManager.formatFileSize(state.totalBytes)}"
                        } else {
                            "${AppUpdateManager.formatFileSize(state.downloadedBytes)} downloaded"
                        }
                        Text(
                            text = "$progressDetail • Saving to Downloads folder as well",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onCancelDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("cancel_update_download_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel Download", color = colors.danger, fontWeight = FontWeight.SemiBold)
                    }
                }

                is AppUpdateState.ReadyToInstall -> {
                    val fileName = state.savedInDownloadsName ?: state.release.apkFileName

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.successSoft)
                            .border(1.dp, colors.successBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Download Complete — ${VersionComparator.displayTag(state.release.tagName)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.savedInDownloadsSuccess) {
                                "Saved to Downloads/$fileName"
                            } else {
                                "APK ready to install ($fileName)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = colors.success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!canInstallPackages) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Choose how you want to install:\n• Allow MonoDo once to install updates directly inside the app.\n• Or open your Downloads folder to tap $fileName manually without granting permission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryInk,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                awaitingPermissionReturn = true
                                runCatching {
                                    context.startActivity(AppUpdateManager.createInstallPermissionIntent(context))
                                }.onFailure {
                                    awaitingPermissionReturn = false
                                    Toast.makeText(
                                        context,
                                        "Couldn't open install permission settings. Use Downloads folder instead.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("allow_and_install_update_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Allow & Install Here", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                val launched = AppUpdateManager.launchApkInstaller(context, state.cacheFilePath)
                                if (!launched) {
                                    Toast.makeText(
                                        context,
                                        "Installer couldn't open directly. Tap 'Open Downloads Folder' below.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("install_update_now_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install Update Now", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            val opened = AppUpdateManager.openDownloadsFolder(context)
                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    "Open your Files app -> Downloads -> $fileName",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_downloads_folder_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = colors.primaryInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (canInstallPackages) "Open Downloads Folder" else "Open Downloads Folder (No Permission)",
                            color = colors.primaryInk,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is AppUpdateState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.dangerSoft)
                            .border(1.dp, colors.dangerBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Update Check Failed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.danger
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primaryInk
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (state.release != null) onStartDownload() else onCheckNow()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                        OutlinedButton(
                            onClick = {
                                val url = state.release?.htmlUrl ?: AppUpdateManager.getReleasesPageUrl()
                                AppUpdateManager.openInBrowser(context, url)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GitHub")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionComparisonCard(
    currentVersion: String,
    newVersion: String,
    sizeLabel: String
) {
    val colors = MonoDoThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.accentSoft)
            .border(1.dp, colors.accentBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "NEW RELEASE",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$currentVersion  →  $newVersion",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                fontWeight = FontWeight.Bold,
                color = colors.primaryInk
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.accentBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }
    }
}

@Composable
private fun DataSafeReassurancePill() {
    val colors = MonoDoThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.tileBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = colors.success,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Installs over MonoDo with all tasks, notes, habits & birthdays preserved.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryInk,
            lineHeight = 16.sp
        )
    }
}
