package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.MonoDoThemeTokens

@Composable
fun AppLockOverlay(
    workspaceTitle: String,
    errorMessage: String?,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors

    // Auto-trigger system fingerprint/PIN prompt when lock shield appears
    LaunchedEffect(Unit) {
        onUnlockClick()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(28.dp)
            .testTag("app_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.5.dp, colors.border, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_monodo_obsidian_mark),
                    contentDescription = "MonoDo Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.canvas)
                    .border(1.dp, colors.border, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "PRIVACY LOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = workspaceTitle.ifBlank { "MonoDo" },
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primaryInk,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your offline tasks, notes, and birthdays are protected with your device lock.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryInk,
                textAlign = TextAlign.Center
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.danger,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onUnlockClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("unlock_monodo_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock with Fingerprint / PIN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
