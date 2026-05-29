# Google Cloud Setup Guide - Fix 403 Error

The 403 error indicates that your Google Cloud Project is not properly configured for OAuth2 authentication. Follow these steps to fix it:

## Step 1: Configure Google Cloud Project

### 1.1 Go to Google Cloud Console
1. Visit [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project: `gleaming-advice-469817-v4`

### 1.2 Enable Google Sheets API
1. Go to "APIs & Services" > "Library"
2. Search for "Google Sheets API"
3. Click on it and press "Enable"

### 1.3 Configure OAuth Consent Screen
1. Go to "APIs & Services" > "OAuth consent screen"
2. Choose "External" user type
3. Fill in the required information:
   - App name: "Inventory Manager"
   - User support email: Your email
   - Developer contact information: Your email
4. Click "Save and Continue"
5. Skip "Scopes" section, click "Save and Continue"
6. Add test users (your email address)
7. Click "Save and Continue"

### 1.4 Update OAuth2 Credentials
1. Go to "APIs & Services" > "Credentials"
2. Find your OAuth 2.0 Client ID
3. Click on it to edit
4. Add your package name: `com.example.inventory`
5. Add your SHA-1 fingerprint (see Step 2 below)
6. Click "Save"

## Step 2: Get Your SHA-1 Fingerprint

### For Debug Builds:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### For Release Builds:
```bash
keytool -list -v -keystore your-release-keystore.jks -alias your-key-alias
```

Copy the SHA-1 fingerprint and add it to your OAuth2 credentials.

## Step 3: Update Your App Configuration

### 3.1 Update GoogleSignInActivity
The activity has been updated to include your client ID. Make sure it matches your OAuth2 credentials.

### 3.2 Test the Configuration
1. Clean and rebuild your project
2. Uninstall and reinstall the app
3. Try signing in again

## Step 4: Alternative - Use Service Account (Recommended)

If OAuth2 continues to have issues, consider using a service account instead:

### 4.1 Create Service Account
1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "Service Account"
3. Name: "Inventory Manager Service Account"
4. Click "Create and Continue"
5. Skip optional steps, click "Done"

### 4.2 Generate Service Account Key
1. Click on your service account
2. Go to "Keys" tab
3. Click "Add Key" > "Create new key"
4. Choose "JSON" format
5. Download the file

### 4.3 Add to Your App
1. Rename the file to `service-account-credentials.json`
2. Place it in `app/src/main/assets/`
3. Update GoogleSheetsHelper to use service account authentication

## Step 5: Troubleshooting

### Common Issues:

1. **403 Error**: Usually means OAuth consent screen or credentials are not configured properly
2. **Package name mismatch**: Ensure package name in OAuth2 credentials matches your app
3. **SHA-1 fingerprint**: Must be correct for your keystore
4. **Test users**: Add your email to test users in OAuth consent screen

### Debug Steps:
1. Check logcat for detailed error messages
2. Verify all configuration steps above
3. Try with a different Google account
4. Clear app data and try again

## Step 6: Production Considerations

For production apps:
1. Complete OAuth consent screen verification
2. Use proper release keystore SHA-1
3. Consider using service account for server-to-server communication
4. Implement proper token refresh logic

## Quick Fix for Testing

If you want to test quickly, you can temporarily use a service account:

1. Create service account credentials as described above
2. Share your Google Sheet with the service account email
3. Use service account authentication instead of OAuth2

This will bypass the OAuth2 configuration issues and allow you to test the Google Sheets integration immediately.
