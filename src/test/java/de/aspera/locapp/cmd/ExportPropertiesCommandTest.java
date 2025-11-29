package de.aspera.locapp.cmd;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;

public class ExportPropertiesCommandTest extends BasicFacadeTest {

    private static final String TEMP_DIR = FileUtils.getTempDirectoryPath();

    @Test
    public void importExcelFile() throws InstantiationException, IllegalAccessException, IOException, CommandException {
        executeCommand("clear-loc");

        String file = ExportPropertiesCommandTest.class.getClassLoader().getResource("slc_excel_export.xlsx").getFile();
        executeCommand("excel-import", file);

        executeCommand("export-properties", TEMP_DIR);

    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
