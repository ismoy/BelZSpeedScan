package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.ismoy.belzspeedscan.domain.CodeScanner

@Composable
expect fun CameraPreview(
    onPreviewViewReady: (Any) -> Unit,
    scanner:CodeScanner?,
    modifier: Modifier,
    waterMark:String? = "BelZSpeedScan",
    tooFarColor: Color? = Color.Red,
    tooCloseColor: Color? = Color.Red,
    tooOptimalColor: Color? = Color.Green,
    tooFarText:String? = "Acerca el código a la cámara\n"+"Distancia muy lejana",
    tooCloseText:String? = "Aleja el código de la cámara\n" + "Demasiado cerca",
    tooOptimalText:String? = "¡Distancia perfecta!\n" + "Mantenga el código dentro del marco"
)