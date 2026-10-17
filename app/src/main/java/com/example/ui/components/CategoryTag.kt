package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CategoryTheme

@Composable
fun CategoryTag(
    category: String,
    modifier: Modifier = Modifier
) {
    val style = CategoryTheme.getStyle(category)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(style.backgroundColor)
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = style.textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}
