package com.example.inventory.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<Boolean> mSubmissionStatus;
    private final MutableLiveData<String> mSubmissionMessage;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Shift Report");
        
        mSubmissionStatus = new MutableLiveData<>();
        mSubmissionStatus.setValue(false);
        
        mSubmissionMessage = new MutableLiveData<>();
        mSubmissionMessage.setValue("");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<Boolean> getSubmissionStatus() {
        return mSubmissionStatus;
    }

    public LiveData<String> getSubmissionMessage() {
        return mSubmissionMessage;
    }

    public void submitShiftReport(HomeFragment.ShiftReport report) {
        // Here you can add logic to save the report to a database or send it to a server
        // For now, we'll just update the status
        
        try {
            // Validate the report
            if (report == null) {
                mSubmissionStatus.setValue(false);
                mSubmissionMessage.setValue("Report is null");
                return;
            }

            // Log the report data (you can replace this with actual submission logic)
            System.out.println("=== SHIFT REPORT SUBMITTED ===");
            System.out.println("Employee: " + report.employeeName);
            System.out.println("Safe Entries: " + report.safeEntries.size());
            System.out.println("Cash Drawer Entries: " + report.drawerEntries.size());
            System.out.println("Safe Drop Entries: " + report.dropEntries.size());
            
            // Print safe entries
            for (HomeFragment.SafeEntry entry : report.safeEntries) {
                System.out.println("Safe " + entry.denomination + ": Open=" + entry.openValue + ", Close=" + entry.closeValue);
            }
            
            // Print cash drawer entries
            for (HomeFragment.CashDrawerEntry entry : report.drawerEntries) {
                System.out.println("Drawer " + entry.denomination + ": Open=" + entry.openValue + ", Close=" + entry.closeValue);
            }
            
            // Print safe drop entries
            for (HomeFragment.SafeDropEntry entry : report.dropEntries) {
                System.out.println("Drop: Envelope=" + entry.envelopeNumber + ", Amount=" + entry.amount + ", IsBank=" + entry.isBankRow);
            }
            System.out.println("=== END REPORT ===");

            // Update status to success
            mSubmissionStatus.setValue(true);
            mSubmissionMessage.setValue("Shift report submitted successfully!");
            
        } catch (Exception e) {
            mSubmissionStatus.setValue(false);
            mSubmissionMessage.setValue("Error submitting report: " + e.getMessage());
        }
    }

    public void resetSubmissionStatus() {
        mSubmissionStatus.setValue(false);
        mSubmissionMessage.setValue("");
    }
}