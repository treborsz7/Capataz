package com.thinkthat.mamusckascaner

import com.thinkthat.mamusckascaner.model.BarModel

sealed interface BarScanState {
    data object Ideal : BarScanState
    data class ScanSuccess(
        val barStateModel: BarModel? = null,
        val rawValue: String? = null,
        val format: String? = null
    ) : BarScanState
    data class Error(val error: String) : BarScanState
    data object Loading : BarScanState
}
