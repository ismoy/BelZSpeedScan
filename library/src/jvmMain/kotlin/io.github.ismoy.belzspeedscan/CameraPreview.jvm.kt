package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.ismoy.belzspeedscan.domain.CodeScanner

@Composable
actual fun CameraPreview(
    onPreviewViewReady: (Any) -> Unit,
    scanner: CodeScanner?,
    modifier: Modifier
) {
}