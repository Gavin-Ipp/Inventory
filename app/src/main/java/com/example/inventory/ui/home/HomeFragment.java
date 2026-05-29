package com.example.inventory.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.inventory.databinding.FragmentHomeBinding;
import com.example.inventory.ui.shared.ReportViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;    
    private HomeViewModel homeViewModel;
    private ReportViewModel reportViewModel;
    private int dropRowCounter = 3; // Start from 3 since we have 2 initial rows
    private SharedPreferences preferences;
    private static final String PREF_NAME = "ShiftReportPrefs";
    
    // Shared data structure for safe values - both open and close display from this
    private String[] safeOpenValues = new String[9]; // Index 0-8 for rows 1-9
    private String[] safeCloseValues = new String[9]; // Index 0-8 for rows 1-9
    private String[] safeSessionStartValues = new String[9]; // Track values at start of current session
    private String[] drawerValues = new String[8];
    private String[] tempOpenDrawerValues = new String[8]; // Temporary storage for open drawer values
    
    // Shift type tracking - alternates between "Open" and "Close" on each submission
    private boolean isOpenShift = true; // true = Open, false = Close

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        android.util.Log.d("HomeFragment", "onCreateView called - HomeFragment is being created");
        android.util.Log.d("HomeFragment", "Fragment state - isAdded: " + isAdded() + ", isVisible: " + isVisible());
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        reportViewModel = new ViewModelProvider(requireActivity()).get(ReportViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Check if binding is properly initialized
        if (binding == null) {
            Toast.makeText(requireContext(), "Error: View binding failed", Toast.LENGTH_LONG).show();
            return root;
        }

        try {
            // Initialize SharedPreferences
            preferences = requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE);

            setupSpinners();
            loadOpenValues(); // Load persisted open values
            setupDateField();
            updateShiftTypeDisplay();
            setupShiftTypeToggleObserver();
            
            // Setup clear fields observer
            setupClearFieldsObserver();
            setupDrawerTextWatchers();
            
            // Setup buttons after view is loaded
            setupButtons();
            setupSubmitButton();
        } catch (Exception e) {
            // Log the error and show a user-friendly message
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error initializing shift report: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return root;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("HomeFragment", "onResume called - Fragment is now visible");
        android.util.Log.d("HomeFragment", "Fragment state - isAdded: " + isAdded() + ", isVisible: " + isVisible());
        
        // Refresh the UI when fragment becomes visible
        try {
            if (binding != null) {
                android.util.Log.d("HomeFragment", "Refreshing UI in onResume");
                loadOpenValuesOnResume(); // Reload data without clearing close values
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error refreshing UI in onResume: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        android.util.Log.d("HomeFragment", "onPause called - Fragment is no longer visible");
    }

    private void setupSpinners() {
        try {
            // Setup envelope spinners for initial rows
            if (binding.dropEnvelope1 != null) {
                setupEnvelopeSpinner(binding.dropEnvelope1, false); // Normal row (1-28)
            }
            if (binding.dropEnvelope2 != null) {
                setupEnvelopeSpinner(binding.dropEnvelope2, true);  // Bank row (Bank 1-11)
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Don't crash the app if spinner setup fails
        }
    }

    private void setupEnvelopeSpinner(Spinner spinner, boolean isBankRow) {
        List<String> options = new ArrayList<>();
        
        if (isBankRow) {
            // Bank options: Bank 1 - Bank 11
            for (int i = 1; i <= 11; i++) {
                options.add("Bank " + i);
            }
        } else {
            // Normal options: 1 - 28
            for (int i = 1; i <= 28; i++) {
                options.add(String.valueOf(i));
            }
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            options
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    
    private void setupDateField() {
        try {
            if (binding.editTextDate != null) {
                // Set today's date as default
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault());
                String today = sdf.format(new java.util.Date());
                binding.editTextDate.setText(today);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updateShiftTypeDisplay() {
        try {
            if (binding.textShiftType != null) {
                // Load shift type from SharedPreferences
                isOpenShift = preferences.getBoolean("is_open_shift", true);
                String shiftType = isOpenShift ? "Open" : "Close";
                binding.textShiftType.setText(shiftType);
                
                // Change color based on shift type
                if (isOpenShift) {
                    binding.textShiftType.setTextColor(getResources().getColor(android.R.color.holo_blue_dark, null));
                } else {
                    binding.textShiftType.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void toggleShiftType() {
        // Toggle the shift type and save to SharedPreferences
        isOpenShift = !isOpenShift;
        preferences.edit().putBoolean("is_open_shift", isOpenShift).apply();
        updateShiftTypeDisplay();
    }
    
    private void setupShiftTypeToggleObserver() {
        // Observe for shift type toggle signal from successful submission
        reportViewModel.getShouldToggleShiftType().observe(getViewLifecycleOwner(), shouldToggle -> {
            if (shouldToggle != null && shouldToggle) {
                toggleShiftType();
                // Reset the signal
                reportViewModel.resetShiftTypeToggle();
            }
        });
    }
    
    private void setupClearFieldsObserver() {
        // Observe for clear fields signal from successful final submission
        reportViewModel.getClearShiftReportFields().observe(getViewLifecycleOwner(), shouldClear -> {
            if (shouldClear != null && shouldClear) {
                android.util.Log.d("HomeFragment", "Clearing Shift Report fields after final submission");
                clearCloseValues();
                // Reset the signal
                reportViewModel.resetClearShiftReportFields();
            }
        });
    }
    
    private void showNextStepGuidance(String nextStep) {
        try {
            android.util.Log.d("HomeFragment", "Showing guidance for next step: " + nextStep);
            
            // Show a subtle guidance message
            if (getActivity() != null) {
                // You can customize this to show a more prominent guidance UI
                // For now, we'll just log it and the Toast message already shows the guidance
                android.util.Log.d("HomeFragment", "Next step guidance: " + nextStep);
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error showing guidance: " + e.getMessage(), e);
        }
    }
    
    private void setupDrawerTextWatchers() {
        try {
            // Setup TextWatchers for all drawer close EditText fields to auto-calculate
            if (binding != null) {
                binding.drawerClose1.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        drawerValues[0] = value;
                        preferences.edit().putString("drawer_close_1", value).apply();
                        calculateDrawerTotal();
                    }
                });
                
                binding.drawerClose2.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        drawerValues[1] = value;
                        preferences.edit().putString("drawer_close_2", value).apply();
                        calculateDrawerTotal();
                    }
                });
                
                binding.drawerClose3.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        drawerValues[2] = value;
                        preferences.edit().putString("drawer_close_3", value).apply();
                        calculateDrawerTotal();
                    }
                });
                
                binding.drawerClose4.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        drawerValues[3] = value;
                        preferences.edit().putString("drawer_close_4", value).apply();
                        calculateDrawerTotal();
                    }
                });
                
                binding.drawerClose5.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        drawerValues[4] = value;
                        preferences.edit().putString("drawer_close_5", value).apply();
                        calculateDrawerTotal();
                    }
                });
                
                binding.drawerClose6.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        drawerValues[5] = value;
                        preferences.edit().putString("drawer_close_6", value).apply();
                        calculateDrawerTotal();
                    }
                });
                
                binding.drawerClose7.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        drawerValues[6] = value;
                        preferences.edit().putString("drawer_close_7", value).apply();
                        calculateDrawerTotal();
                    }
                });
                
                binding.drawerClose8.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        drawerValues[7] = value;
                        preferences.edit().putString("drawer_close_8", value).apply();
                        calculateDrawerTotal();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupButtons() {
        try {
            android.util.Log.d("HomeFragment", "Setting up buttons...");
            System.out.println("Setting up buttons...");
            
            if (binding.buttonAddNormalRow != null) {
                binding.buttonAddNormalRow.setOnClickListener(v -> addDropRow(false));
            }
            if (binding.buttonAddBankRow != null) {
                binding.buttonAddBankRow.setOnClickListener(v -> addDropRow(true));
            }
            
            if (binding.buttonSetOpenDrawer != null) {
                binding.buttonSetOpenDrawer.setOnClickListener(v -> showSetOpenDrawerDialog());
            }
            if (binding.buttonResetSafe != null) {
                binding.buttonResetSafe.setOnClickListener(v -> clearAllSafeValues());
            }
            
            // Setup decrement buttons for Safe table
            android.util.Log.d("HomeFragment", "Setting up safe decrement buttons...");
            System.out.println("Setting up safe decrement buttons...");
            setupSafeDecrementButtons();
            android.util.Log.d("HomeFragment", "Button setup complete");
            System.out.println("Button setup complete");
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error setting up buttons: " + e.getMessage(), e);
            e.printStackTrace();
            // Don't crash the app if button setup fails
        }
    }

    private void setupSubmitButton() {
        try {
            if (binding.buttonSubmitReport != null) {
                binding.buttonSubmitReport.setOnClickListener(v -> submitShiftReport());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Don't crash the app if submit button setup fails
        }
    }

    private void addDropRow(boolean isBankRow) {
        TableLayout table = binding.tableSafeDrops;
        
        // Create new row
        TableRow newRow = new TableRow(requireContext());
        newRow.setLayoutParams(new TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT
        ));

        // Create spinner for envelope number
        Spinner envelopeSpinner = new Spinner(requireContext());
        envelopeSpinner.setLayoutParams(new TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        envelopeSpinner.setPadding(32, 32, 32, 32);
        setupEnvelopeSpinner(envelopeSpinner, isBankRow);

        // Create EditText for amount
        EditText amountEdit = new EditText(requireContext());
        amountEdit.setLayoutParams(new TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        amountEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountEdit.setHint("0.00");
        amountEdit.setGravity(android.view.Gravity.CENTER);
        amountEdit.setPadding(32, 32, 32, 32);

        // Add views to row
        newRow.addView(envelopeSpinner);
        newRow.addView(amountEdit);

        // Add row to table
        table.addView(newRow);
        
        dropRowCounter++;
        
        Toast.makeText(requireContext(), 
            "Added " + (isBankRow ? "bank" : "normal") + " row", 
            Toast.LENGTH_SHORT).show();
    }

    private void submitShiftReport() {
        try {
            // Show pre-save checklist
            showPreSaveChecklist();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error showing checklist: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showPreSaveChecklist() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Pre-Save Checklist");
        
        // Create a custom layout for the checklist
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        
        // Array of checklist items
        String[] checklistItems = {
            "Entered Employee Name",
            "Checked the Safe",
            "Updated the Open Drawer",
            "Made their Close Drawer",
            "Dropped Normal Envelopes",
            "Dropped Bank Envelopes"
        };
        
        // Array to store CheckBox references
        CheckBox[] checkBoxes = new CheckBox[checklistItems.length];
        
        // Create CheckBox for each checklist item
        for (int i = 0; i < checklistItems.length; i++) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(checklistItems[i]);
            
            // Pre-check employee name if it's already entered
            if (i == 0 && !binding.editTextEmployeeName.getText().toString().trim().isEmpty()) {
                checkBox.setChecked(true);
            }
            
            // Pre-check drawer if it's in the correct range (125-126)
            if (i == 3) { // "Made their Close Drawer" is index 3
                double drawerTotal = getCurrentDrawerTotal();
                if (drawerTotal >= 125.0 && drawerTotal <= 125.99) {
                    checkBox.setChecked(true);
                }
            }
            
            checkBoxes[i] = checkBox;
            layout.addView(checkBox);
        }
        
        builder.setView(layout);
        
        // Set up buttons BEFORE creating the dialog
        builder.setPositiveButton("Save", (dialogInterface, which) -> {
            // Check if all items are checked
            boolean allChecked = true;
            for (CheckBox checkBox : checkBoxes) {
                if (!checkBox.isChecked()) {
                    allChecked = false;
                    break;
                }
            }
            
            if (allChecked) {
                // Additional validation for employee name if the checkbox is checked
                if (checkBoxes[0].isChecked() && binding.editTextEmployeeName.getText().toString().trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter employee name", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                performSave();
            } else {
                Toast.makeText(requireContext(), "Please check all items before saving", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialogInterface, which) -> {
            dialogInterface.dismiss();
        });
        
        // Create the dialog AFTER setting up buttons
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void performSave() {
        try {
            // Get employee name (already validated in checklist)
            String employeeName = binding.editTextEmployeeName.getText().toString().trim();
            
            // Get date
            String date = binding.editTextDate.getText().toString().trim();
            
            // Get shift type
            String shiftType = isOpenShift ? "Open" : "Close";

            // Collect Safe data
            List<SafeEntry> safeEntries = collectSafeData();
            
            // Collect Cash Drawer data
            List<CashDrawerEntry> drawerEntries = collectCashDrawerData();
            
            // Collect Safe Drops data
            List<SafeDropEntry> dropEntries = collectSafeDropsData();
            
            // Create shift report
            ShiftReport report = new ShiftReport(employeeName, safeEntries, drawerEntries, dropEntries);
            
            // Submit to local ViewModel for logging
            homeViewModel.submitShiftReport(report);
            
            // Convert data to Maps for shared ViewModel
            Map<String, String> safeOpenMap = new HashMap<>();
            Map<String, String> safeCloseMap = new HashMap<>();
            Map<String, String> drawerOpenMap = new HashMap<>();
            Map<String, String> drawerCloseMap = new HashMap<>();
            Map<String, String> safeDropsMap = new HashMap<>();
            
            // Convert safe data using actual denomination names
            String[] safeLabels = {
                "Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", 
                "$5 Bills", "Pennies (xtra)", "$300", "Xtra $5/10"
            };
            for (int i = 0; i < 9; i++) {
                safeOpenMap.put(safeLabels[i], safeOpenValues[i]);
                safeCloseMap.put(safeLabels[i], safeCloseValues[i]);
            }
            
            // Convert drawer data using actual denomination names
            String[] drawerLabels = {
                "Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", "$5 Bills", "$10 Bills", "$20/ Rolled Coins"
            };
            for (int i = 0; i < 8; i++) {
                drawerOpenMap.put(drawerLabels[i], drawerValues[i]);
                String closeValue = getDrawerCloseValue(i + 1);
                drawerCloseMap.put(drawerLabels[i], closeValue);
            }
            
            // Convert safe drops data
            for (SafeDropEntry drop : dropEntries) {
                safeDropsMap.put(drop.envelopeNumber, drop.amount);
            }
            
            // Get amount owed
            String amountOwed = binding.textAmountOwed.getText().toString();
            
            // Get drawer total
            String drawerTotal = binding.textDrawerTotal.getText().toString();
            
            // Save to shared ViewModel
            reportViewModel.saveShiftReportData(
                safeOpenMap, safeCloseMap, drawerOpenMap, drawerCloseMap, 
                safeDropsMap, employeeName, drawerTotal, amountOwed, date, shiftType
            );
            
            // Copy current close values (including decrements) to open values for next shift
            for (int i = 0; i < 9; i++) {
                safeOpenValues[i] = safeCloseValues[i];
            }
            
            // Update open display to show the new open values
            updateOpenDisplay();
            
            // Apply temporary open drawer values if they exist
            boolean hasTempValues = false;
            for (int i = 0; i < 8; i++) {
                if (tempOpenDrawerValues[i] != null && !tempOpenDrawerValues[i].isEmpty()) {
                    hasTempValues = true;
                    break;
                }
            }
            
            if (hasTempValues) {
                // Apply temporary values to open drawer displays and save them
                for (int i = 0; i < 8; i++) {
                    String value = tempOpenDrawerValues[i] != null ? tempOpenDrawerValues[i] : "0";
                    drawerValues[i] = value;
                    
                    // Update display
                    switch (i) {
                        case 0: binding.drawerOpen1.setText(value); break;
                        case 1: binding.drawerOpen2.setText(value); break;
                        case 2: binding.drawerOpen3.setText(value); break;
                        case 3: binding.drawerOpen4.setText(value); break;
                        case 4: binding.drawerOpen5.setText(value); break;
                        case 5: binding.drawerOpen6.setText(value); break;
                        case 6: binding.drawerOpen7.setText(value); break;
                        case 7: binding.drawerOpen8.setText(value); break;
                    }
                    
                    // Save to SharedPreferences
                    String key = "drawer_open_" + (i + 1);
                    preferences.edit().putString(key, value).apply();
                }
            } else {
                // Update cash drawer open display normally
                updateCashDrawerOpenDisplay();
            }
            
            // Save the new open values to persistent storage
            saveOpenValues();
            
            // Don't clear close values on save - they should persist until final submission
            // clearCloseValues();
            
            // Reset session start values to current close values and reset amount owed
            for (int i = 0; i < 9; i++) {
                safeSessionStartValues[i] = safeCloseValues[i];
            }
            
            // Save new session start values to SharedPreferences
            for (int i = 1; i <= 9; i++) {
                String key = "safe_session_start_" + i;
                preferences.edit().putString(key, safeSessionStartValues[i-1]).apply();
            }
            
            // Reset amount owed to $0.00 after save
            if (binding.textAmountOwed != null) {
                binding.textAmountOwed.setText("Amount Owed: $0.00");
            }
            
            // Reset temporary open drawer values
            for (int i = 0; i < 8; i++) {
                tempOpenDrawerValues[i] = null;
            }
            
            Toast.makeText(requireContext(), "Shift report saved successfully! Next: Tobacco Inventory", Toast.LENGTH_LONG).show();
            
            // Provide subtle guidance without forcing navigation
            showNextStepGuidance("Tobacco Inventory");
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error saving report: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void copyCloseValuesToOpen() {
        // Save current shared safe close values as new open values to persistent storage
        for (int i = 1; i <= 9; i++) {
            String key = "safe_open_" + i;
            String currentValue = safeCloseValues[i-1];
            if (currentValue != null && !currentValue.isEmpty()) {
                preferences.edit().putString(key, currentValue).apply();
            }
        }
        
        // Also update the open values array to match
        for (int i = 0; i < 9; i++) {
            safeOpenValues[i] = safeCloseValues[i];
        }
        
        // Save current shared cash drawer values to persistent storage
        for (int i = 1; i <= 8; i++) {
            String key = "drawer_open_" + i;
            String currentValue = drawerValues[i-1];
            if (currentValue != null && !currentValue.isEmpty()) {
                preferences.edit().putString(key, currentValue).apply();
            }
        }
    }

    private List<SafeEntry> collectSafeData() {
        List<SafeEntry> entries = new ArrayList<>();
        
        // Safe row labels in order
        String[] safeLabels = {
            "Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", 
            "$5 Bills", "Pennies (xtra)", "$300", "Xtra $5/10"
        };
        
        // Collect data from shared data structure
        for (int i = 1; i <= 9; i++) {
            String openValue = safeOpenValues[i-1];
            String closeValue = safeCloseValues[i-1];
            if (openValue != null && !openValue.isEmpty() && closeValue != null && !closeValue.isEmpty()) {
                try {
                    // Validate that values are numeric
                    Integer.parseInt(openValue);
                    Integer.parseInt(closeValue);
                    entries.add(new SafeEntry(i, safeLabels[i-1], openValue, closeValue));
                } catch (NumberFormatException e) {
                    // If parsing fails, use the original value
                    entries.add(new SafeEntry(i, safeLabels[i-1], openValue, closeValue));
                }
            }
        }
        
        return entries;
    }

    private List<CashDrawerEntry> collectCashDrawerData() {
        List<CashDrawerEntry> entries = new ArrayList<>();
        
        // Cash drawer row labels in order
        String[] drawerLabels = {
            "Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", "$5 Bills", "$10 Bills", "$20/ Rolled Coins"
        };
        
        // Collect data from shared data structure and EditText fields
        for (int i = 1; i <= 8; i++) {
            String openValue = drawerValues[i-1];
            String closeValue = getDrawerCloseValue(i);
            
            if (openValue != null && !openValue.isEmpty() && closeValue != null && !closeValue.isEmpty()) {
                try {
                    // Validate that values are numeric
                    Integer.parseInt(openValue);
                    Integer.parseInt(closeValue);
                    entries.add(new CashDrawerEntry(i, drawerLabels[i-1], openValue, closeValue));
                } catch (NumberFormatException e) {
                    // If parsing fails, use the original value
                    entries.add(new CashDrawerEntry(i, drawerLabels[i-1], openValue, closeValue));
                }
            }
        }
        
        return entries;
    }

    private List<SafeDropEntry> collectSafeDropsData() {
        List<SafeDropEntry> entries = new ArrayList<>();
        
        // Collect data from initial rows
        if (binding.dropEnvelope1.getSelectedItem() != null && 
            !binding.dropAmount1.getText().toString().isEmpty()) {
            entries.add(new SafeDropEntry(
                binding.dropEnvelope1.getSelectedItem().toString(),
                binding.dropAmount1.getText().toString(),
                false
            ));
        }
        
        if (binding.dropEnvelope2.getSelectedItem() != null && 
            !binding.dropAmount2.getText().toString().isEmpty()) {
            entries.add(new SafeDropEntry(
                binding.dropEnvelope2.getSelectedItem().toString(),
                binding.dropAmount2.getText().toString(),
                true
            ));
        }
        
        // Collect data from dynamically added rows
        TableLayout table = binding.tableSafeDrops;
        for (int i = 2; i < table.getChildCount(); i++) { // Skip header and initial 2 rows
            TableRow row = (TableRow) table.getChildAt(i);
            if (row.getChildCount() >= 2) {
                Spinner envelopeSpinner = (Spinner) row.getChildAt(0);
                EditText amountEdit = (EditText) row.getChildAt(1);
                
                if (envelopeSpinner.getSelectedItem() != null && 
                    !amountEdit.getText().toString().isEmpty()) {
                    
                    String envelopeValue = envelopeSpinner.getSelectedItem().toString();
                    boolean isBankRow = envelopeValue.startsWith("Bank");
                    
                    entries.add(new SafeDropEntry(
                        envelopeValue,
                        amountEdit.getText().toString(),
                        isBankRow
                    ));
                }
            }
        }
        
        return entries;
    }

    private EditText getEditTextById(String id) {
        try {
            if (getContext() == null || getView() == null) {
                return null;
            }
            int resourceId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
            if (resourceId != 0) {
                return requireView().findViewById(resourceId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private TextView getTextViewById(String id) {
        try {
            if (getContext() == null || getView() == null) {
                return null;
            }
            int resourceId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
            if (resourceId != 0) {
                return requireView().findViewById(resourceId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadOpenValuesOnResume() {
        try {
            android.util.Log.d("HomeFragment", "loadOpenValuesOnResume called - preserving close values");
            // Load safe open values into shared data structure
            String[] safeDefaultValues = {"10", "10", "10", "10", "10", "10", "10", "1", "1"};
            
            // Load safe open values
            for (int i = 1; i <= 9; i++) {
                String key = "safe_open_" + i;
                String savedValue = preferences.getString(key, safeDefaultValues[i-1]);
                safeOpenValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Load safe close values
            for (int i = 1; i <= 9; i++) {
                String key = "safe_close_" + i;
                String savedValue = preferences.getString(key, safeDefaultValues[i-1]);
                safeCloseValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Load safe session start values
            for (int i = 1; i <= 9; i++) {
                String key = "safe_session_start_" + i;
                String savedValue = preferences.getString(key, safeCloseValues[i-1]);
                safeSessionStartValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Update safe displays
            updateSafeDisplays();
            
            // Load drawer values
            for (int i = 1; i <= 8; i++) {
                String key = "drawer_open_" + i;
                String savedValue = preferences.getString(key, "");
                drawerValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Update drawer displays - preserve close values
            if (binding != null) {
                // Update open fields
                binding.drawerOpen1.setText(drawerValues[0]);
                binding.drawerOpen2.setText(drawerValues[1]);
                binding.drawerOpen3.setText(drawerValues[2]);
                binding.drawerOpen4.setText(drawerValues[3]);
                binding.drawerOpen5.setText(drawerValues[4]);
                binding.drawerOpen6.setText(drawerValues[5]);
                binding.drawerOpen7.setText(drawerValues[6]);
                binding.drawerOpen8.setText(drawerValues[7]);
                
                // Load and restore close values from SharedPreferences
                binding.drawerClose1.setText(preferences.getString("drawer_close_1", ""));
                binding.drawerClose2.setText(preferences.getString("drawer_close_2", ""));
                binding.drawerClose3.setText(preferences.getString("drawer_close_3", ""));
                binding.drawerClose4.setText(preferences.getString("drawer_close_4", ""));
                binding.drawerClose5.setText(preferences.getString("drawer_close_5", ""));
                binding.drawerClose6.setText(preferences.getString("drawer_close_6", ""));
                binding.drawerClose7.setText(preferences.getString("drawer_close_7", ""));
                binding.drawerClose8.setText(preferences.getString("drawer_close_8", ""));
            }
            
            // Calculate initial amount owed
            calculateAmountOwed();
            android.util.Log.d("HomeFragment", "loadOpenValuesOnResume completed successfully");
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error in loadOpenValuesOnResume: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    private void loadOpenValues() {
        try {
            android.util.Log.d("HomeFragment", "loadOpenValues called");
            // Load safe open values into shared data structure
            String[] safeDefaultValues = {"10", "10", "10", "10", "10", "10", "10", "1", "1"};
            for (int i = 1; i <= 9; i++) {
                String key = "safe_open_" + i;
                String savedValue = preferences.getString(key, safeDefaultValues[i-1]);
                safeOpenValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Load safe close values from SharedPreferences, defaulting to defaults if not saved
            for (int i = 1; i <= 9; i++) {
                String key = "safe_close_" + i;
                String savedValue = preferences.getString(key, safeDefaultValues[i-1]);
                safeCloseValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Load session start values from SharedPreferences, defaulting to current close values if not saved
            for (int i = 1; i <= 9; i++) {
                String key = "safe_session_start_" + i;
                String savedValue = preferences.getString(key, safeCloseValues[i-1]);
                safeSessionStartValues[i-1] = savedValue;
            }
            
            // Use direct binding to update displays
            if (binding != null) {
                // Update open fields using direct binding
                binding.safeOpen1.setText(safeOpenValues[0]);
                binding.safeOpen2.setText(safeOpenValues[1]);
                binding.safeOpen3.setText(safeOpenValues[2]);
                binding.safeOpen4.setText(safeOpenValues[3]);
                binding.safeOpen5.setText(safeOpenValues[4]);
                binding.safeOpen6.setText(safeOpenValues[5]);
                binding.safeOpen7.setText(safeOpenValues[6]);
                binding.safeOpen8.setText(safeOpenValues[7]);
                binding.safeOpen9.setText(safeOpenValues[8]);
                
                // Update close fields using direct binding
                binding.safeClose1.setText(safeCloseValues[0]);
                binding.safeClose2.setText(safeCloseValues[1]);
                binding.safeClose3.setText(safeCloseValues[2]);
                binding.safeClose4.setText(safeCloseValues[3]);
                binding.safeClose5.setText(safeCloseValues[4]);
                binding.safeClose6.setText(safeCloseValues[5]);
                binding.safeClose7.setText(safeCloseValues[6]);
                binding.safeClose8.setText(safeCloseValues[7]);
                binding.safeClose9.setText(safeCloseValues[8]);
            }
            
            // Load cash drawer values into shared data structure
            for (int i = 1; i <= 8; i++) {
                String key = "drawer_open_" + i;
                String savedValue = preferences.getString(key, "0");
                drawerValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Use direct binding to update cash drawer displays
            if (binding != null) {
                // Update open fields using direct binding
                binding.drawerOpen1.setText(drawerValues[0]);
                binding.drawerOpen2.setText(drawerValues[1]);
                binding.drawerOpen3.setText(drawerValues[2]);
                binding.drawerOpen4.setText(drawerValues[3]);
                binding.drawerOpen5.setText(drawerValues[4]);
                binding.drawerOpen6.setText(drawerValues[5]);
                binding.drawerOpen7.setText(drawerValues[6]);
                binding.drawerOpen8.setText(drawerValues[7]);
                
                // Update close fields to empty (user will enter their own values)
                binding.drawerClose1.setText("");
                binding.drawerClose2.setText("");
                binding.drawerClose3.setText("");
                binding.drawerClose4.setText("");
                binding.drawerClose5.setText("");
                binding.drawerClose6.setText("");
                binding.drawerClose7.setText("");
                binding.drawerClose8.setText("");
            }
            
            // Calculate initial amount owed
            calculateAmountOwed();
            android.util.Log.d("HomeFragment", "loadOpenValues completed successfully");
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error in loadOpenValues: " + e.getMessage(), e);
            e.printStackTrace();
            // Don't crash the app if loading fails
        }
    }
    
    private void updateSafeDisplay(int rowIndex) {
        // Only update close display from shared data structure
        // Open display stays visually static until submit
        TextView closeField = getTextViewById("safe_close_" + rowIndex);
        
        if (closeField != null) {
            closeField.setText(safeCloseValues[rowIndex-1]);
        }
    }
    
    private void updateSafeDisplayDirect(int rowIndex) {
        // Update close display using direct binding
        if (binding != null) {
            switch (rowIndex) {
                case 1: binding.safeClose1.setText(safeCloseValues[0]); break;
                case 2: binding.safeClose2.setText(safeCloseValues[1]); break;
                case 3: binding.safeClose3.setText(safeCloseValues[2]); break;
                case 4: binding.safeClose4.setText(safeCloseValues[3]); break;
                case 5: binding.safeClose5.setText(safeCloseValues[4]); break;
                case 6: binding.safeClose6.setText(safeCloseValues[5]); break;
                case 7: binding.safeClose7.setText(safeCloseValues[6]); break;
                case 8: binding.safeClose8.setText(safeCloseValues[7]); break;
                case 9: binding.safeClose9.setText(safeCloseValues[8]); break;
            }
        }
    }
    
    private void updateOpenDisplay() {
        // Update all open displays to match current shared values
        if (binding != null) {
            binding.safeOpen1.setText(safeOpenValues[0]);
            binding.safeOpen2.setText(safeOpenValues[1]);
            binding.safeOpen3.setText(safeOpenValues[2]);
            binding.safeOpen4.setText(safeOpenValues[3]);
            binding.safeOpen5.setText(safeOpenValues[4]);
            binding.safeOpen6.setText(safeOpenValues[5]);
            binding.safeOpen7.setText(safeOpenValues[6]);
            binding.safeOpen8.setText(safeOpenValues[7]);
            binding.safeOpen9.setText(safeOpenValues[8]);
        }
    }
    
    private void clearCloseValues() {
        // Clear cash drawer close values
        if (binding != null) {
            binding.drawerClose1.setText("");
            binding.drawerClose2.setText("");
            binding.drawerClose3.setText("");
            binding.drawerClose4.setText("");
            binding.drawerClose5.setText("");
            binding.drawerClose6.setText("");
            binding.drawerClose7.setText("");
            binding.drawerClose8.setText("");
        }
        
        // Clear drawer values array
        for (int i = 0; i < 8; i++) {
            drawerValues[i] = null;
        }
        
        // Clear drawer total
        if (binding.textDrawerTotal != null) {
            binding.textDrawerTotal.setText("Total: $0.00");
        }
        
        // Clear safe drops
        if (binding.dropAmount1 != null) binding.dropAmount1.setText("");
        if (binding.dropAmount2 != null) binding.dropAmount2.setText("");
        
        // Clear dynamically added safe drop rows
        TableLayout table = binding.tableSafeDrops;
        for (int i = 2; i < table.getChildCount(); i++) { // Skip header and initial 2 rows
            TableRow row = (TableRow) table.getChildAt(i);
            if (row.getChildCount() >= 2) {
                EditText amountEdit = (EditText) row.getChildAt(1);
                amountEdit.setText("");
            }
        }
        
        // Clear employee name
        if (binding.editTextEmployeeName != null) {
            binding.editTextEmployeeName.setText("");
        }
        
        calculateAmountOwed();
    }
    
    private void saveOpenValues() {
        // Save safe open values to persistent storage
        for (int i = 1; i <= 9; i++) {
            String key = "safe_open_" + i;
            String currentValue = safeOpenValues[i-1];
            if (currentValue != null && !currentValue.isEmpty()) {
                preferences.edit().putString(key, currentValue).apply();
            }
        }
    }
    
    private void calculateDrawerTotal() {
        try {
            double total = 0.0;
            
            // Denomination values in cents
            double[] denominations = {0.01, 0.05, 0.10, 0.25, 1.00, 5.00, 10.00, 20.00};
            
            for (int i = 1; i <= 8; i++) {
                EditText closeField = getEditTextById("drawer_close_" + i);
                if (closeField != null) {
                    String quantityStr = closeField.getText().toString().trim();
                    if (!quantityStr.isEmpty()) {
                        try {
                            int quantity = Integer.parseInt(quantityStr);
                            total += quantity * denominations[i - 1];
                        } catch (NumberFormatException e) {
                            // Skip invalid numbers
                        }
                    }
                }
            }
            
            // Update the total display
            if (binding.textDrawerTotal != null) {
                String formattedTotal = String.format("Total: $%.2f", total);
                binding.textDrawerTotal.setText(formattedTotal);
            }
            
            // Check if total is within acceptable range ($125-$126)
            if (total >= 125.0 && total <= 125.99) {
                // Drawer is in correct range - show success indicator
                if (binding.textDrawerTotal != null) {
                    binding.textDrawerTotal.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                }
            } else {
                // Drawer is not in correct range - show warning color (no popup)
                if (binding.textDrawerTotal != null) {
                    binding.textDrawerTotal.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error calculating total: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showSetOpenDrawerDialog() {
        // Create a custom dialog with EditText fields for each drawer denomination
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Set Open Drawer Values");
        
        // Create a custom layout for the dialog
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        
        // Array of denomination labels
        String[] labels = {"Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", "$5 Bills", "$10 Bills", "$20/Rolled Coins"};
        
        // Array to store EditText references
        EditText[] editTexts = new EditText[8];
        
        // Create EditText fields for each denomination
        for (int i = 0; i < 8; i++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            TextView label = new TextView(requireContext());
            label.setText(labels[i] + ": ");
            label.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.4f
            ));
            
            EditText editText = new EditText(requireContext());
            editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            editText.setHint("Enter amount");
            
            // Load previously entered value if it exists
            String previousValue = tempOpenDrawerValues[i];
            if (previousValue != null && !previousValue.isEmpty()) {
                editText.setText(previousValue);
            } else {
                editText.setText(""); // Start with empty field
            }
            
            editText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.6f
            ));
            
            editTexts[i] = editText;
            
            rowLayout.addView(label);
            rowLayout.addView(editText);
            layout.addView(rowLayout);
            
            // Add some spacing between rows
            if (i < 7) {
                View spacer = new View(requireContext());
                spacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    20
                ));
                layout.addView(spacer);
            }
        }
        
        builder.setView(layout);
        
        builder.setPositiveButton("Set Values", (dialog, which) -> {
            // Store values in temporary array for later use during submit
            for (int i = 0; i < 8; i++) {
                String value = editTexts[i].getText().toString();
                tempOpenDrawerValues[i] = value.isEmpty() ? "0" : value;
            }
            
            Toast.makeText(requireContext(), "Open drawer values stored for submission", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }

    private void showDrawerWarningDialog(double total, String type, double difference) {
        String message;
        String title;
        
        if (type.equals("under")) {
            title = "Drawer Total Too Low";
            message = String.format("Drawer total is $%.2f, which is $%.2f under the minimum of $125.00. Do you want to continue?", total, difference);
        } else {
            title = "Drawer Total Too High";
            message = String.format("Drawer total is $%.2f, which is $%.2f over the maximum of $125.00. Do you want to continue?", total, difference);
        }
        
        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Continue", (dialog, which) -> {
                // User chose to continue
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                // User chose to cancel
            })
            .show();
    }
    
    private void setupSafeDecrementButtons() {
        try {
            // Use direct binding for decrement buttons
            if (binding.buttonDecrement1 != null) {
                binding.buttonDecrement1.setOnClickListener(v -> decrementSafeValue(1));
                System.out.println("Button 1 setup successful");
            } else {
                System.out.println("Button 1 NOT FOUND!");
            }
            
            if (binding.buttonDecrement2 != null) {
                binding.buttonDecrement2.setOnClickListener(v -> decrementSafeValue(2));
                System.out.println("Button 2 setup successful");
            } else {
                System.out.println("Button 2 NOT FOUND!");
            }
            
            if (binding.buttonDecrement3 != null) {
                binding.buttonDecrement3.setOnClickListener(v -> decrementSafeValue(3));
                System.out.println("Button 3 setup successful");
            } else {
                System.out.println("Button 3 NOT FOUND!");
            }
            
            if (binding.buttonDecrement4 != null) {
                binding.buttonDecrement4.setOnClickListener(v -> decrementSafeValue(4));
                System.out.println("Button 4 setup successful");
            } else {
                System.out.println("Button 4 NOT FOUND!");
            }
            
            if (binding.buttonDecrement5 != null) {
                binding.buttonDecrement5.setOnClickListener(v -> decrementSafeValue(5));
                System.out.println("Button 5 setup successful");
            } else {
                System.out.println("Button 5 NOT FOUND!");
            }
            
            if (binding.buttonDecrement6 != null) {
                binding.buttonDecrement6.setOnClickListener(v -> decrementSafeValue(6));
                System.out.println("Button 6 setup successful");
            } else {
                System.out.println("Button 6 NOT FOUND!");
            }
            
            if (binding.buttonDecrement7 != null) {
                binding.buttonDecrement7.setOnClickListener(v -> decrementSafeValue(7));
                System.out.println("Button 7 setup successful");
            } else {
                System.out.println("Button 7 NOT FOUND!");
            }
            
            if (binding.buttonDecrement8 != null) {
                binding.buttonDecrement8.setOnClickListener(v -> decrementSafeValue(8));
                System.out.println("Button 8 setup successful");
            } else {
                System.out.println("Button 8 NOT FOUND!");
            }
            
            if (binding.buttonDecrement9 != null) {
                binding.buttonDecrement9.setOnClickListener(v -> decrementSafeValue(9));
                System.out.println("Button 9 setup successful");
            } else {
                System.out.println("Button 9 NOT FOUND!");
            }
        } catch (Exception e) {
            System.out.println("Exception in setupSafeDecrementButtons: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Button getButtonById(String id) {
        try {
            int resourceId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
            System.out.println("Looking for button: " + id + ", resourceId: " + resourceId);
            if (resourceId != 0) {
                Button button = requireView().findViewById(resourceId);
                System.out.println("Button found: " + (button != null));
                return button;
            } else {
                System.out.println("Resource ID is 0 for: " + id);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Exception finding button " + id + ": " + e.getMessage());
            return null;
        }
    }
    
    private void decrementSafeValue(int rowIndex) {
        if (rowIndex < 1 || rowIndex > 9) return;
        
        int arrayIndex = rowIndex - 1;
        String currentValue = safeCloseValues[arrayIndex];
        
        try {
            int currentInt = Integer.parseInt(currentValue);
            if (currentInt > 0) {
                safeCloseValues[arrayIndex] = String.valueOf(currentInt - 1);
                saveSafeCloseValue(rowIndex, safeCloseValues[arrayIndex]);
                updateSafeDisplayDirect(rowIndex);
                calculateAmountOwed();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid safe value at row " + rowIndex, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveSafeCloseValue(int rowIndex, String value) {
        String key = "safe_close_" + rowIndex;
        preferences.edit().putString(key, value).apply();
    }
    
    private void calculateAmountOwed() {
        double totalOwed = 0.0;
        
        // Safe denomination values in dollars - based on actual safe row labels
        // Row 1: Pennies - $0.50 (50 cents)
        // Row 2: Nickels - $2.00 (roll of nickels)
        // Row 3: Dimes - $5.00 (roll of dimes)
        // Row 4: Quarters - $10.00 (roll of quarters)
        // Row 5: $1 Bills - $25.00 (bundle of $1 bills)
        // Row 6: $5 Bills - $25.00 (bundle of $5 bills)
        // Row 7: Pennies (xtra) - $0.50 (50 cents)
        // Row 8: $300 - $300.00
        // Row 9: Xtra $5/10 - $100.00
        double[] denominations = {0.50, 2.00, 5.00, 10.00, 25.00, 25.00, 0.50, 300.00, 100.00};
        
        for (int i = 0; i < 9; i++) {
            try {
                int sessionStart = Integer.parseInt(safeSessionStartValues[i]);
                int current = Integer.parseInt(safeCloseValues[i]);
                int decremented = sessionStart - current;
                
                if (decremented > 0) {
                    double amountForThisRow = decremented * denominations[i];
                    totalOwed += amountForThisRow;
                    System.out.println("Row " + (i+1) + ": " + decremented + " items * $" + denominations[i] + " = $" + amountForThisRow);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error parsing values for row " + (i+1) + ": " + e.getMessage());
            }
        }
        
        if (binding.textAmountOwed != null) {
            binding.textAmountOwed.setText(String.format("Amount Owed: $%.2f", totalOwed));
        }
        
        System.out.println("Total Amount Owed: $" + totalOwed);
    }
    
    private void clearAllSafeValues() {
        // Clear all safe-related SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        for (int i = 1; i <= 9; i++) {
            editor.remove("safe_open_" + i);
            editor.remove("safe_close_" + i);
            editor.remove("safe_session_start_" + i);
        }
        editor.apply();
        
        // Reset arrays to default values
        String[] defaultValues = {"10", "10", "10", "10", "10", "10", "10", "1", "1"};
        for (int i = 0; i < 9; i++) {
            safeOpenValues[i] = defaultValues[i];
            safeCloseValues[i] = defaultValues[i];
            safeSessionStartValues[i] = defaultValues[i];
        }
        
        // Save these default values to SharedPreferences
        for (int i = 1; i <= 9; i++) {
            preferences.edit().putString("safe_open_" + i, safeOpenValues[i-1]).apply();
            preferences.edit().putString("safe_close_" + i, safeCloseValues[i-1]).apply();
            preferences.edit().putString("safe_session_start_" + i, safeSessionStartValues[i-1]).apply();
        }
        
        // Update displays
        updateSafeDisplays();
        
        // Note: Amount owed is NOT reset - it should persist across safe resets
        // The amount owed tracks session decrements, not the safe values themselves
        
        Toast.makeText(requireContext(), "Reset complete", Toast.LENGTH_SHORT).show();
    }
    
    private void updateSafeDisplays() {
        if (binding != null) {
            // Update open displays
            binding.safeOpen1.setText(safeOpenValues[0]);
            binding.safeOpen2.setText(safeOpenValues[1]);
            binding.safeOpen3.setText(safeOpenValues[2]);
            binding.safeOpen4.setText(safeOpenValues[3]);
            binding.safeOpen5.setText(safeOpenValues[4]);
            binding.safeOpen6.setText(safeOpenValues[5]);
            binding.safeOpen7.setText(safeOpenValues[6]);
            binding.safeOpen8.setText(safeOpenValues[7]);
            binding.safeOpen9.setText(safeOpenValues[8]);
            
            // Update close displays
            binding.safeClose1.setText(safeCloseValues[0]);
            binding.safeClose2.setText(safeCloseValues[1]);
            binding.safeClose3.setText(safeCloseValues[2]);
            binding.safeClose4.setText(safeCloseValues[3]);
            binding.safeClose5.setText(safeCloseValues[4]);
            binding.safeClose6.setText(safeCloseValues[5]);
            binding.safeClose7.setText(safeCloseValues[6]);
            binding.safeClose8.setText(safeCloseValues[7]);
            binding.safeClose9.setText(safeCloseValues[8]);
        }
    }
    
    private void updateCashDrawerOpenDisplay() {
        if (binding != null) {
            binding.drawerOpen1.setText(drawerValues[0]);
            binding.drawerOpen2.setText(drawerValues[1]);
            binding.drawerOpen3.setText(drawerValues[2]);
            binding.drawerOpen4.setText(drawerValues[3]);
            binding.drawerOpen5.setText(drawerValues[4]);
            binding.drawerOpen6.setText(drawerValues[5]);
            binding.drawerOpen7.setText(drawerValues[6]);
            binding.drawerOpen8.setText(drawerValues[7]);
        }
    }
    
    private String getDrawerCloseValue(int index) {
        EditText closeField = getEditTextById("drawer_close_" + index);
        return closeField != null ? closeField.getText().toString().trim() : "";
    }
    
    private double getCurrentDrawerTotal() {
        try {
            double total = 0.0;
            
            // Denomination values in cents
            double[] denominations = {0.01, 0.05, 0.10, 0.25, 1.00, 5.00, 10.00, 20.00};
            
            for (int i = 1; i <= 8; i++) {
                EditText closeField = getEditTextById("drawer_close_" + i);
                if (closeField != null) {
                    String quantityStr = closeField.getText().toString().trim();
                    if (!quantityStr.isEmpty()) {
                        try {
                            int quantity = Integer.parseInt(quantityStr);
                            total += quantity * denominations[i - 1];
                        } catch (NumberFormatException e) {
                            // Skip invalid numbers
                        }
                    }
                }
            }
            
            return total;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Data classes for the shift report
    public static class SafeEntry {
        public final int rowNumber;
        public final String denomination;
        public final String openValue;
        public final String closeValue;

        public SafeEntry(int rowNumber, String denomination, String openValue, String closeValue) {
            this.rowNumber = rowNumber;
            this.denomination = denomination;
            this.openValue = openValue;
            this.closeValue = closeValue;
        }
    }

    public static class CashDrawerEntry {
        public final int rowNumber;
        public final String denomination;
        public final String openValue;
        public final String closeValue;

        public CashDrawerEntry(int rowNumber, String denomination, String openValue, String closeValue) {
            this.rowNumber = rowNumber;
            this.denomination = denomination;
            this.openValue = openValue;
            this.closeValue = closeValue;
        }
    }

    public static class SafeDropEntry {
        public final String envelopeNumber;
        public final String amount;
        public final boolean isBankRow;

        public SafeDropEntry(String envelopeNumber, String amount, boolean isBankRow) {
            this.envelopeNumber = envelopeNumber;
            this.amount = amount;
            this.isBankRow = isBankRow;
        }
    }

    public static class ShiftReport {
        public final String employeeName;
        public final List<SafeEntry> safeEntries;
        public final List<CashDrawerEntry> drawerEntries;
        public final List<SafeDropEntry> dropEntries;

        public ShiftReport(String employeeName, List<SafeEntry> safeEntries, List<CashDrawerEntry> drawerEntries, List<SafeDropEntry> dropEntries) {
            this.employeeName = employeeName;
            this.safeEntries = safeEntries;
            this.drawerEntries = drawerEntries;
            this.dropEntries = dropEntries;
        }
    }
}