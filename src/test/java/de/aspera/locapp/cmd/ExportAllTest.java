package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;

import org.apache.poi.ss.usermodel.*;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import static org.junit.Assert.*;

/**
 * Test to verify that ExcelExportCommand correctly exports EN values
 * when other language localizations are missing or have different keys.
 */
public class ExportAllTest extends BasicFacadeTest {

    private static final String TEMP_DIR = FileUtils.getTempDirectoryPath();
    
    /** Filter for export-all xlsx files */
    private static final FilenameFilter EXPORT_ALL_FILTER = 
        (dir, name) -> name.contains("export-all") && name.endsWith(".xlsx");

    @Before
    public void init() throws Exception {
        // Clean up any old export-all files from previous test runs
        File[] oldFiles = new File(TEMP_DIR).listFiles(EXPORT_ALL_FILTER);
        if (oldFiles != null) {
            for (File f : oldFiles) {
                f.delete();
            }
        }
        
        String testfiles = ExportAllTest.class.getClassLoader().getResource("testfiles").getFile();
        executeCommand("init");
        executeCommand("files", testfiles);
        executeCommand("clear-loc");
        executeCommand("import-properties");
    }

    @Test
    public void exportAllAndCheckEnValuesAreCorrect() throws Exception {
        executeCommand("excel-export", TEMP_DIR);
        
        // Find exported file
        File[] files = new File(TEMP_DIR).listFiles(EXPORT_ALL_FILTER);
        assertNotNull("Export file should exist", files);
        assertEquals("Should have exactly one export file", 1, files.length);
        
        // Read the Excel file and verify EN values
        try (FileInputStream fis = new FileInputStream(files[0]);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            
            // Find the VALUE_EN column index
            Row headerRow = sheet.getRow(0);
            int enColumnIndex = -1;
            int keyColumnIndex = -1;
            for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                Cell cell = headerRow.getCell(j);
                if (cell != null) {
                    String value = cell.getStringCellValue();
                    if ("VALUE_EN".equals(value)) {
                        enColumnIndex = j;
                    } else if ("Key".equals(value)) {
                        keyColumnIndex = j;
                    }
                }
            }
            
            assertTrue("VALUE_EN column should exist", enColumnIndex >= 0);
            assertTrue("Key column should exist", keyColumnIndex >= 0);
            
            // Check each row for EN values
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell keyCell = row.getCell(keyColumnIndex);
                    Cell enCell = row.getCell(enColumnIndex);
                    
                    if (keyCell != null) {
                        String key = keyCell.getStringCellValue();
                        String enValue = enCell != null ? enCell.getStringCellValue() : "";
                        
                        // Verify EN values are correctly exported
                        // test.properties has: key.foobar, key.foobar1, key.foobar2, key.foobar3 all with EN values
                        if ("key.foobar".equals(key)) {
                            assertEquals("EN value for key.foobar should be foobar(EN)", "foobar(EN)", enValue);
                        } else if ("key.foobar1".equals(key)) {
                            assertEquals("EN value for key.foobar1 should be foobar1(EN)", "foobar1(EN)", enValue);
                        } else if ("key.foobar2".equals(key)) {
                            assertEquals("EN value for key.foobar2 should be foobar2(EN)", "foobar2(EN)", enValue);
                        } else if ("key.foobar3".equals(key)) {
                            assertEquals("EN value for key.foobar3 should be foobar3(EN)", "foobar3(EN)", enValue);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
