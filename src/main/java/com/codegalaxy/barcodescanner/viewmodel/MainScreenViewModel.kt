package com.codegalaxy.barcodescanner.ui.screen // Make sure this package matches your project structure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codegalaxy.barcodescanner.ui.theme.BarCodeScannerTheme // Replace with your actual theme if different

@OptIn(ExperimentalMaterial3Api::class) // For TopAppBar
@Composable
fun MainScreen(
    onStokearClicked: () -> Unit // Callback for when the button is clicked
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Stock Management") }) // Optional: Add a title
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from Scaffold
                .padding(16.dp), // Additional padding for content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onStokearClicked,
                modifier = Modifier
                    .width(200.dp) // Set a fixed width for the button
                    .aspectRatio(1f) // Make the button square (height will match width)
            ) {
                Text(
                    text = "Stokear",
                    style = MaterialTheme.typography.headlineMedium // Make text larger
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StockScreenPreview() {
    BarCodeScannerTheme { // Use your app's theme for the preview
        MainScreen(onStokearClicked = {
            // This is a lambda for preview, does nothing in the actual app
            println("Stokear button clicked in preview")
        })
    }
}