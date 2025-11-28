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

    private static final String TEMP_DIR = FileUtils.getTempDirectoryPath();
    private static final String MODIFIED_VALUE = "MODIFIED_VALUE_DURCH_TEST_ÄÖÜ_XYZ";
    
    private LocalizationDao locFacade;
    private Path testDataDir;
    private File exportedFile;

    @Before
    public void init() throws Exception {
        locFacade = new LocalizationDao();
        
        // Generate realistic test data
        Path tempPath = Files.createTempDirectory("locapp_roundtrip_test");
        testDataDir = LocalizationTestGenerator.generateRealisticPropertiesFiles(tempPath);
        
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
        
        // Clean up test data directory
        if (testDataDir != null) {
            try {
                FileUtils.deleteDirectory(testDataDir.getParent().toFile());
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
        // Step 1: Export to XLSX
        logger.info("Step 1: Exporting to XLSX...");
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(TEMP_DIR);
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        exportedFile = findExportedFile(TEMP_DIR, "-export-all.xlsx");
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
        
        // Verify the value contains German Umlaute (encoding stability check)
        Assert.assertTrue("Modified value should contain German Umlaute (ä)", expectedValue.contains("Ä"));
        Assert.assertTrue("Modified value should contain German Umlaute (ö)", expectedValue.contains("Ö"));
        Assert.assertTrue("Modified value should contain German Umlaute (ü)", expectedValue.contains("Ü"));
        
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
     * Helper method to find the most recently created export file.
     */
    private File findExportedFile(String directory, String suffix) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            return null;
        }

        File[] matchingFiles = dir.listFiles((d, name) -> name.endsWith(suffix));
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
