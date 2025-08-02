package io.github.ismoy.belzspeedscan.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.ismoy.belzspeedscan.data.models.FlashMode

@Composable
fun FlashToggleButton(
    flashMode: FlashMode,
    iconColor: Color,
    flashIcon: ImageVector? = null,
    onToggle: () -> Unit
) {
    val icon = flashIcon ?: when (flashMode) {
        FlashMode.ON -> Icons.Default.FlashOn
        FlashMode.OFF -> Icons.Default.FlashOff
    }

    Box(modifier = Modifier.fillMaxWidth()
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onToggle() },
        contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()){
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .background(
                        Color.White.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(
                        horizontal = 30.dp,
                        vertical = 8.dp
                    ),
            ) {
                Icon(imageVector = icon, contentDescription = "Flash mode", tint = iconColor)
            }
        }
    }
} 