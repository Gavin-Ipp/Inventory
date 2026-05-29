package com.example.inventory.ui.lottery;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.inventory.BuildConfig;
import com.example.inventory.GoogleSignInActivity;
import com.example.inventory.utils.GoogleSheetsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LotteryViewModel extends AndroidViewModel {

    private static final String TAG = "LotteryViewModel";
    private final MutableLiveData<List<String>> lotteryCodes;
    private final MutableLiveData<SubmitStatus> submitStatus;
    private final MutableLiveData<Boolean> needsSignIn;
    private GoogleSheetsHelper googleSheetsHelper;

    public enum SubmitStatus {
        IDLE, LOADING, SUCCESS, ERROR
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public LotteryViewModel(@NonNull Application application) {
        super(application);
        lotteryCodes = new MutableLiveData<>(new ArrayList<>());
        submitStatus = new MutableLiveData<>(SubmitStatus.IDLE);
        needsSignIn = new MutableLiveData<>(false);

        // Initialize Google Sheets Helper
        googleSheetsHelper = new GoogleSheetsHelper(application);
        
        googleSheetsHelper.setSpreadsheetId(BuildConfig.SPREADSHEET_ID);
        
        // Check authentication status
        checkAuthenticationStatus();
    }

    public LiveData<List<String>> getLotteryCodes() {
        return lotteryCodes;
    }

    public LiveData<SubmitStatus> getSubmitStatus() {
        return submitStatus;
    }
    
    public LiveData<Boolean> getNeedsSignIn() {
        return needsSignIn;
    }

    public void addLotteryCode(String code) {
        List<String> currentCodes = lotteryCodes.getValue();
        if (currentCodes == null) {
            currentCodes = new ArrayList<>();
        }
        
        // Allow duplicate codes to be added for testing duplicate detection
        currentCodes.add(code);
        lotteryCodes.setValue(currentCodes);
        Log.d(TAG, "Added lottery code: " + code + ", total codes: " + currentCodes.size());
    }

    public void updateCodes(List<String> codes) {
        lotteryCodes.setValue(codes != null ? codes : new ArrayList<>());
    }

    public void clearCodes() {
        lotteryCodes.setValue(new ArrayList<>());
    }

    public void removeDuplicates() {
        List<String> currentCodes = lotteryCodes.getValue();
        if (currentCodes != null) {
            // Remove duplicates while preserving order
            List<String> uniqueCodes = new ArrayList<>();
            Set<String> seenCodes = new HashSet<>();
            
            for (String code : currentCodes) {
                if (!seenCodes.contains(code)) {
                    uniqueCodes.add(code);
                    seenCodes.add(code);
                }
            }
            
            lotteryCodes.setValue(uniqueCodes);
        }
    }

    public void removeInvalidCodes() {
        List<String> currentCodes = lotteryCodes.getValue();
        if (currentCodes != null) {
            // Remove codes that are less than 15 digits
            List<String> validCodes = new ArrayList<>();
            
            for (String code : currentCodes) {
                if (code.length() >= 15) {
                    validCodes.add(code);
                }
            }
            
            lotteryCodes.setValue(validCodes);
        }
    }

    public void submitToGoogleSheets(List<String> codes) {
        submitStatus.setValue(SubmitStatus.LOADING);
        executor.execute(() -> {
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            googleSheetsHelper.submitLotteryCodesViaWebhook(codes, new GoogleSheetsHelper.WebhookCallback() {
                @Override
                public void onSuccess(int codesSubmitted) {
                    success[0] = true;
                    latch.countDown();
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Webhook submission failed: " + error);
                    latch.countDown();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Submission interrupted", e);
            }

            mainHandler.post(() -> {
                if (success[0]) {
                    submitStatus.setValue(SubmitStatus.SUCCESS);
                    clearCodes();
                } else {
                    submitStatus.setValue(SubmitStatus.ERROR);
                }
            });
        });
    }
    
    /**
     * Set the access token from Google Sign-In
     * @param accessToken The OAuth2 access token
     */
    public void setAccessToken(String accessToken) {
        googleSheetsHelper.setAccessToken(accessToken);
        checkAuthenticationStatus();
    }
    
    /**
     * Check authentication status and update UI accordingly
     */
    private void checkAuthenticationStatus() {
        boolean authenticated = googleSheetsHelper.isAuthenticated();
        needsSignIn.setValue(!authenticated);
        
        if (authenticated) {
            // Test the connection on initialization
            testGoogleSheetsConnection();
        }
    }
    
    /**
     * Test the Google Sheets connection
     */
    public void testGoogleSheetsConnection() {
        executor.execute(() -> {
            boolean success = googleSheetsHelper.testConnection();
            if (success) {
                Log.d(TAG, "Google Sheets connection test successful");
            } else {
                Log.e(TAG, "Google Sheets connection test failed");
            }
        });
    }
    
    /**
     * Set the spreadsheet ID
     * @param spreadsheetId The Google Sheets spreadsheet ID
     */
    public void setSpreadsheetId(String spreadsheetId) {
        googleSheetsHelper.setSpreadsheetId(spreadsheetId);
    }
    
    /**
     * Check if Google Sheets is properly authenticated
     * @return true if authenticated, false otherwise
     */
    public boolean isGoogleSheetsAuthenticated() {
        return googleSheetsHelper.isAuthenticated();
    }
    
    /**
     * Get intent for Google Sign-In activity
     * @return Intent for GoogleSignInActivity
     */
    public Intent getSignInIntent() {
        return new Intent(getApplication(), GoogleSignInActivity.class);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
