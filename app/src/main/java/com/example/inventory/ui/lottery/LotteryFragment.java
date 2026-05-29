package com.example.inventory.ui.lottery;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.inventory.databinding.FragmentLotteryBinding;
import com.example.inventory.ui.shared.ReportViewModel;
import com.example.inventory.utils.LotteryTicketParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class LotteryFragment extends Fragment {

    private FragmentLotteryBinding binding;
    private LotteryViewModel lotteryViewModel;
    private ReportViewModel reportViewModel;
    private List<String> lotteryCodes = new ArrayList<>();
    private Set<Integer> selectedPositions = new HashSet<>();
    private boolean selectionMode = false;
    private Map<String, View> codeViews = new HashMap<>();
    private Map<String, String> gameIDToLatestCode = new HashMap<>(); // Track latest code for each game ID
    private Set<String> resolvedGameIDs = new HashSet<>(); // Track resolved conflicts
    private Map<String, String> gameIDStatus = new HashMap<>(); // Track status: "double", "new", or null
    
    // Simple input system with 1-second delay
    private static final int LOTTERY_CODE_LENGTH = 29; // Length of a lottery code
    private static final long SCAN_DELAY = 1000; // 1 second delay between scans
    private long lastScanTime = 0; // Track last scan time
    
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        lotteryViewModel = new ViewModelProvider(this).get(LotteryViewModel.class);
        reportViewModel = new ViewModelProvider(requireActivity()).get(ReportViewModel.class);

        binding = FragmentLotteryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupObservers();
        setupClickListeners();
        setupTextInput();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up resources to prevent memory leaks and crashes
        if (binding != null) {
            binding.linearLayoutLotteryCodes.removeAllViews();
        }
        codeViews.clear();
        selectedPositions.clear();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current selection state
        outState.putSerializable("selectedPositions", new ArrayList<>(selectedPositions));
        outState.putBoolean("selectionMode", selectionMode);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore the selection state
            ArrayList<Integer> savedPositions = savedInstanceState.getSerializable("selectedPositions", ArrayList.class);
            if (savedPositions != null) {
                selectedPositions.clear();
                selectedPositions.addAll(savedPositions);
            }
            selectionMode = savedInstanceState.getBoolean("selectionMode", false);
            
            // Update the UI to reflect the restored state
            if (binding != null) {
                updateScrollViewCodes(lotteryCodes);
                updateClearSelectedButtonState();
            }
        }
    }

    private void addCodeToScrollView(String code, int position) {
        if (getContext() == null) return;

        // Create a horizontal LinearLayout to hold checkbox and text
        LinearLayout containerLayout = new LinearLayout(getContext());
        containerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        containerLayout.setOrientation(LinearLayout.HORIZONTAL);
        containerLayout.setPadding(8, 4, 8, 4);
        containerLayout.setBackgroundColor(Color.TRANSPARENT);

        // Create checkbox for selection
        CheckBox checkBox = new CheckBox(getContext());
        checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        checkBox.setChecked(selectedPositions.contains(position));
        
        // Create a TextView for the code
        TextView textView = new TextView(getContext());
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textView.setPadding(16, 8, 16, 8);
        textView.setTextSize(14);

        // Parse the code and display parsed information if valid
        LotteryTicketParser.TicketInfo ticketInfo = LotteryTicketParser.parseTicketCode(code);
        if (ticketInfo.isValid()) {
            textView.setText(String.format("%d. %s", position + 1, ticketInfo.getSummaryString()));
            
            // Apply highlighting based on game ID status
            String gameID = ticketInfo.getGameID();
            String status = gameIDStatus.get(gameID);
            applyHighlighting(textView, status);
        } else {
            textView.setText(String.format("%d. %s", position + 1, code));
            textView.setBackgroundColor(Color.TRANSPARENT);
        }

        // Add checkbox and text to container
        containerLayout.addView(checkBox);
        containerLayout.addView(textView);

        // Set click listeners for selection
        final int finalPosition = position;
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPositions.add(finalPosition);
            } else {
                selectedPositions.remove(finalPosition);
            }
            updateClearSelectedButtonState();
        });

        // Also allow clicking on the text to toggle selection
        textView.setOnClickListener(v -> {
            checkBox.setChecked(!checkBox.isChecked());
        });

        // Add long press to select all codes
        textView.setOnLongClickListener(v -> {
            // Select all codes
            selectedPositions.clear();
            for (int i = 0; i < lotteryCodes.size(); i++) {
                selectedPositions.add(i);
            }
            updateScrollViewCodes(lotteryCodes); // Refresh to show all checkboxes checked
            updateClearSelectedButtonState();
            Toast.makeText(getContext(), "All codes selected", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Store the view reference
        codeViews.put(code, containerLayout);

        // Add to the LinearLayout
        binding.linearLayoutLotteryCodes.addView(containerLayout);
    }

    private void updateScrollViewCodes(List<String> codes) {
        if (binding == null) return; // Prevent crashes if fragment is destroyed
        
        // Clear existing views
        binding.linearLayoutLotteryCodes.removeAllViews();
        codeViews.clear();

        // Add new codes
        for (int i = 0; i < codes.size(); i++) {
            addCodeToScrollView(codes.get(i), i);
        }
        
        // Update the clear selected button state
        updateClearSelectedButtonState();
    }

    private void clearScrollView() {
        binding.linearLayoutLotteryCodes.removeAllViews();
        codeViews.clear();
    }

    /**
     * Apply highlighting to a TextView based on status
     */
    private void applyHighlighting(TextView textView, String status) {
        if ("double".equals(status)) {
            textView.setBackgroundColor(Color.parseColor("#E8F5E8")); // Light green background
        } else if ("new".equals(status)) {
            textView.setBackgroundColor(Color.parseColor("#FFFDE7")); // Light yellow background
        } else {
            textView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * Check if there are any duplicates
     */
    private boolean hasDuplicates() {
        Set<String> seenCodes = new HashSet<>();
        for (String code : lotteryCodes) {
            if (seenCodes.contains(code)) {
                return true;
            }
            seenCodes.add(code);
        }
        return false;
    }

    /**
     * Get the list of duplicate codes
     */
    private List<String> getDuplicateCodes() {
        List<String> duplicateCodes = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        
        for (String code : lotteryCodes) {
            if (seenCodes.contains(code)) {
                duplicateCodes.add(code);
            } else {
                seenCodes.add(code);
            }
        }
        return duplicateCodes;
    }

    /**
     * Check if there are any invalid codes (less than 15 digits)
     */
    private boolean hasInvalidCodes() {
        for (String code : lotteryCodes) {
            if (code.length() < 15) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the list of invalid codes
     */
    private List<String> getInvalidCodes() {
        List<String> invalidCodes = new ArrayList<>();
        for (String code : lotteryCodes) {
            if (code.length() < 15) {
                invalidCodes.add(code);
            }
        }
        return invalidCodes;
    }



    /**
     * Check for potential duplicate books (same game ID, different book numbers)
     */
    private void checkForPotentialDuplicateBooks() {
        Map<String, String> gameIDToCodeMap = new HashMap<>();
        
        for (String code : lotteryCodes) {
            LotteryTicketParser.TicketInfo ticketInfo = LotteryTicketParser.parseTicketCode(code);
            if (ticketInfo.isValid()) {
                String gameID = ticketInfo.getGameID();
                String bookNumber = ticketInfo.getBookNumber();
                
                // Skip if this game ID conflict has already been resolved
                if (resolvedGameIDs.contains(gameID)) {
                    continue;
                }
                
                if (gameIDToCodeMap.containsKey(gameID)) {
                    // Found same game ID, check if book number is different
                    String existingCode = gameIDToCodeMap.get(gameID);
                    LotteryTicketParser.TicketInfo existingTicket = LotteryTicketParser.parseTicketCode(existingCode);
                    String existingBookNumber = existingTicket.getBookNumber();
                    
                    if (!bookNumber.equals(existingBookNumber)) {
                        // Same game ID but different book numbers - show dialog
                        // Store the latest code for this game ID
                        gameIDToLatestCode.put(gameID, code);
                        showTicketIDConflictDialog(gameID, code, existingCode);
                        return; // Only show one dialog at a time
                    }
                } else {
                    gameIDToCodeMap.put(gameID, code);
                }
            }
        }
    }



    private void setupObservers() {
        lotteryViewModel.getSubmitStatus().observe(getViewLifecycleOwner(), status -> {
            if (binding == null) return; // Prevent crashes if fragment is destroyed
            
            switch (status) {
                case SUCCESS:
                    Toast.makeText(getContext(), "Codes submitted successfully via webhook!", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    Toast.makeText(getContext(), "Error submitting codes via webhook", Toast.LENGTH_SHORT).show();
                    break;
                case LOADING:
                    binding.buttonSubmit.setEnabled(false);
                    binding.buttonSubmit.setText("Submitting...");
                    break;
                case IDLE:
                    binding.buttonSubmit.setEnabled(true);
                    break;
            }
        });
        
        // Observe for codes
        lotteryViewModel.getLotteryCodes().observe(getViewLifecycleOwner(), codes -> {
            if (binding == null) return; // Prevent crashes if fragment is destroyed
            
            lotteryCodes = codes != null ? codes : new ArrayList<>();
            updateScrollViewCodes(lotteryCodes);
            updateSubmitButtonState();
                
            // Check for duplicates and show warning
            if (hasDuplicates()) {
                List<String> duplicateCodes = getDuplicateCodes();
                showDuplicateWarning(duplicateCodes);
            }

            // Check for invalid codes and show warning
            if (hasInvalidCodes()) {
                List<String> invalidCodes = getInvalidCodes();
                showInvalidCodeWarning(invalidCodes);
            }



            // Check for potential duplicate books
            checkForPotentialDuplicateBooks();
        });
    }

    private void setupClickListeners() {
        binding.buttonClearSelected.setOnClickListener(v -> {
            if (binding == null) return;

            if (selectedPositions.isEmpty()) {
                Toast.makeText(getContext(), "No codes selected", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> current = new ArrayList<>(lotteryCodes);
            List<Integer> sorted = new ArrayList<>(selectedPositions);
            java.util.Collections.sort(sorted, java.util.Collections.reverseOrder());
            for (int pos : sorted) {
                if (pos < current.size()) current.remove(pos);
            }

            lotteryViewModel.updateCodes(current);

            selectedPositions.clear();
            selectionMode = false;
            updateClearSelectedButtonState();

            Toast.makeText(getContext(), "Selected codes cleared", Toast.LENGTH_SHORT).show();
        });

        binding.buttonSubmit.setOnClickListener(v -> {
            if (binding == null) return; // Prevent crashes if fragment is destroyed
            
            List<String> codes = lotteryViewModel.getLotteryCodes().getValue();
            if (codes != null && !codes.isEmpty()) {
                // Show save confirmation dialog
                showSaveConfirmationDialog(codes);
            } else {
                Toast.makeText(getContext(), "No codes to save", Toast.LENGTH_SHORT).show();
            }
        });

        binding.buttonClear.setOnClickListener(v -> {
            if (binding == null) return; // Prevent crashes if fragment is destroyed
            
            lotteryViewModel.clearCodes();
            selectedPositions.clear();
            selectionMode = false;
            updateClearSelectedButtonState();
        });


    }

    private void setupTextInput() {
        if (binding == null) return; // Prevent crashes if fragment is destroyed
        
        // Automatically focus the input field for continuous scanning
        binding.editTextQrCode.requestFocus();
        
        // Keep focus on the input field for continuous scanning
        binding.editTextQrCode.setOnFocusChangeListener((v, hasFocus) -> {
            if (binding == null) return; // Prevent crashes if fragment is destroyed
            
            if (!hasFocus) {
                // Re-focus after a short delay to maintain continuous scanning
                binding.editTextQrCode.postDelayed(() -> {
                    if (binding != null) {
                        binding.editTextQrCode.requestFocus();
                    }
                }, 100);
            }
        });
        
        binding.editTextQrCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (binding == null) return;
                
                String input = s.toString();
                if (!input.isEmpty()) {
                    // Check for scanner delimiters (newline, carriage return, tab)
                    if (input.endsWith("\n") || input.endsWith("\r") || input.endsWith("\t")) {
                        String code = input.replaceAll("[\n\r\t]", "");
                        if (!code.isEmpty()) {
                            processScannedCode(code);
                        }
                        // Clear the input field for the next scan
                        binding.editTextQrCode.setText("");
                    }
                    // Handle continuous scanning without delimiters
                    else if (input.length() >= LOTTERY_CODE_LENGTH) {
                        processScannedCode(input);
                        // Clear the input field for the next scan
                        binding.editTextQrCode.setText("");
                    }
                }
            }
        });
        
        // Add a manual add button for testing
        binding.editTextQrCode.setOnEditorActionListener((v, actionId, event) -> {
            if (binding == null) return false; // Prevent crashes if fragment is destroyed
            
            String input = binding.editTextQrCode.getText().toString().trim();
            if (!input.isEmpty()) {
                processScannedCode(input);
                binding.editTextQrCode.setText("");
                return true;
            }
            return false;
        });
    }


    
    /**
     * Handle combined codes from rapid scanning
     */

    
    /**
     * Split a combined code into individual lottery codes
     */
    private List<String> splitCombinedCode(String combinedCode) {
        List<String> codes = new ArrayList<>();
        String remaining = combinedCode;
        
        while (remaining.length() >= LOTTERY_CODE_LENGTH) {
            String code = remaining.substring(0, LOTTERY_CODE_LENGTH);
            if (isValidLotteryCode(code)) {
                codes.add(code);
            }
            remaining = remaining.substring(LOTTERY_CODE_LENGTH);
        }
        
        return codes;
    }
    
    /**
     * Check if a code is a valid lottery code
     */
    private boolean isValidLotteryCode(String code) {
        if (code == null || code.length() != LOTTERY_CODE_LENGTH) {
            return false;
        }
        
        // Check if it contains only digits and letters (lottery codes can be alphanumeric)
        return code.matches("[A-Za-z0-9]{" + LOTTERY_CODE_LENGTH + "}");
    }
    
    /**
     * Process a scanned code with 1-second delay protection
     */
    private void processScannedCode(String code) {
        if (code == null || code.length() != LOTTERY_CODE_LENGTH) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Check if enough time has passed since last scan
        if (lastScanTime > 0 && (currentTime - lastScanTime) < SCAN_DELAY) {
            // Too soon - ignore this scan
            Toast.makeText(getContext(), "Scan too fast - please wait 1 second", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Clean the code
        String cleanedCode = cleanCode(code);
        
        if (cleanedCode.isEmpty() || cleanedCode.length() != LOTTERY_CODE_LENGTH) {
            Toast.makeText(getContext(), "Invalid code format", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate the code
        if (!isValidLotteryCode(cleanedCode)) {
            Toast.makeText(getContext(), "Invalid lottery code format", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Add the code
        lotteryViewModel.addLotteryCode(cleanedCode);
        
        // Show parsed information in toast
        LotteryTicketParser.TicketInfo ticketInfo = LotteryTicketParser.parseTicketCode(cleanedCode);
        String toastMessage = ticketInfo.isValid() ? 
            "Scanned: " + ticketInfo.getSummaryString() : 
            "Scanned: " + cleanedCode;
        Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();
        
        // Update last scan time
        lastScanTime = currentTime;
    }

    private String cleanCode(String code) {
        return LotteryTicketParser.cleanCode(code);
    }

    private void updateSubmitButtonState() {
        if (binding == null) return; // Prevent crashes if fragment is destroyed
        
        List<String> codes = lotteryViewModel.getLotteryCodes().getValue();
        binding.buttonSubmit.setEnabled(codes != null && !codes.isEmpty());
    }

    private void updateClearSelectedButtonState() {
        if (binding == null) return; // Prevent crashes if fragment is destroyed
        
        binding.buttonClearSelected.setText("Clear Selected (" + selectedPositions.size() + ")");
    }

    /**
     * Update selection positions after codes are removed
     */
    private void updateSelectionPositions() {
        Set<Integer> newSelectedPositions = new HashSet<>();
        for (Integer position : selectedPositions) {
            if (position < lotteryCodes.size()) {
                newSelectedPositions.add(position);
            }
        }
        selectedPositions.clear();
        selectedPositions.addAll(newSelectedPositions);
        updateClearSelectedButtonState();
    }

    private void showSaveConfirmationDialog(List<String> codes) {
        // Check if there are invalid codes
        if (hasInvalidCodes()) {
            List<String> invalidCodes = getInvalidCodes();
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Cannot Save - Invalid Codes")
                    .setMessage("The following codes are invalid (less than 15 digits) and cannot be saved:\n\n" + String.join("\n", invalidCodes) + "\n\nPlease remove invalid codes before saving.")
                    .setPositiveButton("Remove Invalid Codes", (dialog, which) -> {
                        lotteryViewModel.removeInvalidCodes();
                        Toast.makeText(getContext(), "Invalid codes removed. You can now save.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setCancelable(true)
                    .show();
            return;
        }
        
        String message = "Are you ready to save " + codes.size() + " lottery codes?\n\nThis will save the codes for final submission.";
        
        // Add duplicate warning if duplicates exist
        if (hasDuplicates()) {
            List<String> duplicateCodes = getDuplicateCodes();
            message += "\n\n⚠️ WARNING: " + duplicateCodes.size() + " duplicate codes detected and will be removed:\n" + String.join(", ", duplicateCodes);
        }
        

        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirm Save")
                .setMessage(message)
                .setPositiveButton("Yes, Save", (dialog, which) -> {
                    // User confirmed, proceed with saving to shared ViewModel
                    reportViewModel.saveLotteryData(codes);
                    
                    // Clear the local lottery codes after saving
                    lotteryViewModel.clearCodes();
                    selectedPositions.clear();
                    selectionMode = false;
                    updateClearSelectedButtonState();
                    
                    Toast.makeText(getContext(), "Lottery codes saved successfully! Next: Submit Report", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled, do nothing and return to previous state
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void showDuplicateWarning(List<String> duplicateCodes) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Duplicate Codes Found")
                .setMessage("The following codes are duplicates and will be removed:\n\n" + String.join("\n", duplicateCodes) + "\n\nWould you like to remove duplicates?")
                .setPositiveButton("Yes, Remove Duplicates", (dialog, which) -> {
                    lotteryViewModel.removeDuplicates();
                    Toast.makeText(getContext(), "Removed " + duplicateCodes.size() + " duplicate codes.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No, Keep Duplicates", (dialog, which) -> {
                    // User chose to keep duplicates, do nothing
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void showInvalidCodeWarning(List<String> invalidCodes) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Invalid Codes Found")
                .setMessage("The following codes are invalid (less than 15 digits) and will be removed:\n\n" + String.join("\n", invalidCodes) + "\n\nWould you like to remove invalid codes?")
                .setPositiveButton("Yes, Remove Invalid Codes", (dialog, which) -> {
                    lotteryViewModel.removeInvalidCodes();
                    Toast.makeText(getContext(), "Removed " + invalidCodes.size() + " invalid codes.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No, Keep Invalid Codes", (dialog, which) -> {
                    // User chose to keep invalid codes, do nothing
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }





    private void showTicketIDConflictDialog(String gameID, String newCode, String existingCode) {
        // Parse the codes to show meaningful information
        LotteryTicketParser.TicketInfo newTicket = LotteryTicketParser.parseTicketCode(newCode);
        LotteryTicketParser.TicketInfo existingTicket = LotteryTicketParser.parseTicketCode(existingCode);
        
        String message = String.format(
            "Same Game ID with different Book Numbers detected!\n\n" +
            "Game ID: %s\n\n" +
            "Existing Ticket: %s (Book #%s)\n" +
            "New Ticket: %s (Book #%s)\n\n" +
            "Is this a double game or a new game?",
            gameID,
            existingTicket.isValid() ? existingTicket.getSummaryString() : existingCode,
            existingTicket.getBookNumber(),
            newTicket.isValid() ? newTicket.getSummaryString() : newCode,
            newTicket.getBookNumber()
        );
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Game ID Conflict Detected")
                .setMessage(message)
                .setPositiveButton("Double Game (Green)", (dialog, which) -> {
                    resolvedGameIDs.add(gameID);
                    gameIDStatus.put(gameID, "double");
                    gameIDToLatestCode.remove(gameID);
                    updateScrollViewCodes(lotteryCodes);
                    Toast.makeText(getContext(), "Marked as double game", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("New Game (Yellow)", (dialog, which) -> {
                    // Mark this game ID as resolved and set status
                    resolvedGameIDs.add(gameID);
                    gameIDStatus.put(gameID, "new");
                    gameIDToLatestCode.remove(gameID);
                    Toast.makeText(getContext(), "Marked as new game", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Skip", (dialog, which) -> {
                    // Mark this game ID as resolved (no status set)
                    resolvedGameIDs.add(gameID);
                    gameIDToLatestCode.remove(gameID);
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
    
}
