package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import de.aspera.locapp.testutil.LocalizationTestGenerator;

/**
 * Full end-to-end integration test for the localization roundtrip process.
 * This test validates:
 * 1. Data generation and import
 * 2. Export to XLSX format
 * 3. Programmatic editing of Excel file (simulating translator work)
 * 4. Re-import of modified Excel file
 * 5. Verification that modifications are correctly persisted
 * 6. Verification that non-modified entries remain unchanged
 */
public class FullRoundtripIntegrationTest extends BasicFacadeTest {

    private static final String MODIFIED_VALUE = "MODIFIED_VALUE_DURCH_TEST_ÄÖÜ_XYZ";
    
    private LocalizationDao locFacade;
    private Path testDataDir;
    private Path tempBasePath;
    private File exportedFile;

    @Before
    public void init() throws Exception {
        locFacade = new LocalizationDao();
        
        // Generate realistic test data in a unique temporary directory
        tempBasePath = Files.createTempDirectory("locapp_roundtrip_test");
        testDataDir = LocalizationTestGenerator.generateRealisticPropertiesFiles(tempBasePath);
        
        // Initialize configuration
        CMDCTX.addArgument("init");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Load generated test files
        CMDCTX.addArgument("files");
        CMDCTX.addArgument(testDataDir.toString());
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Clear database
        CMDCTX.addArgument("cl");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Import properties
        CMDCTX.addArgument("ip");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        logger.info("Test setup complete - test data directory: " + testDataDir);
    }

    @After
    public void cleanup() {
        // Clean up exported file
        if (exportedFile != null && exportedFile.exists()) {
            exportedFile.delete();
        }
        
        // Clean up the specific temporary base directory created for this test
        if (tempBasePath != null) {
            try {
                FileUtils.deleteDirectory(tempBasePath.toFile());
            } catch (IOException e) {
                logger.warning("Failed to cleanup test directory: " + e.getMessage());
            }
        }
    }

    /**
     * Full E2E test covering:
     * - Export to XLSX
     * - Programmatic edit (translator simulation)
     * - Re-import
     * - Verification of German value modification
     * - Verification that English and French values remain unchanged
     */
    @Test
    public void testFullRoundtripWithModification() throws Exception {
        // Step 1: Export to XLSX (use the unique temp base directory)
        String exportDir = tempBasePath.toString();
        logger.info("Step 1: Exporting to XLSX...");
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(exportDir);
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        exportedFile = findExportedFile(exportDir, "-export-all.xlsx");
        Assert.assertNotNull("Exported .xlsx file should exist", exportedFile);
        logger.info("Export complete: " + exportedFile.getAbsolutePath());
        
        // Verify export contains our fixed key
        verifyExportContainsFixedKey(exportedFile);
        
        // Step 2: Programmatic Edit (Translator Simulation)
        logger.info("Step 2: Modifying German value in Excel...");
        modifyGermanValueInExcel(exportedFile);
        
        // Step 3: Re-Import
        logger.info("Step 3: Re-importing modified Excel...");
        CMDCTX.addArgument("ei");
        CMDCTX.addArgument(exportedFile.getAbsolutePath());
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Step 4: Verification
        logger.info("Step 4: Verifying modifications...");
        verifyGermanLocalization(locFacade, MODIFIED_VALUE);
        verifyOtherLanguagesUnchanged(locFacade);
        
        logger.info("Full roundtrip test PASSED!");
    }

    /**
     * Verifies that the exported Excel file contains the fixed key.
     */
    private void verifyExportContainsFixedKey(File xlsxFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(xlsxFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            boolean foundKey = false;
            
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell keyCell = row.getCell(1); // Key is in column 1
                if (keyCell != null && LocalizationTestGenerator.FIXED_KEY.equals(keyCell.getStringCellValue())) {
                    foundKey = true;
                    break;
                }
            }
            
            Assert.assertTrue("Exported file should contain the fixed key: " + LocalizationTestGenerator.FIXED_KEY, foundKey);
        }
    }

    /**
     * Modifies the German value for the fixed key in the Excel file.
     * Simulates translator editing the Excel file.
     */
    private void modifyGermanValueInExcel(File xlsxFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(xlsxFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int deColumnIndex = findLanguageColumnIndex(sheet, "de");
            Assert.assertTrue("German (de) column should exist in exported Excel", deColumnIndex >= 0);
            
            // Find the row with the fixed key and modify the German value
            boolean modified = false;
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell keyCell = row.getCell(1); // Key is in column 1
                if (keyCell != null && LocalizationTestGenerator.FIXED_KEY.equals(keyCell.getStringCellValue())) {
                    Cell deValueCell = row.getCell(deColumnIndex);
                    if (deValueCell != null) {
                        logger.info("Original German value: " + deValueCell.getStringCellValue());
                        deValueCell.setCellValue(MODIFIED_VALUE);
                        modified = true;
                        logger.info("Modified German value to: " + MODIFIED_VALUE);
                        break;
                    }
                }
            }
            
            Assert.assertTrue("Should have modified the German value for key: " + LocalizationTestGenerator.FIXED_KEY, modified);
            
            // Save the modified workbook
            try (FileOutputStream fos = new FileOutputStream(xlsxFile)) {
                workbook.write(fos);
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
            if (cell != null && searchHeader.equals(cell.getStringCellValue())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Verifies that the German localization for the fixed key matches the expected value.
     */
    private void verifyGermanLocalization(LocalizationDao dao, String expectedValue) throws DatabaseException {
        int lastVersion = dao.lastVersion(Status.XLS);
        List<Localization> germanLocs = dao.getLocalizations(lastVersion, Status.XLS, false, null);
        
        Localization germanEntry = null;
        for (Localization loc : germanLocs) {
            if (LocalizationTestGenerator.FIXED_KEY.equals(loc.getKey()) && "de".equals(loc.getLocale())) {
                germanEntry = loc;
                break;
            }
        }
        
        Assert.assertNotNull("German localization for key '" + LocalizationTestGenerator.FIXED_KEY + "' should exist", germanEntry);
        Assert.assertEquals("German value should match the modified test string", expectedValue, germanEntry.getValue());
        
        // Verify the value contains German uppercase Umlaute (encoding stability check)
        Assert.assertTrue("Modified value should contain uppercase German Umlaut (Ä)", expectedValue.contains("Ä"));
        Assert.assertTrue("Modified value should contain uppercase German Umlaut (Ö)", expectedValue.contains("Ö"));
        Assert.assertTrue("Modified value should contain uppercase German Umlaut (Ü)", expectedValue.contains("Ü"));
        
        logger.info("German localization verified successfully: " + germanEntry.getValue());
    }

    /**
     * Verifies that English and French values for the fixed key were not accidentally modified.
     */
    private void verifyOtherLanguagesUnchanged(LocalizationDao dao) throws DatabaseException {
        int lastVersion = dao.lastVersion(Status.XLS);
        List<Localization> allLocs = dao.getLocalizations(lastVersion, Status.XLS, false, null);
        
        Localization englishEntry = null;
        Localization frenchEntry = null;
        
        for (Localization loc : allLocs) {
            if (LocalizationTestGenerator.FIXED_KEY.equals(loc.getKey())) {
                if ("en".equals(loc.getLocale())) {
                    englishEntry = loc;
                } else if ("fr".equals(loc.getLocale())) {
                    frenchEntry = loc;
                }
            }
        }
        
        // Verify English value is unchanged
        Assert.assertNotNull("English localization for key '" + LocalizationTestGenerator.FIXED_KEY + "' should exist", englishEntry);
        Assert.assertEquals("English value should remain unchanged", 
                LocalizationTestGenerator.ORIGINAL_EN_VALUE, englishEntry.getValue());
        logger.info("English value verified unchanged: " + englishEntry.getValue());
        
        // Verify French value is unchanged
        Assert.assertNotNull("French localization for key '" + LocalizationTestGenerator.FIXED_KEY + "' should exist", frenchEntry);
        Assert.assertEquals("French value should remain unchanged", 
                LocalizationTestGenerator.ORIGINAL_FR_VALUE, frenchEntry.getValue());
        logger.info("French value verified unchanged: " + frenchEntry.getValue());
    }

    /**
     * Test that verifies SRC COUNT == XLS COUNT after a full roundtrip.
     * This specifically tests the fix for the Excel import issue where
     * entries with empty or unchanged values were incorrectly filtered out.
     * 
     * After export and re-import, the number of XLS entries for each language
     * should match the number of SRC entries for English (the base language).
     */
    @Test
    public void testRoundtripPreservesAllEntries() throws Exception {
        // Get the SRC count for English (base language)
        int srcVersion = locFacade.lastVersion(Status.SRC);
        List<Localization> srcLocalizations = locFacade.getLocalizations(srcVersion, Status.SRC, false, null);
        
        // Count English entries as the reference
        long englishSrcCount = srcLocalizations.stream()
                .filter(loc -> "en".equals(loc.getLocale()))
                .count();
        
        logger.info("SRC English count: " + englishSrcCount);
        
        // Step 1: Export to XLSX
        String exportDir = tempBasePath.toString();
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(exportDir);
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        File xlsxFile = findExportedFile(exportDir, "-export-all.xlsx");
        Assert.assertNotNull("Exported .xlsx file should exist", xlsxFile);
        
        // Step 2: Re-Import the Excel (without any modifications)
        CMDCTX.addArgument("ei");
        CMDCTX.addArgument(xlsxFile.getAbsolutePath());
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Step 3: Verify XLS counts match SRC counts for all languages
        int xlsVersion = locFacade.lastVersion(Status.XLS);
        List<Localization> xlsLocalizations = locFacade.getLocalizations(xlsVersion, Status.XLS, false, null);
        
        // Count entries per language
        long englishXlsCount = xlsLocalizations.stream()
                .filter(loc -> "en".equals(loc.getLocale()))
                .count();
        long germanXlsCount = xlsLocalizations.stream()
                .filter(loc -> "de".equals(loc.getLocale()))
                .count();
        long frenchXlsCount = xlsLocalizations.stream()
                .filter(loc -> "fr".equals(loc.getLocale()))
                .count();
        
        logger.info("XLS English count: " + englishXlsCount);
        logger.info("XLS German count: " + germanXlsCount);
        logger.info("XLS French count: " + frenchXlsCount);
        
        // Verify: XLS count for each language should equal SRC English count
        Assert.assertEquals("English XLS count should equal SRC count", englishSrcCount, englishXlsCount);
        Assert.assertEquals("German XLS count should equal SRC count", englishSrcCount, germanXlsCount);
        Assert.assertEquals("French XLS count should equal SRC count", englishSrcCount, frenchXlsCount);
        
        logger.info("Roundtrip integrity test PASSED - all counts match!");
    }

    /**
     * Test A: MERGE/Inheritance Verification
     * 
     * Verifies that a partial import does not cause data loss.
     * This tests the MERGE strategy where:
     * 1. Initial XLS v1 contains a full set of keys
     * 2. Partial Excel is imported (with only some keys)
     * 3. New XLS v2 must contain ALL keys (inherited from v1 + updated from partial Excel)
     */
    @Test
    public void testMergeInheritancePreservesData() throws Exception {
        // Step 1: Export full SRC data to XLSX
        String exportDir = tempBasePath.toString();
        logger.info("Step 1: Exporting full data to XLSX...");
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(exportDir);
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        File fullExcelFile = findExportedFile(exportDir, "-export-all.xlsx");
        Assert.assertNotNull("Exported .xlsx file should exist", fullExcelFile);
        
        // Get the total count of SRC English entries as reference
        int srcVersion = locFacade.lastVersion(Status.SRC);
        List<Localization> srcLocalizations = locFacade.getLocalizations(srcVersion, Status.SRC, false, null);
        long totalEnglishCount = srcLocalizations.stream()
                .filter(loc -> "en".equals(loc.getLocale()))
                .count();
        logger.info("Total SRC English count: " + totalEnglishCount);
        
        // Step 2: Import full Excel to create XLS v1
        logger.info("Step 2: Importing full Excel to create XLS v1...");
        CMDCTX.addArgument("ei");
        CMDCTX.addArgument(fullExcelFile.getAbsolutePath());
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        int xlsV1 = locFacade.lastVersion(Status.XLS);
        List<Localization> xlsV1Locs = locFacade.getLocalizations(xlsV1, Status.XLS, false, null);
        long xlsV1EnglishCount = xlsV1Locs.stream()
                .filter(loc -> "en".equals(loc.getLocale()))
                .count();
        logger.info("XLS v1 English count: " + xlsV1EnglishCount);
        
        // Step 3: Create a partial Excel containing only a SUBSET of keys (simulate partial translator work)
        File partialExcelFile = createPartialExcel(fullExcelFile, 10); // Keep only first 10 data rows
        logger.info("Step 3: Created partial Excel with subset of keys...");
        
        // Step 4: Import partial Excel - this should MERGE with XLS v1
        logger.info("Step 4: Importing partial Excel (should MERGE with XLS v1)...");
        CMDCTX.addArgument("ei");
        CMDCTX.addArgument(partialExcelFile.getAbsolutePath());
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Step 5: Verify XLS v2 contains the FULL set of keys (inherited + updated from partial Excel)
        int xlsV2 = locFacade.lastVersion(Status.XLS);
        Assert.assertTrue("New XLS version should be created", xlsV2 > xlsV1);
        
        List<Localization> xlsV2Locs = locFacade.getLocalizations(xlsV2, Status.XLS, false, null);
        long xlsV2EnglishCount = xlsV2Locs.stream()
                .filter(loc -> "en".equals(loc.getLocale()))
                .count();
        logger.info("XLS v2 English count: " + xlsV2EnglishCount);
        
        // The MERGE strategy should preserve all entries from v1 + update/add from partial Excel
        Assert.assertEquals("XLS v2 should have same count as XLS v1 (MERGE inheritance)", 
                xlsV1EnglishCount, xlsV2EnglishCount);
        
        logger.info("MERGE inheritance test PASSED - partial import preserved all data!");
    }

    /**
     * Creates a partial Excel file containing only a subset of rows from the original.
     * Used to simulate partial translator work.
     */
    private File createPartialExcel(File originalFile, int maxDataRows) throws IOException {
        File partialFile = new File(originalFile.getParent(), "partial_" + originalFile.getName());
        
        try (FileInputStream fis = new FileInputStream(originalFile);
             Workbook originalWorkbook = WorkbookFactory.create(fis)) {
            
            Sheet originalSheet = originalWorkbook.getSheetAt(0);
            Workbook partialWorkbook = WorkbookFactory.create(true); // Create new XLSX
            Sheet partialSheet = partialWorkbook.createSheet("SLC Properties");
            
            int rowCount = 0;
            int dataRowCount = 0;
            
            Iterator<Row> rowIterator = originalSheet.iterator();
            while (rowIterator.hasNext() && (dataRowCount < maxDataRows || rowCount == 0)) {
                Row originalRow = rowIterator.next();
                Row partialRow = partialSheet.createRow(rowCount);
                
                // Copy all cells from original row
                for (int i = 0; i < originalRow.getLastCellNum(); i++) {
                    Cell originalCell = originalRow.getCell(i);
                    if (originalCell != null) {
                        Cell partialCell = partialRow.createCell(i);
                        switch (originalCell.getCellType()) {
                            case STRING:
                                partialCell.setCellValue(originalCell.getStringCellValue());
                                break;
                            case NUMERIC:
                                partialCell.setCellValue(originalCell.getNumericCellValue());
                                break;
                            case BOOLEAN:
                                partialCell.setCellValue(originalCell.getBooleanCellValue());
                                break;
                            default:
                                partialCell.setCellValue("");
                        }
                    }
                }
                
                // Header row doesn't count as data
                if (rowCount > 0) {
                    dataRowCount++;
                }
                rowCount++;
            }
            
            // Save partial workbook
            try (FileOutputStream fos = new FileOutputStream(partialFile)) {
                partialWorkbook.write(fos);
            }
            partialWorkbook.close();
        }
        
        return partialFile;
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

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
