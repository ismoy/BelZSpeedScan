package io.github.ismoy.belzspeedscan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.domain.CodeScanner

@Composable
actual fun PlatformCameraPreview(
    onPreviewViewReady: (Any) -> Unit,
    scanner: CodeScanner?,
    modifier: Modifier,
    config: ScannerConfig,
    customOverlay: @Composable (() -> Unit)?,
    customPermissionDialog: @Composable (() -> Unit)?
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        if (customOverlay != null) {
            customOverlay()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.h1,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Camera Scanning",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Not Available on Desktop",
                    style = MaterialTheme.typography.h6,
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Barcode scanning with camera is not yet supported on JVM/Desktop platform.\n\nFor full camera functionality, please use:\n• Android\n• iOS\n\nWe're working on desktop camera support for future releases.",
                    style = MaterialTheme.typography.body1,
                    color = Color(0xFFB0B0B0),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.body1.lineHeight * 1.4
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Watermark: ${config.watermark}",
                    style = MaterialTheme.typography.caption,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    // Notify that "preview" is ready (placeholder)
    onPreviewViewReady("jvm-placeholder-view")
}