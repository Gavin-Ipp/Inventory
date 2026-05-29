package com.example.inventory.ui.lottery;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory.R;
import com.example.inventory.utils.LotteryTicketParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LotteryAdapter extends RecyclerView.Adapter<LotteryAdapter.LotteryViewHolder> {

    private List<String> lotteryCodes = new ArrayList<>();
    private Set<Integer> selectedPositions = new HashSet<>();
    private boolean selectionMode = false;
    private OnSelectionChangedListener selectionChangedListener;
    private Set<String> duplicateCodes = new HashSet<>();
    private Set<String> invalidCodes = new HashSet<>();
    private Set<String> potentialDuplicateBooks = new HashSet<>();
    private Map<String, String> bookStatusMap = new HashMap<>(); // Maps book identifier to status (double/new)
    private static final int MIN_CODE_LENGTH = 15;

    public interface OnSelectionChangedListener {
        void onSelectionChanged();
        void onTicketIDConflictDetected(String ticketID, String newCode, String existingCode);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    @NonNull
    @Override
    public LotteryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lottery_code, parent, false);
        return new LotteryViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull LotteryViewHolder holder, int position) {
        String code = lotteryCodes.get(position);
        boolean isDuplicate = duplicateCodes.contains(code);
        boolean isInvalid = invalidCodes.contains(code);
        
        // Check if this code is part of a potential duplicate game ID with different book numbers
        LotteryTicketParser.TicketInfo ticketInfo = LotteryTicketParser.parseTicketCode(code);
        String gameID = ticketInfo.getGameID();
        boolean isPotentialDuplicateGame = potentialDuplicateBooks.contains(gameID);
        
        // Get the status color for this game
        String gameStatus = bookStatusMap.get(gameID);
        int statusColor = getStatusColor(gameStatus);
        
        holder.bind(code, position + 1, position, selectedPositions.contains(position), selectionMode, 
                   isDuplicate, isInvalid, isPotentialDuplicateGame, statusColor, ticketInfo);
    }

    @Override
    public int getItemCount() {
        return lotteryCodes.size();
    }

    public void updateCodes(List<String> codes) {
        this.lotteryCodes = codes != null ? codes : new ArrayList<>();
        selectedPositions.clear();
        selectionMode = false;
        detectDuplicates();
        detectInvalidCodes();
        detectPotentialDuplicateBooks();
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged();
        }
    }

    public List<String> getLotteryCodes() {
        return new ArrayList<>(lotteryCodes);
    }

    public void toggleSelectionMode() {
        selectionMode = !selectionMode;
        if (!selectionMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged();
        }
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public List<String> getSelectedCodes() {
        List<String> selectedCodes = new ArrayList<>();
        for (Integer position : selectedPositions) {
            if (position < lotteryCodes.size()) {
                selectedCodes.add(lotteryCodes.get(position));
            }
        }
        return selectedCodes;
    }

    public void clearSelected() {
        // Remove selected items from the list
        List<String> newCodes = new ArrayList<>();
        for (int i = 0; i < lotteryCodes.size(); i++) {
            if (!selectedPositions.contains(i)) {
                newCodes.add(lotteryCodes.get(i));
            }
        }
        lotteryCodes = newCodes;
        selectedPositions.clear();
        selectionMode = false;
        detectDuplicates();
        detectInvalidCodes();
        detectPotentialDuplicateBooks();
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged();
        }
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged();
        }
    }

    /**
     * Detect duplicate codes in the list
     */
    private void detectDuplicates() {
        duplicateCodes.clear();
        Set<String> seenCodes = new HashSet<>();
        
        for (String code : lotteryCodes) {
            if (seenCodes.contains(code)) {
                duplicateCodes.add(code);
            } else {
                seenCodes.add(code);
            }
        }
    }

    /**
     * Detect codes that are less than 15 digits
     */
    private void detectInvalidCodes() {
        invalidCodes.clear();
        for (String code : lotteryCodes) {
            if (code.length() < MIN_CODE_LENGTH) {
                invalidCodes.add(code);
            }
        }
    }

    /**
     * Detect when same game ID has different book numbers
     */
    private void detectPotentialDuplicateBooks() {
        potentialDuplicateBooks.clear();
        Map<String, String> gameIDToCodeMap = new HashMap<>();
        
        for (String code : lotteryCodes) {
            LotteryTicketParser.TicketInfo ticketInfo = LotteryTicketParser.parseTicketCode(code);
            if (ticketInfo.isValid()) {
                String gameID = ticketInfo.getGameID();
                String bookNumber = ticketInfo.getBookNumber();
                String gameBookKey = gameID + "-" + bookNumber;
                
                if (gameIDToCodeMap.containsKey(gameID)) {
                    // Found same game ID, check if book number is different
                    String existingCode = gameIDToCodeMap.get(gameID);
                    LotteryTicketParser.TicketInfo existingTicket = LotteryTicketParser.parseTicketCode(existingCode);
                    String existingBookNumber = existingTicket.getBookNumber();
                    
                    if (!bookNumber.equals(existingBookNumber)) {
                        // Same game ID but different book numbers - this is what we want to detect
                        potentialDuplicateBooks.add(gameID);
                        
                        // Notify the listener about the conflict
                        if (selectionChangedListener != null) {
                            selectionChangedListener.onTicketIDConflictDetected(gameID, code, existingCode);
                        }
                    }
                } else {
                    gameIDToCodeMap.put(gameID, code);
                }
            }
        }
    }

    /**
     * Set the status for a game ID conflict (double game or new game)
     * @param gameID The game identifier
     * @param isDoubleGame true if it's a double game, false if it's a new game
     */
    public void setBookStatus(String gameID, boolean isDoubleGame) {
        bookStatusMap.put(gameID, isDoubleGame ? "double" : "new");
        notifyDataSetChanged();
    }

    /**
     * Get the color for a book status
     * @param status The book status ("double", "new", or null)
     * @return Color value
     */
    private int getStatusColor(String status) {
        if ("double".equals(status)) {
            return Color.GREEN;
        } else if ("new".equals(status)) {
            return Color.YELLOW;
        } else {
            return Color.TRANSPARENT;
        }
    }

    /**
     * Get the list of duplicate codes
     * @return List of duplicate codes
     */
    public List<String> getDuplicateCodes() {
        return new ArrayList<>(duplicateCodes);
    }

    /**
     * Get the list of invalid codes
     * @return List of invalid codes
     */
    public List<String> getInvalidCodes() {
        return new ArrayList<>(invalidCodes);
    }

    /**
     * Check if there are any duplicates
     * @return true if duplicates exist, false otherwise
     */
    public boolean hasDuplicates() {
        return !duplicateCodes.isEmpty();
    }

    /**
     * Check if there are any invalid codes
     * @return true if invalid codes exist, false otherwise
     */
    public boolean hasInvalidCodes() {
        return !invalidCodes.isEmpty();
    }

    /**
     * Check if there are any potential duplicate books
     * @return true if potential duplicate books exist, false otherwise
     */
    public boolean hasPotentialDuplicateBooks() {
        return !potentialDuplicateBooks.isEmpty();
    }

    /**
     * Get the list of potential duplicate book identifiers
     * @return List of book identifiers
     */
    public List<String> getPotentialDuplicateBooks() {
        return new ArrayList<>(potentialDuplicateBooks);
    }

    static class LotteryViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewCode;
        private final TextView textViewNumber;
        private final CheckBox checkBox;
        private final LotteryAdapter adapter;

        public LotteryViewHolder(@NonNull View itemView, LotteryAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            textViewCode = itemView.findViewById(R.id.textViewLotteryCode);
            textViewNumber = itemView.findViewById(R.id.textViewLotteryNumber);
            checkBox = itemView.findViewById(R.id.checkBoxLotteryCode);
        }

        public void bind(String code, int number, int position, boolean isSelected, boolean selectionMode, 
                        boolean isDuplicate, boolean isInvalid, boolean isPotentialDuplicateBook, 
                        int statusColor, LotteryTicketParser.TicketInfo ticketInfo) {
            
            // Display parsed information if valid, otherwise show raw code
            if (ticketInfo.isValid()) {
                textViewCode.setText(ticketInfo.getSummaryString());
            } else {
                textViewCode.setText(code);
            }
            
            textViewNumber.setText(String.valueOf(number));
            
            // Set background color based on status (priority: invalid > duplicate > book status > normal)
            if (isInvalid) {
                textViewCode.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light red background
            } else if (isDuplicate) {
                textViewCode.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light red background
            } else if (statusColor != Color.TRANSPARENT) {
                textViewCode.setBackgroundColor(statusColor == Color.GREEN ? 
                    Color.parseColor("#E8F5E8") : Color.parseColor("#FFFDE7")); // Light green/yellow background
            } else if (isPotentialDuplicateBook) {
                textViewCode.setBackgroundColor(Color.parseColor("#FFF3E0")); // Light orange background
            } else {
                textViewCode.setBackgroundColor(Color.TRANSPARENT);
            }
            
            if (selectionMode) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(isSelected);
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    adapter.toggleSelection(position);
                });
            } else {
                checkBox.setVisibility(View.GONE);
                checkBox.setChecked(false);
                checkBox.setOnCheckedChangeListener(null);
            }
        }
    }
}
