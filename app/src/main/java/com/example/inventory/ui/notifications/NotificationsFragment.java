package com.example.inventory.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.inventory.databinding.FragmentNotificationsBinding;
import com.example.inventory.ui.shared.ReportViewModel;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private ReportViewModel reportViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        reportViewModel = new ViewModelProvider(requireActivity()).get(ReportViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupObservers();
        setupClickListeners();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupObservers() {
        // Observe report data changes
        reportViewModel.getReportData().observe(getViewLifecycleOwner(), reportData -> {
            updateReportPreview(reportData);
        });

        // Observe submission status
        reportViewModel.getIsSubmitting().observe(getViewLifecycleOwner(), isSubmitting -> {
            if (binding != null) {
                binding.buttonSubmitReport.setEnabled(!isSubmitting);
                binding.buttonSubmitReport.setText(isSubmitting ? "Submitting..." : "Submit Final Report");
            }
        });

        reportViewModel.getSubmitStatus().observe(getViewLifecycleOwner(), status -> {
            if (binding != null && !status.isEmpty()) {
                binding.textSubmitStatus.setText(status);
            }
        });
    }

    private void setupClickListeners() {
        binding.buttonSubmitReport.setOnClickListener(v -> {
            try {
                if (reportViewModel.getCurrentReportData() != null && reportViewModel.getCurrentReportData().isComplete()) {
                    showSubmitConfirmationDialog();
                }
            } catch (Exception e) {
                // Error handled silently - status will show in the status text
            }
        });

        binding.buttonClearAll.setOnClickListener(v -> {
            try {
                showClearConfirmationDialog();
            } catch (Exception e) {
                // Error handled silently
            }
        });
    }

    private void updateReportPreview(com.example.inventory.data.ReportData reportData) {
        if (binding == null) return;

        try {
            StringBuilder preview = new StringBuilder();
            preview.append("📋 REPORT PREVIEW\n");
            preview.append("================\n\n");

        // Save status
        preview.append("📊 SAVE STATUS:\n");
        preview.append(reportData.getSaveStatus());
        preview.append("\n\n");

        // Shift Report Preview
        preview.append("🏪 SHIFT REPORT:\n");
        if (reportData.shiftReport.isSaved) {
            preview.append("✓ Employee: ").append(reportData.shiftReport.employeeName != null ? reportData.shiftReport.employeeName : "Not specified").append("\n");
            preview.append("✓ Amount Owed: ").append(reportData.shiftReport.amountOwed != null ? reportData.shiftReport.amountOwed : "$0.00").append("\n");
            preview.append("✓ Drawer Total: ").append(reportData.shiftReport.drawerTotal != null ? reportData.shiftReport.drawerTotal : "Not calculated").append("\n");
            
            if (reportData.shiftReport.safeDrops != null && !reportData.shiftReport.safeDrops.isEmpty()) {
                preview.append("✓ Safe Drops: ").append(reportData.shiftReport.safeDrops.size()).append(" entries\n");
            }
        } else {
            preview.append("✗ Not saved\n");
        }
        preview.append("\n");

        // Tobacco Inventory Preview
        preview.append("🚬 TOBACCO INVENTORY:\n");
        if (reportData.tobaccoInventory.isSaved) {
            if (reportData.tobaccoInventory.tobaccoCloseValues != null) {
                int totalItems = 0;
                for (String value : reportData.tobaccoInventory.tobaccoCloseValues.values()) {
                    try {
                        totalItems += Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        // Skip invalid values
                    }
                }
                preview.append("✓ Total Items: ").append(totalItems).append("\n");
            }
        } else {
            preview.append("✗ Not saved\n");
        }
        preview.append("\n");

        // Lottery Preview
        preview.append("🎫 LOTTERY:\n");
        if (reportData.lottery.isSaved) {
            if (reportData.lottery.lotteryCodes != null) {
                preview.append("✓ Codes: ").append(reportData.lottery.lotteryCodes.size()).append(" scanned\n");
            } else {
                preview.append("✓ No codes scanned\n");
            }
        } else {
            preview.append("✗ Not saved\n");
        }
        preview.append("\n");

        // Submission status
        if (reportData.isComplete()) {
            preview.append("✅ READY TO SUBMIT\n");
            preview.append("All sections have been saved and are ready for final submission.");
        } else {
            preview.append("⚠️ INCOMPLETE\n");
            preview.append("Please save all sections before submitting.");
        }

        binding.textReportPreview.setText(preview.toString());
        
        // Update submit button state
        binding.buttonSubmitReport.setEnabled(reportData.isComplete());
        } catch (Exception e) {
            if (binding != null) {
                binding.textReportPreview.setText("Error loading report preview: " + e.getMessage());
                binding.buttonSubmitReport.setEnabled(false);
            }
        }
    }

    private void showSubmitConfirmationDialog() {
        com.example.inventory.data.ReportData reportData = reportViewModel.getCurrentReportData();
        if (reportData == null) return;

        StringBuilder message = new StringBuilder();
        message.append("Are you ready to submit the final report?\n\n");
        message.append("This will send all data via webhook and cannot be undone.\n\n");
        message.append("REPORT SUMMARY:\n");
        message.append("• Shift Report: ✓ Saved\n");
        message.append("• Tobacco Inventory: ✓ Saved\n");
        message.append("• Lottery: ✓ Saved\n\n");
        message.append("Report ID: ").append(reportData.reportId);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Submit Final Report")
                .setMessage(message.toString())
                .setPositiveButton("Submit Report", (dialog, which) -> {
                    reportViewModel.submitReport();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void showClearConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("This will clear all saved data from all sections. This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    reportViewModel.clearAllData();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
}