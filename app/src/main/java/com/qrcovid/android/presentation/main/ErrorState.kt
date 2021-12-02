package com.qrcovid.android.presentation.main

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


sealed class ErrorState: Parcelable {
    @Parcelize
    object Default: ErrorState()
    @Parcelize
    object PermissionError: ErrorState()
}