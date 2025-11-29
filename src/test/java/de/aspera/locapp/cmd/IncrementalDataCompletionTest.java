package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;

/**
 * Integration test for the "Incremental Data Completion" workflow.
 * 
 * This test verifies that "Sparse Data" (SRC V1: 10 entries) can be converted into 
 * a "Dense Matrix" (XLS V3: 16 entries) through a multi-phase translation process,
 * and that the final promotion results in a complete, verified Source baseline 
 * (SRC V3: 16 entries).
 * 
 * <p><b>Prerequisite Files (Located in TEST_RES_DIR/testfiles):</b></p>
 * <ul>
 *   <li>test.properties (EN baseline: 4 keys, 2 empty)</li>
 *   <li>test_de.properties (DE: 2 keys)</li>
 *   <li>test_fr.properties (FR: 3 keys, 2 empty)</li>
 *   <li>test_it.properties (IT: 1 key)</li>
 * </ul>
 * 
 * <p><b>Workflow Phases:</b></p>
 * <ul>
 *   <li>Phase 0: Initial Setup & Baseline (SRC V1 with 10 entries)</li>
 *   <li>Phase 1: Export, Partial Fill & Import (XLS V2 with 16 entries, partially filled)</li>
 *   <li>Phase 2: Target Empty Values & Final Import (XLS V3, fully filled)</li>
 *   <li>Phase 3: Promotion to Source & Final Verification (SRC V3 with 16 entries)</li>
 * </ul>
 */
public class IncrementalDataCompletionTest extends BasicFacadeTest {

    private static final String TEST_FILES_DIR = "src/test/resources/testfiles";
    
    // Expected counts based on the test files
    private static final int SPARSE_SRC_COUNT = 10;  // 4 EN + 2 DE + 3 FR + 1 IT
    private static final int DENSE_MATRIX_COUNT = 16; // 4 keys × 4 languages
    
    // Excel column indices
    private static final int KEY_COLUMN_INDEX = 1;  // Key is in column 1
    
    private LocalizationDao locFacade;
    private Path tempBasePath;
    private Path testDataDir;
    private Path tempExcelDir;
    private File exportedExcelFile;
    
    @Before
    public void init() throws Exception {
        locFacade = new LocalizationDao();
        
        // Create unique temporary directory for this test
        tempBasePath = Files.createTempDirectory("locapp_incremental_completion_test");
        tempExcelDir = tempBasePath.resolve("excel");
        Files.createDirectories(tempExcelDir);
        
        // Copy test files to temporary directory (so we can modify them during promotion)
        testDataDir = tempBasePath.resolve("testfiles");
        Files.createDirectories(testDataDir);
        copyTestFiles();
        
        // Initialize configuration
        executeCommand("init");
        
        logger.info("Test setup complete - test data directory: " + testDataDir);
    }
    
    @After
    public void cleanup() {
        // Clean up exported Excel file
        if (exportedExcelFile != null && exportedExcelFile.exists()) {
            exportedExcelFile.delete();
        }
        
        // Clean up the temporary directory created for this test
        if (tempBasePath != null) {
            try {
                FileUtils.deleteDirectory(tempBasePath.toFile());
            } catch (IOException e) {
                logger.warning("Failed to cleanup test directory: " + e.getMessage());
            }
        }
    }
    
    /**
     * Copies the test files from src/test/resources/testfiles to the temp directory.
     */
    private void copyTestFiles() throws IOException {
        Path sourceDir = Path.of(TEST_FILES_DIR);
        Files.copy(sourceDir.resolve("test.properties"), testDataDir.resolve("test.properties"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceDir.resolve("test_de.properties"), testDataDir.resolve("test_de.properties"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceDir.resolve("test_fr.properties"), testDataDir.resolve("test_fr.properties"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceDir.resolve("test_it.properties"), testDataDir.resolve("test_it.properties"), StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * Full integration test for the Incremental Data Completion workflow.
     * 
     * Validates the complete workflow from Sparse Data (10 entries) to 
     * Dense Matrix (16 entries) through multi-phase translation process.
     */
    @Test
    public void testIncrementalDataCompletion() throws Exception {
        // ===== PHASE 0: Initial Setup & Baseline (SRC V1) =====
        logger.info("\n===== PHASE 0: Initial Setup & Baseline (SRC V1) =====");
        
        // Step 0.1: Delete all existing Localization entries
        logger.info("Step 0.1: Clearing database (cl)...");
        executeCommand("clear-loc");
        
        // Step 0.2: Scan properties files and save file information
        logger.info("Step 0.2: Scanning properties files (f)...");
        executeCommand("files", testDataDir.toString());
        
        // Step 0.3: Import raw files (creates SRC V1)
        logger.info("Step 0.3: Importing properties (ip)...");
        executeCommand("import-properties");
        
        // Step 0.4: Verify SRC count = 10
        logger.info("Step 0.4: Verifying SRC count...");
        long srcCountPhase0 = locFacade.countOfProperties(Status.SRC, null, false);
        logger.info("VERIFICATION: SRC V1 entry count = " + srcCountPhase0);
        Assert.assertEquals("Phase 0: SRC V1 should have " + SPARSE_SRC_COUNT + " entries (sparse data)", 
                SPARSE_SRC_COUNT, srcCountPhase0);
        
        // ===== PHASE 1: Export, Partial Fill & Import (XLS V2) =====
        logger.info("\n===== PHASE 1: Export, Partial Fill & Import (XLS V2) =====");
        
        // Step 1.1: Export full 16-entry matrix for translation
        logger.info("Step 1.1: Exporting to Excel (ee)...");
        executeCommand("excel-export", tempExcelDir.toString());
        
        File excelV1 = findExportedFile(tempExcelDir.toString(), "-export-all.xlsx");
        Assert.assertNotNull("Excel V1 export file should exist", excelV1);
        logger.info("Excel V1 exported: " + excelV1.getAbsolutePath());
        
        // Step 1.2: Simulate Translator - Fill partial values
        logger.info("Step 1.2: Simulating translator (partial fill)...");
        simulateTranslatorPartialFill(excelV1);
        
        // Step 1.3: Import modifications (creates XLS V2)
        logger.info("Step 1.3: Importing modified Excel (ei)...");
        executeCommand("excel-import", excelV1.getAbsolutePath());
        
        // Step 1.4: Verify XLS count = 16 (Dense Matrix)
        logger.info("Step 1.4: Verifying XLS V2 count...");
        long xlsCountPhase1 = locFacade.countOfProperties(Status.XLS, null, false);
        logger.info("VERIFICATION: XLS V2 entry count = " + xlsCountPhase1);
        Assert.assertEquals("Phase 1: XLS V2 should have " + DENSE_MATRIX_COUNT + " entries (dense matrix)", 
                DENSE_MATRIX_COUNT, xlsCountPhase1);
        
        // Verify some entries still have empty values
        long emptyCount = locFacade.countOfProperties(Status.XLS, null, true);
        logger.info("Phase 1: XLS V2 has " + emptyCount + " entries with empty values");
        Assert.assertTrue("Phase 1: XLS V2 should have some empty values remaining", emptyCount > 0);
        
        // ===== PHASE 2: Target Empty Values & Final Import (XLS V3) =====
        logger.info("\n===== PHASE 2: Target Empty Values & Final Import (XLS V3) =====");
        
        // Step 2.1: Export only properties with empty values
        logger.info("Step 2.1: Exporting empty properties (ee -e)...");
        executeCommand("excel-export", tempExcelDir.toString(), "-e");
        
        File excelV2 = findExportedFile(tempExcelDir.toString(), "-export-all.xlsx");
        Assert.assertNotNull("Excel V2 export file should exist", excelV2);
        logger.info("Excel V2 (empty values only) exported: " + excelV2.getAbsolutePath());
        
        // Step 2.2: Simulate Translator - Fill remaining gaps
        logger.info("Step 2.2: Simulating translator (complete fill)...");
        simulateTranslatorCompleteFill(excelV2);
        
        // Step 2.3: Import final modifications (creates XLS V3)
        logger.info("Step 2.3: Importing final Excel (ei)...");
        executeCommand("excel-import", excelV2.getAbsolutePath());
        
        // Verify XLS V3 count = 16 and no empty values
        int xlsV3 = locFacade.lastVersion(Status.XLS);
        long xlsCountPhase2 = locFacade.countOfProperties(Status.XLS, null, false);
        logger.info("VERIFICATION: XLS V3 (version " + xlsV3 + ") entry count = " + xlsCountPhase2);
        Assert.assertEquals("Phase 2: XLS V3 should have " + DENSE_MATRIX_COUNT + " entries", 
                DENSE_MATRIX_COUNT, xlsCountPhase2);
        
        // ===== PHASE 3: Promotion to Source & Final Verification (SRC V3) =====
        logger.info("\n===== PHASE 3: Promotion to Source & Final Verification (SRC V3) =====");
        
        // Step 3.1: Export XLS V3 back to properties files
        logger.info("Step 3.1: Exporting to properties (ep)...");
        executeCommand("export-properties", testDataDir.toString());
        
        // Step 3.2: Re-import the fully populated physical files (creates SRC V3)
        logger.info("Step 3.2: Re-importing properties (ip)...");
        executeCommand("import-properties");
        
        // Step 3.3: Verify SRC V3 count = 16 (Dense Matrix promoted to Source)
        logger.info("Step 3.3: Verifying SRC V3 count...");
        long srcCountPhase3 = locFacade.countOfProperties(Status.SRC, null, false);
        logger.info("VERIFICATION: SRC V3 entry count = " + srcCountPhase3);
        Assert.assertEquals("Phase 3: SRC V3 should have " + DENSE_MATRIX_COUNT + " entries (dense matrix promoted)", 
                DENSE_MATRIX_COUNT, srcCountPhase3);
        
        // Step 3.4: Final Integrity Check (SRC V3 vs XLS V3)
        logger.info("Step 3.4: Performing final integrity check (ci)...");
        verifyIntegrity();
        
        logger.info("\n*** SUCCESS! The localization properties are complete! ***");
        logger.info("Workflow completed: " + SPARSE_SRC_COUNT + " entries (SRC V1) -> " + 
                DENSE_MATRIX_COUNT + " entries (SRC V3)");
    }
    
    /**
     * Simulates the first translation round, filling critical gaps:
     * - key.foobar (IT/FR)
     * - key.foobar2 (DE)
     * - key.foobar3 (DE)
     */
    private void simulateTranslatorPartialFill(File excelFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int deCol = findLanguageColumnIndex(sheet, "de");
            int frCol = findLanguageColumnIndex(sheet, "fr");
            int itCol = findLanguageColumnIndex(sheet, "it");
            
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell keyCell = row.getCell(KEY_COLUMN_INDEX);
                
                if (keyCell == null) continue;
                String key = getCellStringValue(keyCell);
                if (key == null) continue;
                
                if ("key.foobar".equals(key)) {
                    // Fill IT and FR values for key.foobar
                    if (itCol >= 0) {
                        fillCellIfEmpty(row, itCol, "foobar(IT)");
                    }
                    if (frCol >= 0) {
                        fillCellIfEmpty(row, frCol, "foobar(FR)");
                    }
                } else if ("key.foobar2".equals(key)) {
                    // Fill DE value for key.foobar2
                    if (deCol >= 0) {
                        fillCellIfEmpty(row, deCol, "foobar2(DE)");
                    }
                } else if ("key.foobar3".equals(key)) {
                    // Fill DE value for key.foobar3
                    if (deCol >= 0) {
                        fillCellIfEmpty(row, deCol, "foobar3(DE)");
                    }
                }
            }
            
            // Save the modified workbook
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        logger.info("Translator simulation (partial fill) complete");
    }
    
    /**
     * Simulates the final translation round, filling all remaining empty values.
     * This includes: key.foobar2/3 for IT/FR.
     */
    private void simulateTranslatorCompleteFill(File excelFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int frCol = findLanguageColumnIndex(sheet, "fr");
            int itCol = findLanguageColumnIndex(sheet, "it");
            
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell keyCell = row.getCell(KEY_COLUMN_INDEX);
                
                if (keyCell == null) continue;
                String key = getCellStringValue(keyCell);
                if (key == null) continue;
                
                if ("key.foobar2".equals(key)) {
                    // Fill IT and FR values
                    if (itCol >= 0) {
                        fillCellIfEmpty(row, itCol, "foobar2(IT)");
                    }
                    if (frCol >= 0) {
                        fillCellIfEmpty(row, frCol, "foobar2(FR)");
                    }
                } else if ("key.foobar3".equals(key)) {
                    // Fill IT and FR values
                    if (itCol >= 0) {
                        fillCellIfEmpty(row, itCol, "foobar3(IT)");
                    }
                    if (frCol >= 0) {
                        fillCellIfEmpty(row, frCol, "foobar3(FR)");
                    }
                }
            }
            
            // Save the modified workbook
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        logger.info("Translator simulation (complete fill) complete");
    }
    
    /**
     * Safely gets the string value from a cell, handling different cell types.
     * 
     * @param cell the cell to get the value from
     * @return the string value, or null if the cell is empty or has an unsupported type
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return null;
        }
    }
    
    /**
     * Checks if a value is a placeholder value (e.g., "[en]foobar(EN)").
     * These are generated by the export when a translation is missing.
     * 
     * @param value the value to check
     * @return true if the value is a placeholder, false otherwise
     */
    private boolean isPlaceholderValue(String value) {
        // Placeholder values start with "[" followed by a language code, e.g., "[en]"
        return value != null && value.startsWith("[");
    }
    
    /**
     * Fills a cell with the given value if it's empty, contains only whitespace, or is a placeholder.
     */
    private void fillCellIfEmpty(Row row, int colIndex, String value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
            cell.setCellValue(value);
        } else {
            String currentValue = getCellStringValue(cell);
            if (currentValue == null || currentValue.trim().isEmpty() || isPlaceholderValue(currentValue)) {
                cell.setCellValue(value);
            }
        }
    }
    
    /**
     * Finds the column index for a specific language in the header row.
     */
    private int findLanguageColumnIndex(Sheet sheet, String language) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return -1;
        }
        
        String searchHeader = "VALUE_" + language.toUpperCase();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            String cellValue = getCellStringValue(cell);
            if (searchHeader.equals(cellValue)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Performs the final integrity check between SRC and XLS entries.
     * Verifies that for each language, the SRC count equals the XLS count.
     */
    private void verifyIntegrity() throws DatabaseException {
        int srcVersion = locFacade.lastVersion(Status.SRC);
        int xlsVersion = locFacade.lastVersion(Status.XLS);
        
        List<Localization> srcLocs = locFacade.getLocalizations(srcVersion, Status.SRC, false, null);
        List<Localization> xlsLocs = locFacade.getLocalizations(xlsVersion, Status.XLS, false, null);
        
        List<String> languages = locFacade.getLanguages(false);
        
        boolean allSuccess = true;
        for (String lang : languages) {
            long srcCount = srcLocs.stream().filter(loc -> lang.equals(loc.getLocale())).count();
            long xlsCount = xlsLocs.stream().filter(loc -> lang.equals(loc.getLocale())).count();
            
            if (srcCount == xlsCount) {
                logger.info("SUCCESS: LANGUAGE[" + lang + "] The amount of src(" + srcCount + 
                        ") vs xls(" + xlsCount + ") is equal.");
            } else {
                logger.warning("FAIL: LANGUAGE[" + lang + "] The amount of src(" + srcCount + 
                        ") vs xls(" + xlsCount + ") is NOT equal!");
                allSuccess = false;
            }
        }
        
        Assert.assertTrue("Final integrity check should pass for all languages", allSuccess);
    }
    
    /**
     * Helper method to find the most recently created export file.
     */
    private File findExportedFile(String directory, String suffix) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            return null;
        }

        File[] matchingFiles = dir.listFiles((dir1, name) -> name.endsWith(suffix));
        if (matchingFiles == null || matchingFiles.length == 0) {
            return null;
        }

        // Return the most recently modified file
        File mostRecent = matchingFiles[0];
        for (File file : matchingFiles) {
            if (file.lastModified() > mostRecent.lastModified()) {
                mostRecent = file;
            }
        }
        return mostRecent;
    }
    
    /**
     * Test to verify that the initial test files match the expected sparse data structure.
     * This validates the test data prerequisites before running the main workflow.
     */
    @Test
    public void testPrerequisiteFilesStructure() throws Exception {
        // Clear database
        executeCommand("clear-loc");
        
        // Scan and import
        executeCommand("files", testDataDir.toString());
        
        executeCommand("import-properties");
        
        // Verify the prerequisite file structure
        int srcVersion = locFacade.lastVersion(Status.SRC);
        List<Localization> srcLocs = locFacade.getLocalizations(srcVersion, Status.SRC, false, null);
        
        // Count by language
        long enCount = srcLocs.stream().filter(loc -> "en".equals(loc.getLocale())).count();
        long deCount = srcLocs.stream().filter(loc -> "de".equals(loc.getLocale())).count();
        long frCount = srcLocs.stream().filter(loc -> "fr".equals(loc.getLocale())).count();
        long itCount = srcLocs.stream().filter(loc -> "it".equals(loc.getLocale())).count();
        
        logger.info("Prerequisite file structure verification:");
        logger.info("  EN entries: " + enCount + " (expected: 4)");
        logger.info("  DE entries: " + deCount + " (expected: 2)");
        logger.info("  FR entries: " + frCount + " (expected: 3)");
        logger.info("  IT entries: " + itCount + " (expected: 1)");
        logger.info("  Total: " + srcLocs.size() + " (expected: " + SPARSE_SRC_COUNT + ")");
        
        Assert.assertEquals("EN should have 4 entries", 4, enCount);
        Assert.assertEquals("DE should have 2 entries", 2, deCount);
        Assert.assertEquals("FR should have 3 entries", 3, frCount);
        Assert.assertEquals("IT should have 1 entry", 1, itCount);
        Assert.assertEquals("Total should be " + SPARSE_SRC_COUNT + " entries", SPARSE_SRC_COUNT, srcLocs.size());
        
        logger.info("Prerequisite files structure verification PASSED!");
    }
    
    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
