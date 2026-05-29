package com.example.inventory.utils;

import android.util.Log;

/**
 * Parser for lottery ticket QR codes
 * Extracts game ID, book number, ticket number, and ticket price from QR code data
 */
public class LotteryTicketParser {

    private static final String TAG = "LotteryTicketParser";

    public static class TicketInfo {
        private String gameID;
        private String bookNumber;
        private String ticketNumber;
        private String ticketPrice;
        private String ticketID;
        private String rawCode;
        private boolean isValid;

        public TicketInfo(String rawCode) {
            this.rawCode = rawCode;
            this.isValid = false;
            parseCode(rawCode);
        }

        // Getters
        public String getGameID() { return gameID; }
        public String getBookNumber() { return bookNumber; }
        public String getTicketNumber() { return ticketNumber; }
        public String getTicketPrice() { return ticketPrice; }
        public String getTicketID() { return ticketID; }
        public String getRawCode() { return rawCode; }
        public boolean isValid() { return isValid; }

        public String getGameTypeDescription() {
            if (gameID == null) return "Unknown";
            switch (gameID) {
                case "001": return "Powerball";
                case "002": return "Mega Millions";
                case "003": return "Lotto";
                case "004": return "Scratch-off";
                case "005": return "Daily Game";
                default: return "Game ID " + gameID;
            }
        }

        /**
         * Parse the code and extract ticket information
         * @param code The raw QR code data
         */
        private void parseCode(String code) {
            if (code == null || code.length() < 15) {
                Log.w(TAG, "Code too short or null: " + code);
                return;
            }

            try {
                // Parse according to the specified format:
                // gameID (0-2), bookNumber (4-9), ticketNumber (10-12), ticketPrice (13-14)
                gameID = code.substring(0, 3).trim();
                bookNumber = code.substring(4, 10).trim();
                ticketNumber = code.substring(10, 13).trim();
                ticketPrice = code.substring(13, 15).trim();

                // Pad book number and ticket number with zeros
                bookNumber = String.format("%06d", Integer.parseInt(bookNumber));
                ticketNumber = String.format("%03d", Integer.parseInt(ticketNumber));

                // Create ticket ID
                ticketID = gameID + "-" + bookNumber + "-" + ticketNumber;

                // Validate that we have valid data
                if (isNumeric(gameID) && isNumeric(bookNumber) && isNumeric(ticketNumber) && isNumeric(ticketPrice)) {
                    isValid = true;
                    Log.d(TAG, "Successfully parsed ticket: " + toString());
                } else {
                    Log.w(TAG, "Invalid numeric data in code: " + code);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing code: " + code, e);
            }
        }

        /**
         * Check if a string contains only numeric characters
         * @param str The string to check
         * @return true if numeric, false otherwise
         */
        private boolean isNumeric(String str) {
            if (str == null || str.isEmpty()) {
                return false;
            }
            for (char c : str.toCharArray()) {
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            if (!isValid) {
                return "Invalid Ticket Code";
            }
            return String.format("%s - Book #%s, Ticket #%s ($%s)",
                    getGameTypeDescription(), bookNumber, ticketNumber, ticketPrice);
        }

        /**
         * Get a short summary string
         * @return Short summary of ticket info
         */
        public String getSummaryString() {
            if (!isValid) {
                return "Invalid Code";
            }
            return String.format("%s - Book #%s, Ticket #%s", getGameTypeDescription(), bookNumber, ticketNumber);
        }

        /**
         * Get the book identifier (gameID + bookNumber) for duplicate detection
         * @return Book identifier string
         */
        public String getBookIdentifier() {
            if (!isValid) {
                return null;
            }
            return gameID + "-" + bookNumber;
        }
    }

    /**
     * Parse a lottery ticket QR code
     * @param code The raw QR code data
     * @return TicketInfo object with parsed data
     */
    public static TicketInfo parseTicketCode(String code) {
        return new TicketInfo(code);
    }
}
