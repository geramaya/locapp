package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import static org.junit.Assert.*;

public class ExportAllTest extends BasicFacadeTest {

    @Before
    public void init() throws Exception {
        String testfiles = ExportAllTest.class.getClassLoader().getResource("testfiles").getFile();
        executeCommand("init");
        executeCommand("files", testfiles);
        executeCommand("clear-loc");
        executeCommand("import-properties");
    }

    @Test
    public void exportAllAndCheck() throws Exception {
        String tempDir = FileUtils.getTempDirectoryPath();
        executeCommand("excel-export", tempDir);
        
        // Find exported file
        File[] files = new File(tempDir).listFiles((dir, name) -> name.contains("export-all") && name.endsWith(".xlsx"));
        assertNotNull("Export file should exist", files);
        assertEquals("Should have exactly one export file", 1, files.length);
        
        // Read the Excel file
        try (FileInputStream fis = new FileInputStream(files[0]);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            
            // Print all rows
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j <= row.getLastCellNum(); j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null) {
                            sb.append(cell.getStringCellValue()).append("\t");
                        } else {
                            sb.append("\t");
                        }
                    }
                    System.out.println(sb.toString());
                }
            }
        }
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
