package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.ismoy.belzspeedscan.domain.CodeScanner

@Composable
actual fun CameraPreview(
    onPreviewViewReady: (Any) -> Unit,
    scanner: CodeScanner?,
    modifier: Modifier,
    waterMark: String?,
    tooFarColor: Color?,
    tooCloseColor: Color?,
    tooOptimalColor: Color?,
    tooFarText: String?,
    tooCloseText: String?,
    tooOptimalText: String?
) {
}