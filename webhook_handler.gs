// Function to handle GET requests (for testing)
function doGet(e) {
  return ContentService.createTextOutput("Unified Report Webhook is working!");
}

// Function to handle POST requests (for receiving unified reports)
function doPost(e) {
  try {
    // Log the incoming data for debugging
    console.log("Received unified report data: " + e.postData.contents);
    
    // Parse the incoming JSON data
    var data = JSON.parse(e.postData.contents);
    
    // Log the parsed data structure
    console.log("Parsed report data structure:");
    console.log("- Report ID: " + data.reportId);
    console.log("- Timestamp: " + data.timestamp);
    console.log("- Shift Report saved: " + (data.shiftReport ? "Yes" : "No"));
    console.log("- Tobacco Inventory saved: " + (data.tobaccoInventory ? "Yes" : "No"));
    console.log("- Lottery saved: " + (data.lottery ? "Yes" : "No"));
    
    // Get the spreadsheet
    var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
    
    // Process each section of the report
    var results = {
      shiftReport: processShiftReport(spreadsheet, data.shiftReport),
      tobaccoInventory: processTobaccoInventory(spreadsheet, data.tobaccoInventory),
      lottery: processLottery(spreadsheet, data.lottery)
    };
    
    // Create a summary sheet entry
    createReportSummary(spreadsheet, data, results);
    
    console.log("Processing complete. Results:", JSON.stringify(results));
    
    // Return success response
    return ContentService.createTextOutput(JSON.stringify({
      success: true,
      message: "Unified report processed successfully",
      reportId: data.reportId,
      timestamp: data.timestamp,
      results: results
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    // Log the error
    console.error("Error processing unified report: " + error.toString());
    console.error("Error stack: " + error.stack);
    
    // Return error response
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      error: error.toString(),
      message: "Failed to process unified report"
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

// Function to process Shift Report data
function processShiftReport(spreadsheet, shiftReportData) {
  if (!shiftReportData) {
    console.log("No shift report data to process");
    return { processed: false, message: "No shift report data" };
  }
  
  try {
    console.log("Processing Shift Report...");
    
    // Get or create the Shift Report sheet
    var shiftSheet = spreadsheet.getSheetByName('Shift Reports');
    if (!shiftSheet) {
      shiftSheet = spreadsheet.insertSheet('Shift Reports');
      createShiftReportHeaders(shiftSheet);
    }
    
    var now = new Date();
    var currentRow = shiftSheet.getLastRow() + 1;
    
    // Add the shift report entry
    shiftSheet.getRange(currentRow, 1).setValue(now); // Timestamp
    shiftSheet.getRange(currentRow, 2).setValue(shiftReportData.date || "Not specified"); // Date
    shiftSheet.getRange(currentRow, 3).setValue(shiftReportData.shiftType || "Not specified"); // Shift Type (Open/Close)
    shiftSheet.getRange(currentRow, 4).setValue(shiftReportData.employeeName || "Not specified"); // Employee Name
    shiftSheet.getRange(currentRow, 5).setValue(shiftReportData.amountOwed || "$0.00"); // Amount Owed
    shiftSheet.getRange(currentRow, 6).setValue(shiftReportData.drawerTotal || "Not calculated"); // Drawer Total
    
    // Add safe close values only (no open values in submission)
    var safeCloseValues = shiftReportData.safeCloseValues || {};
    var col = 7; // Start after the basic info columns
    
    // Safe denomination names in order
    var safeNames = ["Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", "$5 Bills", "Pennies (xtra)", "$300", "Xtra $5/10"];
    for (var i = 0; i < safeNames.length; i++) {
      var safeName = safeNames[i];
      shiftSheet.getRange(currentRow, col).setValue(safeCloseValues[safeName] || "0"); // Close only
      col += 1;
    }
    
    // Add drawer close values only (no open values in submission)
    var drawerCloseValues = shiftReportData.drawerCloseValues || {};
    
    // Drawer denomination names in order
    var drawerNames = ["Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", "$5 Bills", "$10 Bills", "$20/ Rolled Coins"];
    for (var i = 0; i < drawerNames.length; i++) {
      var drawerName = drawerNames[i];
      shiftSheet.getRange(currentRow, col).setValue(drawerCloseValues[drawerName] || "0"); // Close only
      col += 1;
    }
    
    // Add safe drops with envelope numbers and amounts
    var safeDrops = shiftReportData.safeDrops || {};
    var safeDropsCount = Object.keys(safeDrops).length;
    shiftSheet.getRange(currentRow, col).setValue(safeDropsCount); // Safe Drops Count
    
    // Add individual safe drops details
    col += 1;
    var dropIndex = 1;
    for (var envelope in safeDrops) {
      if (safeDrops.hasOwnProperty(envelope)) {
        shiftSheet.getRange(currentRow, col).setValue(envelope); // Envelope Number
        shiftSheet.getRange(currentRow, col + 1).setValue(safeDrops[envelope]); // Amount
        col += 2;
        dropIndex++;
        if (dropIndex > 10) break; // Limit to 10 drops per row to avoid overflow
      }
    }
    
    console.log("Shift Report processed successfully");
    return { processed: true, message: "Shift report added to row " + currentRow };
    
  } catch (error) {
    console.error("Error processing shift report: " + error.message);
    return { processed: false, error: error.message };
  }
}

// Function to process Tobacco Inventory data
function processTobaccoInventory(spreadsheet, tobaccoData) {
  if (!tobaccoData) {
    console.log("No tobacco inventory data to process");
    return { processed: false, message: "No tobacco inventory data" };
  }
  
  try {
    console.log("Processing Tobacco Inventory...");
    
    // Get or create the Tobacco Inventory sheet
    var tobaccoSheet = spreadsheet.getSheetByName('Tobacco Inventory');
    if (!tobaccoSheet) {
      tobaccoSheet = spreadsheet.insertSheet('Tobacco Inventory');
      createTobaccoInventoryHeaders(tobaccoSheet);
    }
    
    var now = new Date();
    var currentRow = tobaccoSheet.getLastRow() + 1;
    
    // Add the tobacco inventory entry
    tobaccoSheet.getRange(currentRow, 1).setValue(now); // Timestamp
    
    var tobaccoCloseValues = tobaccoData.tobaccoCloseValues || {};
    
    // Add tobacco close values only (no open values in submission)
    var col = 2;
    var tobaccoProducts = [
      "Shelf 1", "Shelf 2", "Shelf 3", "Shelf 4", "Zig Zag", "Cigar", 
      "Zyn", "Dip/On!", "Cig Cartons", "Zyn Rolls", "Unopened Cigars"
    ];
    
    for (var i = 0; i < tobaccoProducts.length; i++) {
      var product = tobaccoProducts[i];
      tobaccoSheet.getRange(currentRow, col).setValue(tobaccoCloseValues[product] || "0"); // Close only
      col += 1;
    }
    
    console.log("Tobacco Inventory processed successfully");
    return { processed: true, message: "Tobacco inventory added to row " + currentRow };
    
  } catch (error) {
    console.error("Error processing tobacco inventory: " + error.message);
    return { processed: false, error: error.message };
  }
}

// Function to process Lottery data
function processLottery(spreadsheet, lotteryData) {
  if (!lotteryData) {
    console.log("No lottery data to process");
    return { processed: false, message: "No lottery data" };
  }
  
  try {
    console.log("Processing Lottery...");
    
    // Get or create the Lottery sheet
    var lotterySheet = spreadsheet.getSheetByName('Lottery');
    if (!lotterySheet) {
      lotterySheet = spreadsheet.insertSheet('Lottery');
      createLotteryHeaders(lotterySheet);
    }
    
    var codes = lotteryData.lotteryCodes || [];
    var processed = 0;
    var errors = 0;
    
    // Process each lottery code
    for (var i = 0; i < codes.length; i++) {
      try {
        var code = codes[i].toString().trim();
        
        if (code.length < 15) {
          console.log("Skipping lottery code (too short): " + code);
          errors++;
          continue;
        }
        
        // Parse the lottery code
        var gameID = code.substring(0, 3).trim();
        var bookNumber = code.substring(4, 10).padStart(6, '0').trim();
        var ticketNumber = code.substring(10, 13).padStart(3, '0').trim();
        var ticketPrice = code.substring(13, 15).trim();
        var ticketID = gameID + "-" + bookNumber + "-" + ticketNumber;
        
        // Add to the lottery sheet
        var currentRow = lotterySheet.getLastRow() + 1;
        var now = new Date();
        
        lotterySheet.getRange(currentRow, 1).setValue(now); // Date
        lotterySheet.getRange(currentRow, 2).setValue(now.toTimeString().split(' ')[0]); // Time
        lotterySheet.getRange(currentRow, 3).setValue(ticketID); // Ticket ID
        lotterySheet.getRange(currentRow, 4).setValue(ticketNumber); // Ticket Number
        lotterySheet.getRange(currentRow, 5).setValue(gameID); // Game ID
        lotterySheet.getRange(currentRow, 6).setValue(bookNumber); // Book Number
        lotterySheet.getRange(currentRow, 7).setValue(ticketPrice); // Ticket Price
        
        processed++;
        
      } catch (error) {
        console.error("Error processing lottery code " + codes[i] + ": " + error.message);
        errors++;
      }
    }
    
    console.log("Lottery processed: " + processed + " successful, " + errors + " errors");
    return { 
      processed: true, 
      message: "Lottery processed: " + processed + " codes, " + errors + " errors",
      processed: processed,
      errors: errors
    };
    
  } catch (error) {
    console.error("Error processing lottery: " + error.message);
    return { processed: false, error: error.message };
  }
}

// Function to create a report summary entry
function createReportSummary(spreadsheet, data, results) {
  try {
    console.log("Creating report summary...");
    
    // Get or create the Report Summary sheet
    var summarySheet = spreadsheet.getSheetByName('Report Summary');
    if (!summarySheet) {
      summarySheet = spreadsheet.insertSheet('Report Summary');
      createReportSummaryHeaders(summarySheet);
    }
    
    var now = new Date();
    var currentRow = summarySheet.getLastRow() + 1;
    
    // Add the summary entry
    summarySheet.getRange(currentRow, 1).setValue(now); // Timestamp
    summarySheet.getRange(currentRow, 2).setValue(data.reportId); // Report ID
    summarySheet.getRange(currentRow, 3).setValue(data.timestamp); // Original Timestamp
    summarySheet.getRange(currentRow, 4).setValue(results.shiftReport.processed ? "Yes" : "No"); // Shift Report
    summarySheet.getRange(currentRow, 5).setValue(results.tobaccoInventory.processed ? "Yes" : "No"); // Tobacco
    summarySheet.getRange(currentRow, 6).setValue(results.lottery.processed ? "Yes" : "No"); // Lottery
    summarySheet.getRange(currentRow, 7).setValue(JSON.stringify(results)); // Full Results
    
    console.log("Report summary created successfully");
    
  } catch (error) {
    console.error("Error creating report summary: " + error.message);
  }
}

// Helper function to create Shift Report headers
function createShiftReportHeaders(sheet) {
  sheet.getRange('A1').setValue('Timestamp');
  sheet.getRange('B1').setValue('Date');
  sheet.getRange('C1').setValue('Shift Type');
  sheet.getRange('D1').setValue('Employee Name');
  sheet.getRange('E1').setValue('Amount Owed');
  sheet.getRange('F1').setValue('Drawer Total');
  
  // Safe headers (Close only for each denomination)
  var col = 7;
  var safeNames = ["Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", "$5 Bills", "Pennies (xtra)", "$300", "Xtra $5/10"];
  for (var i = 0; i < safeNames.length; i++) {
    sheet.getRange(1, col).setValue(safeNames[i] + ' Close');
    col += 1;
  }
  
  // Drawer headers (Close only for each denomination)
  var drawerNames = ["Pennies", "Nickels", "Dimes", "Quarters", "$1 Bills", "$5 Bills", "$10 Bills", "$20/ Rolled Coins"];
  for (var i = 0; i < drawerNames.length; i++) {
    sheet.getRange(1, col).setValue(drawerNames[i] + ' Close');
    col += 1;
  }
  
  sheet.getRange(1, col).setValue('Safe Drops Count');
  col += 1;
  
  // Safe drops headers (up to 10 drops)
  for (var i = 1; i <= 10; i++) {
    sheet.getRange(1, col).setValue('Drop ' + i + ' Envelope');
    sheet.getRange(1, col + 1).setValue('Drop ' + i + ' Amount');
    col += 2;
  }
  
  // Format headers
  var headerRange = sheet.getRange(1, 1, 1, col - 1);
  headerRange.setFontWeight('bold');
  headerRange.setBackground('#4285f4');
  headerRange.setFontColor('white');
}

// Helper function to create Tobacco Inventory headers
function createTobaccoInventoryHeaders(sheet) {
  sheet.getRange('A1').setValue('Timestamp');
  
  var tobaccoProducts = [
    "Shelf 1", "Shelf 2", "Shelf 3", "Shelf 4", "Zig Zag", "Cigar", 
    "Zyn", "Dip/On!", "Cig Cartons", "Zyn Rolls", "Unopened Cigars"
  ];
  
  var col = 2;
  for (var i = 0; i < tobaccoProducts.length; i++) {
    sheet.getRange(1, col).setValue(tobaccoProducts[i] + ' Close'); // Close only
    col += 1;
  }
  
  // Format headers
  var headerRange = sheet.getRange(1, 1, 1, col - 1);
  headerRange.setFontWeight('bold');
  headerRange.setBackground('#34a853');
  headerRange.setFontColor('white');
}

// Helper function to create Lottery headers
function createLotteryHeaders(sheet) {
  sheet.getRange('A1').setValue('Date');
  sheet.getRange('B1').setValue('Time');
  sheet.getRange('C1').setValue('Ticket ID');
  sheet.getRange('D1').setValue('Ticket Number');
  sheet.getRange('E1').setValue('Game ID');
  sheet.getRange('F1').setValue('Book Number');
  sheet.getRange('G1').setValue('Ticket Price');
  
  // Format headers
  var headerRange = sheet.getRange('A1:G1');
  headerRange.setFontWeight('bold');
  headerRange.setBackground('#ea4335');
  headerRange.setFontColor('white');
}

// Helper function to create Report Summary headers
function createReportSummaryHeaders(sheet) {
  sheet.getRange('A1').setValue('Timestamp');
  sheet.getRange('B1').setValue('Report ID');
  sheet.getRange('C1').setValue('Original Timestamp');
  sheet.getRange('D1').setValue('Shift Report');
  sheet.getRange('E1').setValue('Tobacco Inventory');
  sheet.getRange('F1').setValue('Lottery');
  sheet.getRange('G1').setValue('Full Results');
  
  // Format headers
  var headerRange = sheet.getRange('A1:G1');
  headerRange.setFontWeight('bold');
  headerRange.setBackground('#fbbc04');
  headerRange.setFontColor('white');
}

// Function to check and enable required APIs
function checkAndEnableAPIs() {
  try {
    // Test if we can access the spreadsheet
    var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
    console.log("Spreadsheet access: OK");
    
    // Test if we can use the basic SpreadsheetApp methods
    var sheet = spreadsheet.getActiveSheet();
    var testRange = sheet.getRange('A1');
    testRange.setValue('API Test');
    console.log("Basic SpreadsheetApp access: OK");
    
    return {
      success: true,
      message: "All required APIs are working correctly"
    };
    
  } catch (error) {
    console.error("API check failed:", error);
    return {
      success: false,
      error: error.toString(),
      message: "Basic spreadsheet access failed. Check permissions."
    };
  }
}
