package com.example.inventory.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> mSubmissionStatus;
    private final MutableLiveData<String> mSubmissionMessage;

    public DashboardViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Tobacco Inventory");
        
        mSubmissionStatus = new MutableLiveData<>();
        mSubmissionMessage = new MutableLiveData<>();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getSubmissionStatus() {
        return mSubmissionStatus;
    }

    public LiveData<String> getSubmissionMessage() {
        return mSubmissionMessage;
    }

    public void submitTobaccoInventory(List<DashboardFragment.TobaccoEntry> tobaccoEntries) {
        try {
            // Log the tobacco inventory data
            StringBuilder report = new StringBuilder();
            report.append("Tobacco Inventory Report:\n");
            
            for (DashboardFragment.TobaccoEntry entry : tobaccoEntries) {
                report.append(String.format("Row %d: %s - Open: %s, Close: %s\n", 
                    entry.getRowNumber(), 
                    entry.getProductName(), 
                    entry.getOpenValue(), 
                    entry.getCloseValue()));
            }
            
            // For now, just log the data
            // In the future, you could integrate with Google Sheets or other systems
            System.out.println(report.toString());
            
            mSubmissionStatus.setValue("SUCCESS");
            mSubmissionMessage.setValue("Tobacco inventory data logged successfully");
            
        } catch (Exception e) {
            mSubmissionStatus.setValue("ERROR");
            mSubmissionMessage.setValue("Error submitting tobacco inventory: " + e.getMessage());
        }
    }
}