package com.codegalaxy.barcodescanner.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codegalaxy.barcodescanner.model.BarModel
import com.codegalaxy.barcodescanner.BarScanState
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class BarCodeScannerViewModel : ViewModel() {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    private var _barScanState by mutableStateOf<BarScanState>(BarScanState.Ideal)
    val barScanState: BarScanState get() = _barScanState

    fun onBarCodeDetected(barcodes: List<Barcode>) {
        viewModelScope.launch {
            if (barcodes.isEmpty()) {
                _barScanState = BarScanState.Error("No barcode detected")
                return@launch
            }

            _barScanState = BarScanState.Loading

            barcodes.forEach { barcode ->
                barcode.rawValue?.let { barcodeValue ->
                    try {
                        val barModel: BarModel = jsonParser.decodeFromString(barcodeValue)
                        _barScanState = BarScanState.ScanSuccess(barStateModel = barModel)
                    } catch (e: Exception) {
                        Log.i("BarCodeScanner", "onBarCodeDetected: $e")
                        _barScanState = BarScanState.Error("Invalid JSON format in barcode")
                    }
                    return@launch
                }
            }
            _barScanState = BarScanState.Error("No valid barcode value")
        }
    }

    fun resetState() {
        _barScanState = BarScanState.Ideal
    }
}