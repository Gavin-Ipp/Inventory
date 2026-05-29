# Inventory Manager - Lottery Ticket Scanner

An Android app for managing inventory with a focus on lottery ticket scanning and tracking.

## Features

- **QR Code Scanner**: Scan lottery ticket QR codes using the device camera
- **Code List Management**: View and manage scanned codes in a clean list interface
- **Google Sheets Integration**: Submit scanned codes to Google Sheets for tracking
- **Modern UI**: Material Design 3 interface with intuitive navigation

## Setup Instructions

### 1. Prerequisites

- Android Studio (latest version)
- Android device or emulator with camera support
- Google Cloud Project (for Google Sheets integration)

### 2. Project Setup

1. Clone or download this project
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build the project to ensure all dependencies are resolved

### 3. Permissions

The app requires the following permissions:
- **Camera**: For QR code scanning
- **Internet**: For Google Sheets integration
- **Network State**: For checking connectivity

These permissions are already declared in the AndroidManifest.xml.

### 4. Building and Running

1. Connect your Android device or start an emulator
2. Click "Run" in Android Studio
3. Select your target device
4. The app will install and launch

## Usage

### Scanning Lottery Tickets

1. Navigate to the "Lottery" tab in the bottom navigation
2. Tap "Scan QR Code" button
3. Grant camera permission if prompted
4. Point the camera at a lottery ticket QR code
5. The scanned code will appear in the list below

### Managing Codes

- **View Codes**: All scanned codes are displayed in a numbered list
- **Clear All**: Tap "Clear All" to remove all scanned codes
- **Submit to Sheets**: Tap "Submit to Google Sheets" to send codes to your spreadsheet

### Navigation

The app uses bottom navigation with four tabs:
- **Home**: Main dashboard
- **Dashboard**: Analytics and overview
- **Lottery**: QR code scanner and lottery ticket management
- **Notifications**: App notifications

## Technical Details

### Architecture
- **MVVM Pattern**: Uses ViewModel and LiveData for data management
- **Navigation Component**: For fragment navigation
- **View Binding**: For safe view access
- **RecyclerView**: For displaying the list of scanned codes

### Dependencies
- **ZXing**: For QR code scanning
- **Google Sheets API**: For spreadsheet integration
- **Material Design**: For modern UI components
- **AndroidX**: For core Android functionality

### File Structure
```
app/src/main/java/com/example/inventory/
├── MainActivity.java
├── ui/
│   ├── lottery/
│   │   ├── LotteryFragment.java
│   │   ├── LotteryViewModel.java
│   │   └── LotteryAdapter.java
│   ├── home/
│   ├── dashboard/
│   └── notifications/
└── utils/
    └── GoogleSheetsHelper.java
```

## Troubleshooting

### Common Issues

1. **Camera Permission Denied**
   - Go to Settings > Apps > Inventory Manager > Permissions
   - Enable Camera permission

2. **Google Sheets Integration Not Working**
   - Verify your Google Cloud Project setup
   - Check that the service account has access to the spreadsheet
   - Ensure the spreadsheet ID is correct

3. **Build Errors**
   - Clean and rebuild the project
   - Sync project with Gradle files
   - Check that all dependencies are properly resolved

