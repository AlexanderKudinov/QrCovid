package com.qrcovid.android.presentation.main

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class ScanState: Parcelable {
    @Parcelize
    object Default: ScanState()
    @Parcelize
    object MaskScanning: ScanState()
    @Parcelize
    object QrScanning: ScanState()
    @Parcelize
    object FinishScanning: ScanState()
    @Parcelize
    object Error: ScanState()
}