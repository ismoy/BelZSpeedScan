package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable

@Composable
expect fun getLocalContext(): Any?

@Composable
expect fun getLocalLifecycleOwner(): Any? 