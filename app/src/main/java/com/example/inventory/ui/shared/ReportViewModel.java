package com.example.inventory.ui.shared;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.inventory.data.ReportData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared ViewModel to manage unified report data across all fragments
 */
public class ReportViewModel extends ViewModel {
    
    private final MutableLiveData<ReportData> reportData = new MutableLiveData<>(new ReportData());
    private final MutableLiveData<Boolean> isSubmitting = new MutableLiveData<>(false);
    private final MutableLiveData<String> submitStatus = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> shouldToggleShiftType = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> clearShiftReportFields = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> clearTobaccoFields = new MutableLiveData<>(false);
    
    public ReportViewModel() {
        // Initialize with empty report data
        reportData.setValue(new ReportData());
    }
    
    /**
     * Get the current report data
     */
    public LiveData<ReportData> getReportData() {
        return reportData;
    }
    
    /**
     * Get the current report data value (non-LiveData)
     */
    public ReportData getCurrentReportData() {
        return reportData.getValue();
    }
    
    /**
     * Save shift report data
     */
    public void saveShiftReportData(Map<String, String> safeOpenValues, 
                                   Map<String, String> safeCloseValues,
                                   Map<String, String> drawerOpenValues,
                                   Map<String, String> drawerCloseValues,
                                   Map<String, String> safeDrops,
                                   String employeeName,
                                   String drawerTotal,
                                   String amountOwed,
                                   String date,
                                   String shiftType) {
        ReportData current = reportData.getValue();
        if (current != null) {
            current.shiftReport.safeOpenValues = safeOpenValues;
            current.shiftReport.safeCloseValues = safeCloseValues;
            current.shiftReport.drawerOpenValues = drawerOpenValues;
            current.shiftReport.drawerCloseValues = drawerCloseValues;
            current.shiftReport.safeDrops = safeDrops;
            current.shiftReport.employeeName = employeeName;
            current.shiftReport.drawerTotal = drawerTotal;
            current.shiftReport.amountOwed = amountOwed;
            current.shiftReport.date = date;
            current.shiftReport.shiftType = shiftType;
            current.shiftReport.isSaved = true;
            reportData.setValue(current);
        }
    }
    
    /**
     * Save tobacco inventory data
     */
    public void saveTobaccoInventoryData(Map<String, String> tobaccoOpenValues,
                                        Map<String, String> tobaccoCloseValues) {
        ReportData current = reportData.getValue();
        if (current != null) {
            current.tobaccoInventory.tobaccoOpenValues = tobaccoOpenValues;
            current.tobaccoInventory.tobaccoCloseValues = tobaccoCloseValues;
            current.tobaccoInventory.isSaved = true;
            reportData.setValue(current);
        }
    }
    
    /**
     * Save lottery data
     */
    public void saveLotteryData(List<String> lotteryCodes) {
        ReportData current = reportData.getValue();
        if (current != null) {
            current.lottery.lotteryCodes = lotteryCodes;
            current.lottery.isSaved = true;
            reportData.setValue(current);
        }
    }
    
    /**
     * Clear all saved data
     */
    public void clearAllData() {
        reportData.setValue(new ReportData());
    }
    
    /**
     * Clear all saved data (for background thread use)
     */
    public void clearAllDataFromBackground() {
        reportData.postValue(new ReportData());
    }
    
    /**
     * Reset submission status (for background thread use)
     */
    public void resetSubmissionStatusFromBackground() {
        isSubmitting.postValue(false);
        submitStatus.postValue("");
    }
    
    /**
     * Clear Shift Report fields after successful submission (for background thread use)
     */
    public void clearShiftReportFieldsFromBackground() {
        // This will be handled by the HomeFragment when it receives the signal
        // We'll use a LiveData signal to notify the fragment to clear its fields
        clearShiftReportFields.postValue(true);
    }
    
    /**
     * Clear Tobacco fields after successful submission (for background thread use)
     */
    public void clearTobaccoFieldsFromBackground() {
        // This will be handled by the DashboardFragment when it receives the signal
        // We'll use a LiveData signal to notify the fragment to clear its fields
        clearTobaccoFields.postValue(true);
    }
    
    /**
     * Toggle shift type after successful submission (for background thread use)
     */
    public void toggleShiftTypeFromBackground() {
        shouldToggleShiftType.postValue(true);
    }
    
    /**
     * Get the shift type toggle signal
     */
    public LiveData<Boolean> getShouldToggleShiftType() {
        return shouldToggleShiftType;
    }
    
    /**
     * Reset the shift type toggle signal
     */
    public void resetShiftTypeToggle() {
        shouldToggleShiftType.setValue(false);
    }
    
    /**
     * Get the clear shift report fields signal
     */
    public LiveData<Boolean> getClearShiftReportFields() {
        return clearShiftReportFields;
    }
    
    /**
     * Reset the clear shift report fields signal
     */
    public void resetClearShiftReportFields() {
        clearShiftReportFields.setValue(false);
    }
    
    /**
     * Get the clear tobacco fields signal
     */
    public LiveData<Boolean> getClearTobaccoFields() {
        return clearTobaccoFields;
    }
    
    /**
     * Reset the clear tobacco fields signal
     */
    public void resetClearTobaccoFields() {
        clearTobaccoFields.setValue(false);
    }
    
    /**
     * Get submission status
     */
    public LiveData<Boolean> getIsSubmitting() {
        return isSubmitting;
    }
    
    /**
     * Get submit status message
     */
    public LiveData<String> getSubmitStatus() {
        return submitStatus;
    }
    
    /**
     * Submit the complete report via webhook
     */
    public void submitReport() {
        try {
            ReportData current = reportData.getValue();
            if (current == null) {
                submitStatus.setValue("Error: No report data available");
                return;
            }
            
            if (!current.isComplete()) {
                submitStatus.setValue("Error: All sections must be saved before submitting");
                return;
            }
            
            isSubmitting.setValue(true);
            submitStatus.setValue("Submitting report...");
            
            // Submit to actual webhook
            new Thread(() -> {
                try {
                    // Generate JSON for submission
                    String reportJson = current.toJsonString();
                    System.out.println("Submitting report JSON: " + reportJson);
                    
                            // Webhook URL
        String webhookUrl = "https://script.google.com/macros/s/AKfycbxsSis56yTAiPqQqjayYOePnzRkjuBYu6AEgfZy2tLWCrDTxH-MuncPczAe2qEitfmD/exec";
                    
                    // Create HTTP connection
                    java.net.URL url = new java.net.URL(webhookUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setDoOutput(true);
                    
                    // Send the JSON data
                    try (java.io.OutputStream os = connection.getOutputStream()) {
                        byte[] input = reportJson.getBytes("UTF-8");
                        os.write(input, 0, input.length);
                    }
                    
                    // Get the response
                    int responseCode = connection.getResponseCode();
                    System.out.println("Webhook response code: " + responseCode);
                    
                    // Read response
                    java.io.BufferedReader reader;
                    if (responseCode >= 200 && responseCode < 300) {
                        reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                    } else {
                        reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream()));
                    }
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    System.out.println("Webhook response: " + response.toString());
                    
                    // Handle response
                    if (responseCode >= 200 && responseCode < 300) {
                        submitStatus.postValue("Report submitted successfully! Response: " + response.toString());
                        isSubmitting.postValue(false);
                        
                        // Toggle shift type after successful submission
                        toggleShiftTypeFromBackground();
                        
                        // Clear the data after successful submission
                        clearAllDataFromBackground();
                        
                        // Clear Shift Report fields after successful submission
                        clearShiftReportFieldsFromBackground();
                        
                        // Clear Tobacco fields after successful submission
                        clearTobaccoFieldsFromBackground();
                    } else {
                        submitStatus.postValue("Error: Webhook returned code " + responseCode + ". Response: " + response.toString());
                        isSubmitting.postValue(false);
                    }
                    
                } catch (Exception e) {
                    System.out.println("Webhook submission error: " + e.getMessage());
                    e.printStackTrace();
                    submitStatus.postValue("Error: " + e.getMessage());
                    isSubmitting.postValue(false);
                }
            }).start();
            
        } catch (Exception e) {
            submitStatus.setValue("Error: " + e.getMessage());
            isSubmitting.setValue(false);
        }
    }
    
    /**
     * Get the JSON string for webhook submission
     */
    public String getReportJson() {
        ReportData current = reportData.getValue();
        return current != null ? current.toJsonString() : "{}";
    }
}
