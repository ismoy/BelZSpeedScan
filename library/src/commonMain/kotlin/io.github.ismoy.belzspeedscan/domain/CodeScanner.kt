package io.github.ismoy.belzspeedscan.domain

interface CodeScanner {
    fun startScanning()
    fun stopScanning()
    fun pauseScanning()
    fun resumeScanning()
}