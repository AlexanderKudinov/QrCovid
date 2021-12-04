package com.qrcovid.android.presentation.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qrcovid.android.data.MainRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    private val savedState: SavedStateHandle
): ViewModel() {

    private val repository = MainRepository()

    private val _scanState = MutableStateFlow<ScanState>(
        savedState.get(SCAN_STATE) ?: ScanState.Default
    )
    val scanState: StateFlow<ScanState> = _scanState

    private val _errorState = MutableStateFlow<ErrorState>(ErrorState.Default)
    val errorState: StateFlow<ErrorState> = _errorState

    private val errorHandler = CoroutineExceptionHandler { context, exception ->
        println("Exception: " + exception.message)
    }

    init {

    }

    override fun onCleared() {
        savedState.set(SCAN_STATE, scanState.value)
        super.onCleared()
    }

    fun back() {
        when (_scanState.value) {
            ScanState.MaskScanning -> _scanState.value = ScanState.MaskScanning
            ScanState.QrScanning -> _scanState.value = ScanState.MaskScanning
            ScanState.FinishScanning -> _scanState.value = ScanState.QrScanning
        }
    }

    fun finish() {
        _scanState.value = ScanState.FinishScanning
    }

    fun openMask() {
        _scanState.value = ScanState.MaskScanning
    }

    fun openQr() {
        _scanState.value = ScanState.QrScanning
    }


    fun setDefaultErrorState() {
        _errorState.value = ErrorState.Default
    }

    fun grantPermissions(permissions: Map<String, Boolean>) {
        var grantedAllPermissions = true
        permissions.entries.forEach { (permission, granted) ->
            if (!granted) {
                grantedAllPermissions = false
            }
        }
        if (grantedAllPermissions) {
            _scanState.value = ScanState.MaskScanning
        } else {
            _scanState.value = ScanState.Default
            _errorState.value = ErrorState.PermissionError
        }
    }

    fun onMaskSuccess() {

    }

    fun onMaskFailure() {

    }

    fun onQrSuccess() {

    }

    fun onQrFailure() {

    }


    companion object {
        private const val SCAN_STATE = "SCAN_STATE"
    }
}