package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
actual fun getLocalContext(): Any? = LocalContext.current

@Composable
actual fun getLocalLifecycleOwner(): Any? = LocalLifecycleOwner.current 