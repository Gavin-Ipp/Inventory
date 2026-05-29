package com.example.inventory.ui.dashboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.inventory.databinding.FragmentDashboardBinding;
import com.example.inventory.ui.shared.ReportViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private ReportViewModel reportViewModel;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "TobaccoInventoryPrefs";
    
    // Shared data structure for tobacco values - both open and close display from this
    private String[] tobaccoValues = new String[11]; // Index 0-10 for rows 1-11

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        android.util.Log.d("DashboardFragment", "onCreateView called - DashboardFragment is being created");
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        reportViewModel = new ViewModelProvider(requireActivity()).get(ReportViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        try {
            // Initialize SharedPreferences
            sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, 0);
            
            // Setup the fragment
            setupTobaccoInventory();
            
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error in onCreateView: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing tobacco inventory: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return root;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("DashboardFragment", "onResume called - Fragment is now visible");
        android.util.Log.d("DashboardFragment", "Fragment state - isAdded: " + isAdded() + ", isVisible: " + isVisible());
        
        // Refresh the UI when fragment becomes visible
        try {
            if (binding != null) {
                android.util.Log.d("DashboardFragment", "Refreshing UI in onResume");
                loadOpenValuesOnResume(); // Reload data without clearing close values
            }
        } catch (Exception e) {
            android.util.Log.e("DashboardFragment", "Error refreshing UI in onResume: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        android.util.Log.d("DashboardFragment", "onPause called - Fragment is no longer visible");
    }

    private void setupTobaccoInventory() {
        try {
            // Load saved open values
            loadOpenValues();
            
            // Setup decrement buttons
            setupTobaccoDecrementButtons();
            
            // Setup action buttons
            setupActionButtons();
            
            // Setup TextWatchers for manual editing
            setupTobaccoTextWatchers();
            
            // Setup clear fields observer
            setupClearFieldsObserver();
            
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error in setupTobaccoInventory: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up tobacco inventory: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupTobaccoDecrementButtons() {
        try {
            // Setup decrement buttons using direct references
            if (binding.buttonDecrementTobacco1 != null) {
                binding.buttonDecrementTobacco1.setOnClickListener(v -> decrementTobaccoValue(1));
            }
            
            if (binding.buttonDecrementTobacco2 != null) {
                binding.buttonDecrementTobacco2.setOnClickListener(v -> decrementTobaccoValue(2));
            }
            
            if (binding.buttonDecrementTobacco3 != null) {
                binding.buttonDecrementTobacco3.setOnClickListener(v -> decrementTobaccoValue(3));
            }
            
            if (binding.buttonDecrementTobacco4 != null) {
                binding.buttonDecrementTobacco4.setOnClickListener(v -> decrementTobaccoValue(4));
            }
            
            if (binding.buttonDecrementTobacco5 != null) {
                binding.buttonDecrementTobacco5.setOnClickListener(v -> decrementTobaccoValue(5));
            }
            
            if (binding.buttonDecrementTobacco6 != null) {
                binding.buttonDecrementTobacco6.setOnClickListener(v -> decrementTobaccoValue(6));
            }
            
            if (binding.buttonDecrementTobacco7 != null) {
                binding.buttonDecrementTobacco7.setOnClickListener(v -> decrementTobaccoValue(7));
            }
            
            if (binding.buttonDecrementTobacco8 != null) {
                binding.buttonDecrementTobacco8.setOnClickListener(v -> decrementTobaccoValue(8));
            }
            
            if (binding.buttonDecrementTobacco9 != null) {
                binding.buttonDecrementTobacco9.setOnClickListener(v -> decrementTobaccoValue(9));
            }
            
            if (binding.buttonDecrementTobacco10 != null) {
                binding.buttonDecrementTobacco10.setOnClickListener(v -> decrementTobaccoValue(10));
            }
            
            if (binding.buttonDecrementTobacco11 != null) {
                binding.buttonDecrementTobacco11.setOnClickListener(v -> decrementTobaccoValue(11));
            }
            
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error in setupTobaccoDecrementButtons: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up decrement buttons: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    private void setupActionButtons() {
        try {
            // Submit button
            if (binding.buttonSubmitTobacco != null) {
                binding.buttonSubmitTobacco.setOnClickListener(v -> submitTobaccoInventory());
            }
            
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error in setupActionButtons: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up action buttons: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void submitTobaccoInventory() {
        try {
            if (binding == null) return;
            
            // Collect tobacco inventory data
            List<TobaccoEntry> tobaccoEntries = collectTobaccoData();
            
            // Submit to local ViewModel for logging
            dashboardViewModel.submitTobaccoInventory(tobaccoEntries);
            
            // Convert data to Maps for shared ViewModel
            Map<String, String> tobaccoOpenMap = new HashMap<>();
            Map<String, String> tobaccoCloseMap = new HashMap<>();
            
            // Convert tobacco data - collect close values from EditText fields
            String[] tobaccoLabels = {
                "Shelf 1", "Shelf 2", "Shelf 3", "Shelf 4", "Zig Zag", "Cigar", 
                "Zyn", "Dip/On!", "Cig Cartons", "Zyn Rolls", "Unopened Cigars"
            };
            
            // Collect close values from EditText fields
            String[] closeValues = {
                binding.tobaccoClose1.getText().toString().trim(),
                binding.tobaccoClose2.getText().toString().trim(),
                binding.tobaccoClose3.getText().toString().trim(),
                binding.tobaccoClose4.getText().toString().trim(),
                binding.tobaccoClose5.getText().toString().trim(),
                binding.tobaccoClose6.getText().toString().trim(),
                binding.tobaccoClose7.getText().toString().trim(),
                binding.tobaccoClose8.getText().toString().trim(),
                binding.tobaccoClose9.getText().toString().trim(),
                binding.tobaccoClose10.getText().toString().trim(),
                binding.tobaccoClose11.getText().toString().trim()
            };
            
            for (int i = 0; i < 11; i++) {
                tobaccoOpenMap.put(tobaccoLabels[i], tobaccoValues[i]); // Current open values
                tobaccoCloseMap.put(tobaccoLabels[i], closeValues[i]); // Close values from EditText
            }
            
            // Save to shared ViewModel
            reportViewModel.saveTobaccoInventoryData(tobaccoOpenMap, tobaccoCloseMap);
            
            // Set open values to the close values that were just entered
            for (int i = 0; i < 11; i++) {
                tobaccoValues[i] = closeValues[i]; // Set open to close values
            }
            
            // Update open display to show the new open values
            updateOpenDisplay();
            
            // Save the new open values to persistent storage
            copyCloseValuesToOpen();
            
            // Don't clear tobacco close values on save - they should persist until final submission
            // if (binding != null) {
            //     binding.tobaccoClose1.setText("");
            //     binding.tobaccoClose2.setText("");
            //     binding.tobaccoClose3.setText("");
            //     binding.tobaccoClose4.setText("");
            //     binding.tobaccoClose5.setText("");
            //     binding.tobaccoClose6.setText("");
            //     binding.tobaccoClose7.setText("");
            //     binding.tobaccoClose8.setText("");
            //     binding.tobaccoClose9.setText("");
            //     binding.tobaccoClose10.setText("");
            //     binding.tobaccoClose11.setText("");
            // }
            
            Toast.makeText(requireContext(), "Tobacco inventory saved successfully! Next: Lottery", Toast.LENGTH_LONG).show();
            
            // Provide subtle guidance without forcing navigation
            showNextStepGuidance("Lottery");
            
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error in submitTobaccoInventory: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error saving tobacco inventory: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<TobaccoEntry> collectTobaccoData() {
        List<TobaccoEntry> entries = new ArrayList<>();
        
        // Tobacco product labels in order
        String[] tobaccoLabels = {
            "Shelf 1", "Shelf 2", "Shelf 3", "Shelf 4", "Zig Zag", "Cigar", 
            "Zyn", "Dip/On!", "Cig Cartons", "Zyn Rolls", "Unopened Cigars"
        };
        
        // Collect data from shared data structure
        for (int i = 1; i <= 11; i++) {
            String currentValue = tobaccoValues[i-1];
            if (currentValue != null && !currentValue.isEmpty()) {
                entries.add(new TobaccoEntry(i, tobaccoLabels[i-1], currentValue, currentValue));
            }
        }
        
        return entries;
    }

    private void copyCloseValuesToOpen() {
        try {
            // Save current shared tobacco values to persistent storage
            for (int i = 1; i <= 11; i++) {
                String key = "tobacco_open_" + i;
                String currentValue = tobaccoValues[i-1];
                if (currentValue != null && !currentValue.isEmpty()) {
                    sharedPreferences.edit().putString(key, currentValue).apply();
                }
            }
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error in copyCloseValuesToOpen: " + e.getMessage(), e);
        }
    }

    private void setupTobaccoTextWatchers() {
        try {
            // Setup TextWatchers for all close EditText fields
            if (binding != null) {
                binding.tobaccoClose1.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[0] = value;
                        sharedPreferences.edit().putString("tobacco_close_1", value).apply();
                    }
                });
                
                binding.tobaccoClose2.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[1] = value;
                        sharedPreferences.edit().putString("tobacco_close_2", value).apply();
                    }
                });
                
                binding.tobaccoClose3.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[2] = value;
                        sharedPreferences.edit().putString("tobacco_close_3", value).apply();
                    }
                });
                
                binding.tobaccoClose4.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[3] = value;
                        sharedPreferences.edit().putString("tobacco_close_4", value).apply();
                    }
                });
                
                binding.tobaccoClose5.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[4] = value;
                        sharedPreferences.edit().putString("tobacco_close_5", value).apply();
                    }
                });
                
                binding.tobaccoClose6.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[5] = value;
                        sharedPreferences.edit().putString("tobacco_close_6", value).apply();
                    }
                });
                
                binding.tobaccoClose7.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[6] = value;
                        sharedPreferences.edit().putString("tobacco_close_7", value).apply();
                    }
                });
                
                binding.tobaccoClose8.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[7] = value;
                        sharedPreferences.edit().putString("tobacco_close_8", value).apply();
                    }
                });
                
                binding.tobaccoClose9.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[8] = value;
                        sharedPreferences.edit().putString("tobacco_close_9", value).apply();
                    }
                });
                
                binding.tobaccoClose10.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[9] = value;
                        sharedPreferences.edit().putString("tobacco_close_10", value).apply();
                    }
                });
                
                binding.tobaccoClose11.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        // Save the value to SharedPreferences for persistence
                        String value = s.toString();
                        tobaccoValues[10] = value;
                        sharedPreferences.edit().putString("tobacco_close_11", value).apply();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error in setupTobaccoTextWatchers: " + e.getMessage(), e);
        }
    }
    
    private void showNextStepGuidance(String nextStep) {
        try {
            android.util.Log.d("DashboardFragment", "Showing guidance for next step: " + nextStep);
            
            // Show a subtle guidance message
            if (getActivity() != null) {
                // You can customize this to show a more prominent guidance UI
                // For now, we'll just log it and the Toast message already shows the guidance
                android.util.Log.d("DashboardFragment", "Next step guidance: " + nextStep);
            }
        } catch (Exception e) {
            android.util.Log.e("DashboardFragment", "Error showing guidance: " + e.getMessage(), e);
        }
    }
    
    private void setupClearFieldsObserver() {
        // Observe for clear fields signal from successful final submission
        reportViewModel.getClearTobaccoFields().observe(getViewLifecycleOwner(), shouldClear -> {
            if (shouldClear != null && shouldClear) {
                android.util.Log.d("DashboardFragment", "Clearing Tobacco fields after final submission");
                clearTobaccoCloseValues();
                // Reset the signal
                reportViewModel.resetClearTobaccoFields();
            }
        });
    }
    
    private void clearTobaccoCloseValues() {
        // Clear tobacco close values
        if (binding != null) {
            binding.tobaccoClose1.setText("");
            binding.tobaccoClose2.setText("");
            binding.tobaccoClose3.setText("");
            binding.tobaccoClose4.setText("");
            binding.tobaccoClose5.setText("");
            binding.tobaccoClose6.setText("");
            binding.tobaccoClose7.setText("");
            binding.tobaccoClose8.setText("");
            binding.tobaccoClose9.setText("");
            binding.tobaccoClose10.setText("");
            binding.tobaccoClose11.setText("");
        }
        
        // Clear tobacco close values from SharedPreferences
        for (int i = 1; i <= 11; i++) {
            sharedPreferences.edit().putString("tobacco_close_" + i, "").apply();
        }
    }

    private void loadOpenValues() {
        try {
            // Load tobacco values into shared data structure
            for (int i = 1; i <= 11; i++) {
                String key = "tobacco_open_" + i;
                // No default values - load saved value or empty string
                String savedValue = sharedPreferences.getString(key, "");
                tobaccoValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Use direct binding to update displays
            if (binding != null) {
                // Set initial open display only
                binding.tobaccoOpen1.setText(tobaccoValues[0]);
                binding.tobaccoOpen2.setText(tobaccoValues[1]);
                binding.tobaccoOpen3.setText(tobaccoValues[2]);
                binding.tobaccoOpen4.setText(tobaccoValues[3]);
                binding.tobaccoOpen5.setText(tobaccoValues[4]);
                binding.tobaccoOpen6.setText(tobaccoValues[5]);
                binding.tobaccoOpen7.setText(tobaccoValues[6]);
                binding.tobaccoOpen8.setText(tobaccoValues[7]);
                binding.tobaccoOpen9.setText(tobaccoValues[8]);
                binding.tobaccoOpen10.setText(tobaccoValues[9]);
                binding.tobaccoOpen11.setText(tobaccoValues[10]);
                
                // Set close fields to empty on app start
                binding.tobaccoClose1.setText("");
                binding.tobaccoClose2.setText("");
                binding.tobaccoClose3.setText("");
                binding.tobaccoClose4.setText("");
                binding.tobaccoClose5.setText("");
                binding.tobaccoClose6.setText("");
                binding.tobaccoClose7.setText("");
                binding.tobaccoClose8.setText("");
                binding.tobaccoClose9.setText("");
                binding.tobaccoClose10.setText("");
                binding.tobaccoClose11.setText("");
            }
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error in loadOpenValues: " + e.getMessage(), e);
        }
    }
    
    private void loadOpenValuesOnResume() {
        try {
            android.util.Log.d("DashboardFragment", "loadOpenValuesOnResume called - preserving close values");
            
            // Load tobacco open values into shared data structure
            for (int i = 1; i <= 11; i++) {
                String key = "tobacco_open_" + i;
                String savedValue = sharedPreferences.getString(key, "");
                tobaccoValues[i-1] = savedValue; // Store in shared data structure
            }
            
            // Update open display
            updateOpenDisplay();
            
            // Load and restore close values from SharedPreferences
            if (binding != null) {
                binding.tobaccoClose1.setText(sharedPreferences.getString("tobacco_close_1", ""));
                binding.tobaccoClose2.setText(sharedPreferences.getString("tobacco_close_2", ""));
                binding.tobaccoClose3.setText(sharedPreferences.getString("tobacco_close_3", ""));
                binding.tobaccoClose4.setText(sharedPreferences.getString("tobacco_close_4", ""));
                binding.tobaccoClose5.setText(sharedPreferences.getString("tobacco_close_5", ""));
                binding.tobaccoClose6.setText(sharedPreferences.getString("tobacco_close_6", ""));
                binding.tobaccoClose7.setText(sharedPreferences.getString("tobacco_close_7", ""));
                binding.tobaccoClose8.setText(sharedPreferences.getString("tobacco_close_8", ""));
                binding.tobaccoClose9.setText(sharedPreferences.getString("tobacco_close_9", ""));
                binding.tobaccoClose10.setText(sharedPreferences.getString("tobacco_close_10", ""));
                binding.tobaccoClose11.setText(sharedPreferences.getString("tobacco_close_11", ""));
            }
            
            android.util.Log.d("DashboardFragment", "loadOpenValuesOnResume completed successfully");
        } catch (Exception e) {
            android.util.Log.e("DashboardFragment", "Error in loadOpenValuesOnResume: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    private void updateTobaccoDisplay(int rowIndex) {
        // Only update close display from shared data structure
        // Open display stays visually static until submit
        if (binding != null) {
            String value = tobaccoValues[rowIndex-1];
            switch (rowIndex) {
                case 1: binding.tobaccoClose1.setText(value); break;
                case 2: binding.tobaccoClose2.setText(value); break;
                case 3: binding.tobaccoClose3.setText(value); break;
                case 4: binding.tobaccoClose4.setText(value); break;
                case 5: binding.tobaccoClose5.setText(value); break;
                case 6: binding.tobaccoClose6.setText(value); break;
                case 7: binding.tobaccoClose7.setText(value); break;
                case 8: binding.tobaccoClose8.setText(value); break;
                case 9: binding.tobaccoClose9.setText(value); break;
                case 10: binding.tobaccoClose10.setText(value); break;
                case 11: binding.tobaccoClose11.setText(value); break;
            }
        }
    }
    
    private void updateOpenDisplay() {
        // Update all open displays to match current shared values
        // This is called when submit is pressed
        if (binding != null) {
            binding.tobaccoOpen1.setText(tobaccoValues[0]);
            binding.tobaccoOpen2.setText(tobaccoValues[1]);
            binding.tobaccoOpen3.setText(tobaccoValues[2]);
            binding.tobaccoOpen4.setText(tobaccoValues[3]);
            binding.tobaccoOpen5.setText(tobaccoValues[4]);
            binding.tobaccoOpen6.setText(tobaccoValues[5]);
            binding.tobaccoOpen7.setText(tobaccoValues[6]);
            binding.tobaccoOpen8.setText(tobaccoValues[7]);
            binding.tobaccoOpen9.setText(tobaccoValues[8]);
            binding.tobaccoOpen10.setText(tobaccoValues[9]);
            binding.tobaccoOpen11.setText(tobaccoValues[10]);
        }
    }
    
    private void decrementTobaccoValue(int rowIndex) {
        try {
            // Get current value from shared data structure
            String currentValueStr = tobaccoValues[rowIndex-1];
            if (currentValueStr != null && !currentValueStr.isEmpty()) {
                int currentValue = Integer.parseInt(currentValueStr);
                if (currentValue > 0) {
                    // Decrement the value in shared data structure
                    tobaccoValues[rowIndex-1] = String.valueOf(currentValue - 1);
                    // Update both displays
                    updateTobaccoDisplay(rowIndex);
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }



    private TextView getTextViewById(String id) {
        try {
            int resourceId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
            if (resourceId != 0) {
                return requireView().findViewById(resourceId);
            }
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error getting TextView by ID " + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    private EditText getEditTextById(String id) {
        try {
            int resourceId = getResources().getIdentifier(id, "id", requireContext().getPackageName());
            if (resourceId != 0) {
                return requireView().findViewById(resourceId);
            }
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error getting EditText by ID " + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Data class for tobacco entries
    public static class TobaccoEntry {
        private int rowNumber;
        private String productName;
        private String openValue;
        private String closeValue;

        public TobaccoEntry(int rowNumber, String productName, String openValue, String closeValue) {
            this.rowNumber = rowNumber;
            this.productName = productName;
            this.openValue = openValue;
            this.closeValue = closeValue;
        }

        // Getters
        public int getRowNumber() { return rowNumber; }
        public String getProductName() { return productName; }
        public String getOpenValue() { return openValue; }
        public String getCloseValue() { return closeValue; }
    }
}