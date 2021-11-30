package com.qrcovid.android.presentation.main

sealed class ScanState {
    object MaskScanning: ScanState()
    object QrScanning: ScanState()
    object FinishScanning: ScanState()
}