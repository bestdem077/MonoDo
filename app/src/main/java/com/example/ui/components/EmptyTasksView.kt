package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SlateGraphite

@Composable
fun EmptyTasksView(
    message: String = "All tasks completed.",
    subMessage: String = "Clear space. Clear mind.",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Minimalist geometric zen artwork
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(90.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                // Outer subtle circle
                drawCircle(
                    color = Color(0xFFE2E2DC),
                    radius = size.width / 2 - 2f,
                    style = Stroke(width = 2f)
                )
                // Middle concentric ring
                drawCircle(
                    color = Color(0xFFD4D4CE),
                    radius = size.width / 3,
                    style = Stroke(width = 1.5f)
                )
                // Center clean checkmark
                val checkPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.35f, size.height * 0.50f)
                    lineTo(size.width * 0.46f, size.height * 0.62f)
                    lineTo(size.width * 0.68f, size.height * 0.38f)
                }
                drawPath(
                    path = checkPath,
                    color = ObsidianBlack,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = ObsidianBlack,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGraphite,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )
    }
}
