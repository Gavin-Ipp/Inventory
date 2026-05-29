package com.example.inventory.data;

import java.util.List;
import java.util.Map;

/**
 * Data class to hold all report data from Shift Report, Tobacco Inventory, and Lottery
 * This will be used for the unified submission system
 */
public class ReportData {
    
    // Shift Report Data
    public static class ShiftReportData {
        public Map<String, String> safeOpenValues;
        public Map<String, String> safeCloseValues;
        public Map<String, String> drawerOpenValues;
        public Map<String, String> drawerCloseValues;
        public Map<String, String> safeDrops;
        public String employeeName;
        public String drawerTotal;
        public String amountOwed;
        public String date;
        public String shiftType; // "Open" or "Close"
        public boolean isSaved;
        
        public ShiftReportData() {
            this.isSaved = false;
        }
    }
    
    // Tobacco Inventory Data
    public static class TobaccoInventoryData {
        public Map<String, String> tobaccoOpenValues;
        public Map<String, String> tobaccoCloseValues;
        public boolean isSaved;
        
        public TobaccoInventoryData() {
            this.isSaved = false;
        }
    }
    
    // Lottery Data
    public static class LotteryData {
        public List<String> lotteryCodes;
        public boolean isSaved;
        
        public LotteryData() {
            this.isSaved = false;
        }
    }
    
    // Main report data container
    public ShiftReportData shiftReport;
    public TobaccoInventoryData tobaccoInventory;
    public LotteryData lottery;
    public String timestamp;
    public String reportId;
    
    public ReportData() {
        this.shiftReport = new ShiftReportData();
        this.tobaccoInventory = new TobaccoInventoryData();
        this.lottery = new LotteryData();
        this.timestamp = String.valueOf(System.currentTimeMillis());
        this.reportId = "REPORT_" + System.currentTimeMillis();
    }
    
    /**
     * Check if all sections have been saved
     */
    public boolean isComplete() {
        return shiftReport.isSaved && tobaccoInventory.isSaved && lottery.isSaved;
    }
    
    /**
     * Get a summary of what's been saved
     */
    public String getSaveStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Shift Report: ").append(shiftReport.isSaved ? "✓ Saved" : "✗ Not Saved").append("\n");
        status.append("Tobacco Inventory: ").append(tobaccoInventory.isSaved ? "✓ Saved" : "✗ Not Saved").append("\n");
        status.append("Lottery: ").append(lottery.isSaved ? "✓ Saved" : "✗ Not Saved");
        return status.toString();
    }
    
    /**
     * Convert to JSON-like string for webhook submission
     */
    public String toJsonString() {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"reportId\": \"").append(escapeJsonString(reportId)).append("\",\n");
            json.append("  \"timestamp\": \"").append(escapeJsonString(timestamp)).append("\",\n");
            json.append("  \"shiftReport\": {\n");
            // Only include close values in submission, not open values
            json.append("    \"safeCloseValues\": ").append(mapToJson(shiftReport.safeCloseValues)).append(",\n");
            json.append("    \"drawerCloseValues\": ").append(mapToJson(shiftReport.drawerCloseValues)).append(",\n");
            json.append("    \"safeDrops\": ").append(mapToJson(shiftReport.safeDrops)).append(",\n");
            json.append("    \"employeeName\": \"").append(escapeJsonString(shiftReport.employeeName)).append("\",\n");
            json.append("    \"drawerTotal\": \"").append(escapeJsonString(shiftReport.drawerTotal)).append("\",\n");
            json.append("    \"amountOwed\": \"").append(escapeJsonString(shiftReport.amountOwed)).append("\",\n");
            json.append("    \"date\": \"").append(escapeJsonString(shiftReport.date)).append("\",\n");
            json.append("    \"shiftType\": \"").append(escapeJsonString(shiftReport.shiftType)).append("\"\n");
            json.append("  },\n");
            json.append("  \"tobaccoInventory\": {\n");
            // Only include close values in submission, not open values
            json.append("    \"tobaccoCloseValues\": ").append(mapToJson(tobaccoInventory.tobaccoCloseValues)).append("\n");
            json.append("  },\n");
            json.append("  \"lottery\": {\n");
            json.append("    \"lotteryCodes\": ").append(listToJson(lottery.lotteryCodes)).append("\n");
            json.append("  }\n");
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            // Return a safe fallback JSON if there's any error
            return "{\"error\": \"Failed to generate report JSON\", \"message\": \"" + escapeJsonString(e.getMessage()) + "\"}";
        }
    }
    
    private String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) json.append(", ");
            String key = entry.getKey() != null ? entry.getKey() : "";
            String value = entry.getValue() != null ? entry.getValue() : "";
            json.append("\"").append(escapeJsonString(key)).append("\": \"").append(escapeJsonString(value)).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    private String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) json.append(", ");
            String item = list.get(i) != null ? list.get(i) : "";
            json.append("\"").append(escapeJsonString(item)).append("\"");
        }
        json.append("]");
        return json.toString();
    }
    
    /**
     * Escape special characters in JSON strings
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
