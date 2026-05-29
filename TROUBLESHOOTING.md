# Troubleshooting Google Sheets Submission Errors

## Common Error Codes and Solutions

### 403 Error - Permission Denied
**Problem:** The service account doesn't have access to your Google Sheet.

**Solution:**
1. Open your Google Sheet: `1d3cGGrE6oDFJIY_xJ7nCLIsPMclfDE8w64RL9-i7jEU`
2. Click the **"Share"** button in the top right
3. Add this email: `inventory-manager-service-acco@gleaming-advice-469817-v4.iam.gserviceaccount.com`
4. Give it **"Editor"** permissions
5. Click **"Send"** (uncheck "Notify people")

### 404 Error - Spreadsheet Not Found
**Problem:** The spreadsheet ID is incorrect or the sheet doesn't exist.

**Solution:**
1. Verify your spreadsheet ID: `1d3cGGrE6oDFJIY_xJ7nCLIsPMclfDE8w64RL9-i7jEU`
2. Make sure the spreadsheet exists and is accessible
3. Check that the ID is copied correctly (no extra spaces)

### 400 Error - Bad Request
**Problem:** Google Sheets API is not enabled or there's a configuration issue.

**Solution:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project: `gleaming-advice-469817-v4`
3. Go to "APIs & Services" > "Library"
4. Search for "Google Sheets API"
5. Click on it and press "Enable"

## Debugging Steps

### Step 1: Check Logcat
Look for these log messages in Android Studio:
- `"Service account authentication successful"`
- `"Testing connection to spreadsheet: [ID]"`
- `"Google Sheets connection test successful"`
- `"Attempting to submit [X] codes to spreadsheet: [ID]"`

### Step 2: Test Connection
The app automatically tests the connection on startup. Check if you see:
- `"Google Sheets connection test successful"` - Connection works
- `"Google Sheets connection test failed"` - There's an issue

### Step 3: Verify Setup
1. **Service Account Credentials**: Check that `service-account-credentials.json` is in `app/src/main/assets/`
2. **Spreadsheet ID**: Verify it's set to `1d3cGGrE6oDFJIY_xJ7nCLIsPMclfDE8w64RL9-i7jEU`
3. **Google Sheet Sharing**: Make sure the service account email has Editor access

## Quick Test

1. **Add some test codes** using the "Add Code" button
2. **Try submitting** to Google Sheets
3. **Check logcat** for specific error messages
4. **Look for the error code** (403, 404, 400, etc.)

## Common Issues

### Issue: "Sheets service not initialized"
**Cause:** Service account authentication failed
**Solution:** Check that the credentials file is properly placed in assets folder

### Issue: "Permission denied"
**Cause:** Service account doesn't have access to the sheet
**Solution:** Share the spreadsheet with the service account email

### Issue: "Spreadsheet not found"
**Cause:** Wrong spreadsheet ID
**Solution:** Verify the spreadsheet ID is correct

### Issue: "Network or API error"
**Cause:** Google Sheets API not enabled
**Solution:** Enable Google Sheets API in Google Cloud Console

## Getting Help

If you're still having issues:
1. **Check the logcat** for specific error messages
2. **Note the error code** (403, 404, 400, etc.)
3. **Verify all setup steps** above
4. **Try the manual test** with the "Add Code" button first
