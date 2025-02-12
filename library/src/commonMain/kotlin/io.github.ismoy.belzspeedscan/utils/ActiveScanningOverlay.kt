package io.github.ismoy.belzspeedscan.utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActiveScanningOverlay(watermark: String = "BelZSpeedScan") {
    val transition = rememberInfiniteTransition(label = "")
    val offsetFloat by transition.animateFloat(
        initialValue = 60F,
        targetValue = 0F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    val offset = offsetFloat.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Barra central fija
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(4.dp)
                .background(Color.Gray)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(40.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }

        // Barra superior
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -offset)
                .width(100.dp)
                .height(4.dp)
                .background(Color.Gray)
        )

        // Barra inferior
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = offset)
                .width(100.dp)
                .height(4.dp)
                .background(Color.Gray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = watermark,
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = FontFamily.Cursive,
            modifier = Modifier
                .padding(end = 30.dp)
                .align(Alignment.CenterEnd)
                .offset(y = 25.dp)
        )
    }
}