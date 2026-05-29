package com.example.inventory.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.inventory.BuildConfig;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for Google Sheets integration
 * Supports both OAuth2 and Service Account authentication
 * Also supports Google Apps Script webhook submission
 */
public class GoogleSheetsHelper {
    
    private static final String TAG = "GoogleSheetsHelper";
    private static final String APPLICATION_NAME = "Inventory Manager";
    private static final String SPREADSHEET_ID = BuildConfig.SPREADSHEET_ID;
    private static final String RANGE = "A:A";
    private static final String PREF_NAME = "GoogleSheetsPrefs";
    private static final String KEY_SPREADSHEET_ID = "spreadsheet_id";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private static final String WEBHOOK_URL = BuildConfig.WEBHOOK_URL;
    
    private Sheets sheetsService;
    private Context context;
    private SharedPreferences preferences;
    private ExecutorService executorService;
    
    public GoogleSheetsHelper(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();
        setupSheetsService();
    }
    
    /**
     * Setup Google Sheets service
     * Tries service account first, then falls back to OAuth2
     */
    private void setupSheetsService() {
        try {
            // Try service account authentication first (more reliable)
            if (setupServiceAccountAuth()) {
                Log.d(TAG, "Service account authentication successful");
                return;
            }
            
            // Fall back to OAuth2 authentication
            if (setupOAuth2Auth()) {
                Log.d(TAG, "OAuth2 authentication successful");
                return;
            }
            
            Log.e(TAG, "Failed to setup any authentication method");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Google Sheets service", e);
        }
    }
    
    /**
     * Setup authentication using service account credentials
     * @return true if successful, false otherwise
     */
    private boolean setupServiceAccountAuth() {
        try {
            // Try to load service account credentials from assets
            InputStream credentialsStream = context.getAssets().open("service-account-credentials.json");
            
            // Load service account credentials
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            
            // Create HTTP transport
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            
            // Create Sheets service
            sheetsService = new Sheets.Builder(httpTransport, GsonFactory.getDefaultInstance(), 
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            
            credentialsStream.close();
            return true;
            
        } catch (Exception e) {
            Log.d(TAG, "Service account authentication failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Setup authentication using OAuth2 access token
     * @return true if successful, false otherwise
     */
    private boolean setupOAuth2Auth() {
        try {
            // Get stored access token
            String accessToken = preferences.getString(KEY_ACCESS_TOKEN, null);
            
            if (accessToken != null && !accessToken.equals("placeholder_token")) {
                // Create HTTP transport
                NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                
                // Create credential with access token
                GoogleCredential credential = new GoogleCredential()
                        .setAccessToken(accessToken);
                
                // Set required scopes
                credential = credential.createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
                
                // Create Sheets service
                sheetsService = new Sheets.Builder(httpTransport, GsonFactory.getDefaultInstance(), credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                
                Log.d(TAG, "OAuth2 authentication successful");
                return true;
            } else {
                Log.d(TAG, "No valid access token available");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up OAuth2 authentication", e);
            return false;
        }
    }
    
    /**
     * Set the access token from Google Sign-In
     * @param accessToken The OAuth2 access token
     */
    public void setAccessToken(String accessToken) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply();
        setupSheetsService(); // Re-setup service with new token
        Log.d(TAG, "Access token set and service reinitialized");
    }
    
    /**
     * Clear the access token (for sign out)
     */
    public void clearAccessToken() {
        preferences.edit().remove(KEY_ACCESS_TOKEN).apply();
        sheetsService = null;
        Log.d(TAG, "Access token cleared");
    }
    
    /**
     * Check if authentication is properly set up
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return sheetsService != null;
    }
    
    /**
     * Submit lottery codes via Google Apps Script webhook
     * @param codes List of lottery codes to submit
     * @param callback Callback to handle the result
     */
    public void submitLotteryCodesViaWebhook(List<String> codes, WebhookCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting webhook submission for " + codes.size() + " codes");
                
                // Clean up all codes before submission
                List<String> cleanedCodes = new ArrayList<>();
                for (String code : codes) {
                    String cleanedCode = cleanCode(code);
                    if (!cleanedCode.isEmpty()) {
                        cleanedCodes.add(cleanedCode);
                    }
                }

                if (cleanedCodes.isEmpty()) {
                    Log.w(TAG, "No valid codes to submit after cleaning");
                    callback.onFailure("No valid codes to submit");
                    return;
                }

                Log.d(TAG, "Cleaned codes: " + cleanedCodes);

                // Create the webhook payload
                String payload = createWebhookPayload(cleanedCodes);
                Log.d(TAG, "Created payload: " + payload);
                
                // Send the webhook request
                boolean success = sendWebhookRequest(payload);
                
                if (success) {
                    Log.d(TAG, "Successfully submitted " + cleanedCodes.size() + " codes via webhook");
                    callback.onSuccess(cleanedCodes.size());
                } else {
                    Log.e(TAG, "Failed to submit codes via webhook");
                    callback.onFailure("Failed to submit codes via webhook - check logs for details");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error submitting codes via webhook", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Create the webhook payload
     */
    private String createWebhookPayload(List<String> codes) {
        StringBuilder payload = new StringBuilder();
        payload.append("{\"codes\":[");
        
        for (int i = 0; i < codes.size(); i++) {
            if (i > 0) {
                payload.append(",");
            }
            // Clean each code before adding to payload
            String cleanedCode = cleanCode(codes.get(i));
            payload.append("\"").append(cleanedCode).append("\"");
        }
        
        payload.append("]}");
        
        String finalPayload = payload.toString();
        Log.d(TAG, "Created webhook payload: " + finalPayload);
        Log.d(TAG, "Payload length: " + finalPayload.length() + " characters");
        Log.d(TAG, "Number of codes: " + codes.size());
        
        return finalPayload;
    }
    
    /**
     * Send the webhook request
     */
    private boolean sendWebhookRequest(String payload) {
        try {
            Log.d(TAG, "Sending webhook request to: " + WEBHOOK_URL);
            Log.d(TAG, "Payload: " + payload);
            
            java.net.URL url = new java.net.URL(WEBHOOK_URL);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            
            // Set up the connection
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "InventoryManager/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000); // 15 seconds
            connection.setReadTimeout(15000); // 15 seconds
            
            Log.d(TAG, "Connection established, sending payload...");
            
            // Send the payload
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("UTF-8");
                os.write(input, 0, input.length);
                os.flush();
                Log.d(TAG, "Payload sent successfully");
            }
            
            // Get the response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Webhook response code: " + responseCode);
            
            // Read the response
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                            responseCode >= 200 && responseCode < 300 ? 
                            connection.getInputStream() : 
                            connection.getErrorStream(), "utf-8"))) {
                
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                Log.d(TAG, "Webhook response body: " + response.toString());
                
                // Special handling for 500 errors
                if (responseCode == 500) {
                    Log.e(TAG, "500 Internal Server Error - This indicates a problem with the Google Apps Script");
                    Log.e(TAG, "Common causes:");
                    Log.e(TAG, "1. Script is not deployed as a web app");
                    Log.e(TAG, "2. Script has syntax errors");
                    Log.e(TAG, "3. Script is trying to access a spreadsheet without proper permissions");
                    Log.e(TAG, "4. Script is expecting different data format");
                    Log.e(TAG, "Response body: " + response.toString());
                }
                
                // Special handling for 404 errors
                if (responseCode == 404) {
                    Log.e(TAG, "404 Not Found - The webhook URL is not accessible");
                    Log.e(TAG, "Common causes:");
                    Log.e(TAG, "1. Webhook URL is incorrect or has changed");
                    Log.e(TAG, "2. Google Apps Script is not deployed as a web app");
                    Log.e(TAG, "3. Script deployment was deleted or expired");
                    Log.e(TAG, "4. Script access permissions are too restrictive");
                    Log.e(TAG, "Current webhook URL: " + WEBHOOK_URL);
                    Log.e(TAG, "Response body: " + response.toString());
                    Log.e(TAG, "To fix this:");
                    Log.e(TAG, "1. Go to your Google Apps Script editor");
                    Log.e(TAG, "2. Click 'Deploy' > 'New deployment'");
                    Log.e(TAG, "3. Choose 'Web app' as the type");
                    Log.e(TAG, "4. Set 'Who has access' to 'Anyone'");
                    Log.e(TAG, "5. Copy the new webhook URL and update the app");
                }
            }
            
            boolean success = responseCode >= 200 && responseCode < 300;
            Log.d(TAG, "Webhook request " + (success ? "succeeded" : "failed") + " with response code: " + responseCode);
            
            return success;
            
        } catch (java.net.ConnectException e) {
            Log.e(TAG, "Connection failed - check internet connection and webhook URL", e);
            return false;
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Request timed out - webhook may be slow or unresponsive", e);
            return false;
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Unknown host - check webhook URL", e);
            return false;
        } catch (java.io.IOException e) {
            Log.e(TAG, "IO error sending webhook request", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error sending webhook request", e);
            return false;
        }
    }
    
    /**
     * Submit lottery codes to Google Sheets
     * @param codes List of lottery codes to submit
     * @return true if successful, false otherwise
     */
    public boolean submitLotteryCodes(List<String> codes) {
        try {
            if (sheetsService == null) {
                Log.e(TAG, "Sheets service not initialized - authentication failed");
                return false;
            }

            // Clean up all codes before submission
            List<String> cleanedCodes = new ArrayList<>();
            for (String code : codes) {
                String cleanedCode = cleanCode(code);
                if (!cleanedCode.isEmpty()) {
                    cleanedCodes.add(cleanedCode);
                }
            }

            // Prepare the data for submission
            ValueRange body = new ValueRange();
            List<List<Object>> values = new ArrayList<>();
            for (String code : cleanedCodes) {
                values.add(Arrays.asList((Object) code)); // Create a list for each code (row)
            }
            body.setValues(values);

            Log.d(TAG, "Attempting to submit " + cleanedCodes.size() + " cleaned codes to spreadsheet: " + getSpreadsheetId());

            // Append the data to the spreadsheet
            sheetsService.spreadsheets().values()
                    .append(getSpreadsheetId(), RANGE, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
            
            Log.d(TAG, "Successfully submitted " + cleanedCodes.size() + " codes to Google Sheets");
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error submitting codes to Google Sheets", e);
            
            // Provide more specific error information
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("403")) {
                    Log.e(TAG, "Permission denied. Please share the spreadsheet with: inventory-manager-service-acco@gleaming-advice-469817-v4.iam.gserviceaccount.com");
                } else if (errorMessage.contains("404")) {
                    Log.e(TAG, "Spreadsheet not found. Check if the spreadsheet ID is correct: " + getSpreadsheetId());
                } else if (errorMessage.contains("400")) {
                    Log.e(TAG, "Bad request. Check if the Google Sheets API is enabled in your project.");
                } else if (errorMessage.contains("Unable to parse range")) {
                    Log.e(TAG, "Range parsing error. The app is now using 'A:A' range which should work with any sheet.");
                    // Try to get available sheet names
                    List<String> sheetNames = getSheetNames();
                    if (sheetNames != null) {
                        Log.e(TAG, "Available sheets in your spreadsheet: " + sheetNames);
                    }
                } else {
                    Log.e(TAG, "Network or API error: " + errorMessage);
                }
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error submitting codes to Google Sheets", e);
            return false;
        }
    }

    /**
     * Clean up a code by removing all whitespace, newlines, and formatting characters
     */
    private String cleanCode(String code) {
        if (code == null) {
            return "";
        }
        
        // Remove all whitespace characters
        String cleaned = code.replaceAll("\\s+", "");
        
        // Remove common scanner delimiters and formatting
        cleaned = cleaned.replaceAll("[\\n\\r\\t]", "");
        
        // Remove any other control characters
        cleaned = cleaned.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // Remove leading and trailing whitespace
        cleaned = cleaned.replaceAll("^\\s+|\\s+$", "");
        
        return cleaned;
    }
    
    /**
     * Get the current spreadsheet ID
     * @return The spreadsheet ID
     */
    public String getSpreadsheetId() {
        return preferences.getString(KEY_SPREADSHEET_ID, SPREADSHEET_ID);
    }
    
    /**
     * Set the spreadsheet ID
     * @param spreadsheetId The Google Sheets spreadsheet ID
     */
    public void setSpreadsheetId(String spreadsheetId) {
        preferences.edit().putString(KEY_SPREADSHEET_ID, spreadsheetId).apply();
        Log.d(TAG, "Spreadsheet ID set to: " + spreadsheetId);
    }
    
    /**
     * Get available sheet names from the spreadsheet
     * @return List of sheet names, or null if error
     */
    public List<String> getSheetNames() {
        try {
            if (sheetsService == null) {
                Log.e(TAG, "Sheets service not initialized");
                return null;
            }
            
            // Get spreadsheet metadata
            var spreadsheet = sheetsService.spreadsheets().get(getSpreadsheetId()).execute();
            List<String> sheetNames = new ArrayList<>();
            
            for (var sheet : spreadsheet.getSheets()) {
                sheetNames.add(sheet.getProperties().getTitle());
            }
            
            Log.d(TAG, "Available sheets: " + sheetNames);
            return sheetNames;
            
        } catch (IOException e) {
            Log.e(TAG, "Error getting sheet names", e);
            return null;
        }
    }
    
    /**
     * Test the connection to Google Sheets
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            if (sheetsService == null) {
                Log.e(TAG, "Sheets service not initialized");
                return false;
            }
            
            Log.d(TAG, "Testing connection to spreadsheet: " + getSpreadsheetId());
            
            // Try to read a small range to test the connection
            sheetsService.spreadsheets().values()
                    .get(getSpreadsheetId(), "A1:A1")
                    .execute();
            
            Log.d(TAG, "Google Sheets connection test successful");
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Google Sheets connection test failed", e);
            
            // Provide more specific error information
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("403")) {
                    Log.e(TAG, "Permission denied. Please share the spreadsheet with: inventory-manager-service-acco@gleaming-advice-469817-v4.iam.gserviceaccount.com");
                } else if (errorMessage.contains("404")) {
                    Log.e(TAG, "Spreadsheet not found. Check if the spreadsheet ID is correct: " + getSpreadsheetId());
                } else if (errorMessage.contains("400")) {
                    Log.e(TAG, "Bad request. Check if the Google Sheets API is enabled in your project.");
                } else if (errorMessage.contains("Unable to parse range")) {
                    Log.e(TAG, "Range parsing error. Check if the sheet name exists in your spreadsheet.");
                    // Try to get available sheet names
                    List<String> sheetNames = getSheetNames();
                    if (sheetNames != null) {
                        Log.e(TAG, "Available sheets in your spreadsheet: " + sheetNames);
                    }
                } else {
                    Log.e(TAG, "Network or API error: " + errorMessage);
                }
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error testing Google Sheets connection", e);
            return false;
        }
    }
    
    /**
     * Callback interface for webhook submission
     */
    public interface WebhookCallback {
        void onSuccess(int codesSubmitted);
        void onFailure(String error);
    }
}
