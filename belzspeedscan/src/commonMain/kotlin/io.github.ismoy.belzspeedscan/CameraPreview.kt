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
    tooFarText:String? = "Bring the code closer to the camera\n"+"Distance too far",
    tooCloseText:String? = "Move the code away from the camera \n" + "Too close",
    tooOptimalText:String? = "Perfect distance!\n" + "Keep the code with in the frame",
    tooDarkText: String? = "Not enough light\nMove to a brighter area",
    tooBrightText: String? = "Too much reflection\nReduce direct light on the code",
    adjustAngleText: String? = "Reflections detected\nTry scanning from a different angle"
)