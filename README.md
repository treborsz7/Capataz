# Barcode Scanner App

A modern Android application built with Jetpack Compose that scans both QR codes and various barcode formats. The app supports JSON-formatted QR codes for invoice data and standard barcodes, providing a clean and user-friendly interface.

## Features

- Supports multiple barcode formats:
  - QR Code
  - Aztec
  - Codabar
  - Code 39
  - Code 93
  - Code 128
  - Data Matrix
  - EAN-8
  - EAN-13
  - ITF
  - PDF417
  - UPC-A
  - UPC-E

- Real-time barcode scanning
- JSON QR code parsing for invoice data
- Clean Material Design 3 UI
- Proper camera permission handling
- Automatic camera cleanup
- Error handling and user feedback

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Barcode Scanning**: ML Kit
- **Camera**: CameraX
- **JSON Parsing**: KotlinX Serialization
- **Minimum SDK**: 26 (Android 8.0)

## Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/barcode-scanner.git
```

2. Open the project in Android Studio Arctic Fox or later.

3. Make sure you have the following dependencies in your app's build.gradle:
```gradle
dependencies {
    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    
    // CameraX
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    
    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}
```

4. Add camera permission to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

## JSON Format for QR Codes

The app expects QR codes containing invoice data to be in the following JSON format:
```json
{
    "invoiceNumber": "INV12345",
    "client": {
        "name": "John Doe",
        "email": "john.doe@example.com",
        "address": "123 Main St, Cityville, Country"
    },
    "purchase": [
        {
            "item": "Laptop",
            "quantity": 1,
            "price": 1000
        },
        {
            "item": "Mouse",
            "quantity": 2,
            "price": 25
        }
    ],
    "totalAmount": 1050
}
```

## Architecture

The app follows the MVVM architecture pattern:

- **Model**: Data classes for invoice data (BarModel, Client, PurchaseItem)
- **View**: Compose UI components (BarcodeScannerScreen, CameraPreview)
- **ViewModel**: BarCodeScannerViewModel for handling business logic and state management
- **State**: BarScanState for managing UI states

## Usage

1. Launch the app
2. Grant camera permission when prompted
3. Point the camera at a barcode or QR code
4. The app will automatically detect and process the code:
   - For JSON QR codes: Displays invoice details
   - For regular barcodes: Shows the format and raw value
5. Click "Scan Another" to scan more codes

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request




https://github.com/user-attachments/assets/7b20257b-dbed-468c-9242-1b849d6d9cc5

### Contact - Let's become friend
- [Github](https://github.com/cheetahmail007)
- [Linkedin](https://www.linkedin.com/in/myofficework/)
- [MEDIUM](https://medium.com/@myofficework000)

<p>
Don't forget to star ⭐ the repo it motivates me to share more open source
</p>

