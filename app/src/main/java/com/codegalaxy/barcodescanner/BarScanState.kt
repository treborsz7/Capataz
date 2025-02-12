package com.codegalaxy.barcodescanner

import com.codegalaxy.barcodescanner.model.BarModel

sealed interface BarScanState {
    data object Ideal : BarScanState
    data class ScanSuccess(val barStateModel: BarModel) : BarScanState
    data class Error(val error: String) : BarScanState
    data object Loading : BarScanState
}