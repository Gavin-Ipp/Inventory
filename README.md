# Inventory Manager

An Android inventory management system that uses barcode scanning to track inventory items and report data to Google Sheets.

## Features

- **Barcode Scanning**: Scan inventory item barcodes using the device camera
- **Inventory Tracking**: View and manage scanned items in a clean list interface
- **Duplicate & Conflict Detection**: Automatically flags duplicate scans and item conflicts
- **Google Sheets Reporting**: Submit inventory data to Google Sheets via webhook for reporting
- **Shift Reports**: Generate open/close shift reports with drawer counts
- **Modern UI**: Material Design 3 interface with bottom navigation

## Setup

### Prerequisites

- Android Studio (latest version)
- Android device or emulator with camera support
- A deployed Google Apps Script webhook (see `webhook_handler.gs`)

### Configuration

1. Clone the repo and open it in Android Studio
2. Copy `secrets.properties.example` to `secrets.properties`
3. Fill in your webhook URL in `secrets.properties`:
   ```
   WEBHOOK_URL=https://script.google.com/macros/s/.../exec
   ```
4. Sync Gradle and build

### Permissions

Declared in `AndroidManifest.xml`:
- **Camera** — barcode scanning
- **Internet** — Google Sheets webhook submission

## Usage

### Scanning Inventory

1. Navigate to the **Inventory** tab in the bottom navigation
2. Point the device camera at an item barcode — codes are captured automatically
3. Scanned items appear in a numbered list with parsed item details
4. Use **Clear Selected** (checkbox each item) or **Clear All** to remove entries

### Submitting a Report

1. Review the scanned list; resolve any duplicate or conflict warnings
2. Tap **Save** to confirm the inventory count
3. Tap **Submit Report** to push the data to Google Sheets

### Shift Reports

Navigate to the **Home** tab to fill in open/close shift details and drawer counts before submitting.

## Architecture

- **MVVM** — `ViewModel` + `LiveData` as the single source of truth; fragments only observe and dispatch actions
- **Navigation Component** — single-activity, fragment-based navigation
- **View Binding** — type-safe view access
- **Webhook submission** — plain `HttpURLConnection` POST to a Google Apps Script web app; no Google API client library required

## File Structure

```
app/src/main/java/com/example/inventory/
├── MainActivity.java
├── ui/
│   ├── lottery/          # Inventory scanning screen
│   │   ├── LotteryFragment.java
│   │   ├── LotteryViewModel.java
│   │   └── LotteryAdapter.java
│   ├── home/             # Shift report screen
│   ├── dashboard/
│   ├── notifications/
│   └── shared/
│       └── ReportViewModel.java
└── utils/
    ├── GoogleSheetsHelper.java   # Webhook submission
    └── LotteryTicketParser.java  # Barcode parsing
```

## Troubleshooting

**Camera permission denied** — Settings → Apps → Inventory Manager → Permissions → enable Camera.

**Submission fails** — confirm your Google Apps Script is deployed as a Web App with access set to *Anyone*, and that the URL in `secrets.properties` matches the current deployment.

**Build errors** — run *File → Sync Project with Gradle Files*, then *Build → Clean Project*.
