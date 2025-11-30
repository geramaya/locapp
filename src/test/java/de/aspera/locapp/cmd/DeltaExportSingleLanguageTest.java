package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization.Status;

/**
 * Test that verifies delta export only modifies the specific language file
 * that was changed in the Excel, not all language variants.
 * 
 * This addresses the issue where modifying only VALUE_EN for a key was causing
 * all language files (test.properties, test_de.properties, test_fr.properties, 
 * test_it.properties) to be modified.
 */
public class DeltaExportSingleLanguageTest extends BasicFacadeTest {

    private LocalizationDao locFacade;
    private Path tempBasePath;
    private Path testDataDir;

    @Before
    public void init() throws Exception {
        locFacade = new LocalizationDao();
        tempBasePath = Files.createTempDirectory("locapp_delta_single_lang_test");
        testDataDir = tempBasePath.resolve("testdata");
        Files.createDirectories(testDataDir);
        
        // Create test files matching the user's scenario
        Properties enProps = new Properties();
        enProps.setProperty("key.foobar", "foobar(EN)");
        enProps.setProperty("key.foobar1", "foobar1(EN)");
        enProps.setProperty("key.foobar2", "");
        enProps.setProperty("key.foobar3", "");
        
        Properties deProps = new Properties();
        deProps.setProperty("key.foobar", "foobar(DE)");
        deProps.setProperty("key.foobar1", "foobar1(DE)");
        
        Properties frProps = new Properties();
        frProps.setProperty("key.foobar1", "foobar1(FR)");
        frProps.setProperty("key.foobar2", "");
        frProps.setProperty("key.foobar3", "");
        
        Properties itProps = new Properties();
        itProps.setProperty("key.foobar1", "foobar1(IT)");
        
        writeProps(testDataDir.resolve("test.properties"), enProps);
        writeProps(testDataDir.resolve("test_de.properties"), deProps);
        writeProps(testDataDir.resolve("test_fr.properties"), frProps);
        writeProps(testDataDir.resolve("test_it.properties"), itProps);
        
        executeCommand("init");
        executeCommand("files", testDataDir.toString());
        executeCommand("clear-loc");
        executeCommand("import-properties");
        
        logger.info("Test setup complete");
    }
    
    private void writeProps(Path path, Properties props) throws IOException {
        try (var os = Files.newOutputStream(path)) {
            props.store(os, null);
        }
    }

    @After
    public void cleanup() {
        if (tempBasePath != null) {
            try {
                FileUtils.deleteDirectory(tempBasePath.toFile());
            } catch (IOException e) {
                logger.warning("Failed to cleanup: " + e.getMessage());
            }
        }
    }

    /**
     * Test that when only the English value is modified in Excel,
     * only test.properties is modified during delta export.
     */
    @Test
    public void testDeltaExportOnlyModifiesChangedLanguageFile() throws Exception {
        // Step 1: Export to Excel
        executeCommand("excel-export", tempBasePath.toString());
        File excelFile = findExcel(tempBasePath.toString());
        Assert.assertNotNull("Excel file should be created", excelFile);
        
        // Step 2: Modify the Excel - change ONLY key.foobar2 VALUE_EN
        modifyExcel(excelFile, "key.foobar2", "VALUE_EN", "foobar2(EN)-edited");
        
        // Step 3: Import modified Excel
        executeCommand("excel-import", excelFile.getAbsolutePath());
        
        // Step 4: Verify only 1 difference was detected
        int srcVersion = locFacade.lastVersion(Status.SRC);
        int xlsVersion = locFacade.lastVersion(Status.XLS);
        var diffs = locFacade.getXlsToSrcDifferences(srcVersion, xlsVersion);
        Assert.assertEquals("Should find exactly 1 difference", 1, diffs.size());
        Assert.assertEquals("Difference should be for English locale", "en", diffs.get(0).getLocale());
        Assert.assertEquals("Difference should be for test.properties", "test.properties", diffs.get(0).getFileName());
        
        // Step 5: Record file timestamps BEFORE delta export
        Map<String, Long> beforeTimestamps = new HashMap<>();
        for (File f : testDataDir.toFile().listFiles()) {
            if (f.getName().endsWith(".properties")) {
                beforeTimestamps.put(f.getName(), f.lastModified());
            }
        }
        
        // Sleep to ensure timestamp difference is detectable
        Thread.sleep(1100);
        
        // Step 6: Run delta export
        executeCommand("export-properties", "-d", testDataDir.toString());
        
        // Step 7: Check which files were modified
        List<String> modifiedFiles = new ArrayList<>();
        for (File f : testDataDir.toFile().listFiles()) {
            if (f.getName().endsWith(".properties")) {
                Long before = beforeTimestamps.get(f.getName());
                Long after = f.lastModified();
                if (after > before) {
                    modifiedFiles.add(f.getName());
                }
            }
        }
        
        // Only test.properties should be modified
        Assert.assertEquals("Only 1 file should be modified", 1, modifiedFiles.size());
        Assert.assertTrue("test.properties should be modified", modifiedFiles.contains("test.properties"));
        Assert.assertFalse("test_de.properties should NOT be modified", modifiedFiles.contains("test_de.properties"));
        Assert.assertFalse("test_fr.properties should NOT be modified", modifiedFiles.contains("test_fr.properties"));
        Assert.assertFalse("test_it.properties should NOT be modified", modifiedFiles.contains("test_it.properties"));
        
        logger.info("SUCCESS: Only test.properties was modified as expected!");
    }
    
    private void modifyExcel(File excelFile, String targetKey, String column, String newValue) throws Exception {
        FileInputStream fis = new FileInputStream(excelFile);
        Workbook wb = WorkbookFactory.create(fis);
        Sheet sheet = wb.getSheetAt(0);
        
        Row headerRow = sheet.getRow(0);
        int colIdx = -1;
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && column.equalsIgnoreCase(cell.getStringCellValue())) {
                colIdx = i;
                break;
            }
        }
        
        Assert.assertTrue("Column " + column + " should exist", colIdx >= 0);
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell keyCell = row.getCell(1);
                if (keyCell != null && targetKey.equals(keyCell.getStringCellValue())) {
                    row.getCell(colIdx).setCellValue(newValue);
                    break;
                }
            }
        }
        
        fis.close();
        FileOutputStream fos = new FileOutputStream(excelFile);
        wb.write(fos);
        fos.close();
        wb.close();
    }
    
    private File findExcel(String dir) {
        File d = new File(dir);
        File[] files = d.listFiles((f, name) -> name.endsWith(".xlsx"));
        return files != null && files.length > 0 ? files[0] : null;
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
