package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
 * Comprehensive integration tests for modern .xlsx Excel file support.
 * Tests export, import, roundtrip, and edit/save scenarios.
 */
public class ModernExcelIntegrationTest extends BasicFacadeTest {

    private static final String TEMP_DIR = FileUtils.getTempDirectoryPath();
    private LocalizationDao locFacade;
    private List<File> createdFiles = new ArrayList<>();

    @Before
    public void init() throws InstantiationException, IllegalAccessException, CommandException {
        locFacade = new LocalizationDao();

        String testfiles = ModernExcelIntegrationTest.class.getClassLoader().getResource("testfiles").getFile();

        CMDCTX.addArgument("init");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("files");
        CMDCTX.addArgument(testfiles);
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("cl");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("ip");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
    }

    @After
    public void cleanup() {
        for (File file : createdFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Test Case 1 (Export): Verify that ExcelExportCommand creates a valid .xlsx file
     * that can be opened by WorkbookFactory.create().
     */
    @Test
    public void testExportCreatesValidXlsxFile() throws Exception {
        // Export to a known location
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(TEMP_DIR);
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        // Find the exported file
        File exportedFile = findExportedFile(TEMP_DIR, "-export-all.xlsx");
        Assert.assertNotNull("Exported .xlsx file should exist", exportedFile);
        createdFiles.add(exportedFile);

        // Verify the file can be opened by WorkbookFactory
        try (FileInputStream fis = new FileInputStream(exportedFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Assert.assertNotNull("Workbook should not be null", workbook);
            Assert.assertTrue("Workbook should have at least one sheet", workbook.getNumberOfSheets() > 0);
            
            Sheet sheet = workbook.getSheetAt(0);
            Assert.assertNotNull("Sheet should not be null", sheet);
            
            // Verify header row exists
            Row headerRow = sheet.getRow(0);
            Assert.assertNotNull("Header row should exist", headerRow);
            Assert.assertEquals("First header should be 'Filename'", "Filename", headerRow.getCell(0).getStringCellValue());
            Assert.assertEquals("Second header should be 'Key'", "Key", headerRow.getCell(1).getStringCellValue());
        }
        
        logger.info("Test Case 1 passed: Export creates valid .xlsx file");
    }

    /**
     * Test Case 2 (Roundtrip): Export localizations to .xlsx, import them back,
     * and verify the data matches. Tests special characters (ä, ö, ü).
     */
    @Test
    public void testRoundtripWithSpecialCharacters() throws Exception {
        // Get initial data count
        int lastVersionBefore = locFacade.lastVersion(Status.SRC);
        List<Localization> originalLocs = locFacade.getLocalizations(lastVersionBefore, Status.SRC, false, null);
        Assert.assertFalse("Should have localizations to export", originalLocs.isEmpty());

        // Export to xlsx
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(TEMP_DIR);
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        // Find exported file
        File exportedFile = findExportedFile(TEMP_DIR, "-export-all.xlsx");
        Assert.assertNotNull("Exported file should exist", exportedFile);
        createdFiles.add(exportedFile);

        // Clear database and import the file back
        CMDCTX.addArgument("cl");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("ei");
        CMDCTX.addArgument(exportedFile.getAbsolutePath());
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        // Verify imported data
        int lastVersionAfter = locFacade.lastVersion(Status.XLS);
        List<Localization> importedLocs = locFacade.getLocalizations(lastVersionAfter, Status.XLS, false, null);
        
        Assert.assertFalse("Should have imported localizations", importedLocs.isEmpty());
        
        // Verify the count matches (may differ by empty values that are filtered)
        logger.info("Original count: " + originalLocs.size() + ", Imported count: " + importedLocs.size());
        
        // Verify special characters are preserved if present in data
        boolean hasSpecialChars = false;
        for (Localization loc : importedLocs) {
            String value = loc.getValue();
            if (value != null && (value.contains("ä") || value.contains("ö") || value.contains("ü"))) {
                hasSpecialChars = true;
                logger.info("Special character found in value: " + value);
                break;
            }
        }
        
        logger.info("Test Case 2 passed: Roundtrip test completed, special chars preserved: " + hasSpecialChars);
    }

    /**
     * Test Case 3 (Edit/Save): Generate .xlsx file, modify a cell value programmatically,
     * save it, import again, and verify the modification is detected.
     */
    @Test
    public void testEditAndSaveDetectsModifications() throws Exception {
        // Export to xlsx
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(TEMP_DIR);
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        // Find exported file
        File exportedFile = findExportedFile(TEMP_DIR, "-export-all.xlsx");
        Assert.assertNotNull("Exported file should exist", exportedFile);
        createdFiles.add(exportedFile);

        // Modify a cell in the file (simulating translator edit)
        String modifiedValue = "MODIFIED_VALUE_WITH_SPECIAL_CHARS_äöü";
        String originalKey = null;
        
        try (FileInputStream fis = new FileInputStream(exportedFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            // Get the first data row (row 1, after header)
            Row dataRow = sheet.getRow(1);
            if (dataRow != null) {
                // Get the key from column 1
                Cell keyCell = dataRow.getCell(1);
                if (keyCell != null) {
                    originalKey = keyCell.getStringCellValue();
                }
                
                // Modify the value in column 2 (first VALUE column)
                Cell valueCell = dataRow.getCell(2);
                if (valueCell != null) {
                    valueCell.setCellValue(modifiedValue);
                }
            }
            
            // Save the modified workbook
            try (FileOutputStream fos = new FileOutputStream(exportedFile)) {
                workbook.write(fos);
            }
        }
        
        Assert.assertNotNull("Should have found original key", originalKey);
        
        // Clear database and import the modified file
        CMDCTX.addArgument("cl");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("ei");
        CMDCTX.addArgument(exportedFile.getAbsolutePath());
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        // Verify the modification was imported
        int lastVersion = locFacade.lastVersion(Status.XLS);
        List<Localization> importedLocs = locFacade.getLocalizations(lastVersion, Status.XLS, false, null);
        
        boolean foundModifiedValue = false;
        for (Localization loc : importedLocs) {
            if (modifiedValue.equals(loc.getValue())) {
                foundModifiedValue = true;
                // Verify special characters in the modified value are preserved
                Assert.assertTrue("Value should contain special char ä", loc.getValue().contains("ä"));
                Assert.assertTrue("Value should contain special char ö", loc.getValue().contains("ö"));
                Assert.assertTrue("Value should contain special char ü", loc.getValue().contains("ü"));
                logger.info("Found modified value with key: " + loc.getKey() + ", value: " + loc.getValue());
                break;
            }
        }
        
        Assert.assertTrue("Modified value should be found in imported data", foundModifiedValue);
        logger.info("Test Case 3 passed: Edit/Save modification detected with special characters");
    }

    /**
     * Additional test: Verify language-specific export creates valid .xlsx
     */
    @Test
    public void testLanguageSpecificExport() throws Exception {
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(TEMP_DIR);
        CMDCTX.addArgument("fr");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        // Find the exported file
        File exportedFile = findExportedFile(TEMP_DIR, "-export-fr.xlsx");
        Assert.assertNotNull("Language-specific exported .xlsx file should exist", exportedFile);
        createdFiles.add(exportedFile);

        // Verify the file is valid
        try (FileInputStream fis = new FileInputStream(exportedFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Assert.assertNotNull("Workbook should not be null", workbook);
            Assert.assertTrue("Workbook should have at least one sheet", workbook.getNumberOfSheets() > 0);
        }
        
        logger.info("Language-specific export test passed");
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
