package com.qrcovid.android.presentation.main

import androidx.lifecycle.ViewModel
import com.qrcovid.android.data.MainRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel: ViewModel() {

    private val repository = MainRepository()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.MaskScanning)
    val scanState: StateFlow<ScanState> = _scanState

    private val errorHandler = CoroutineExceptionHandler { context, exception ->
        println("Exception: " + exception.message)
    }

    init {

    }
}