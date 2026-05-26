package com.vibelock.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vibelock.ui.theme.DrawColors
import com.vibelock.ui.theme.VibePurple

@Composable
fun ColorPicker(
    selectedColor: Long,
    strokeWidth: Float,
    onColorSelected: (Long) -> Unit,
    onStrokeSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Color swatches
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(DrawColors) { color ->
                val isSelected = color.value == selectedColor
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 36.dp else 30.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorSelected(color.value) },
                )
            }
        }

        // Stroke width slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            )
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeSelected,
                valueRange = 3f..30f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = VibePurple,
                    activeTrackColor = VibePurple,
                ),
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}
