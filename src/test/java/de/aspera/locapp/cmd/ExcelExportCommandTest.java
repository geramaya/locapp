package de.aspera.locapp.cmd;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;

public class ExcelExportCommandTest extends BasicFacadeTest {

    @Before
    public void init() throws InstantiationException, IllegalAccessException, CommandException {

        String testfiles = ExcelExportCommandTest.class.getClassLoader().getResource("testfiles").getFile();

        executeCommand("init");
        executeCommand("files", testfiles);
        executeCommand("clear-loc");
        executeCommand("import-properties");
    }

    private static final String TEMP_DIR = FileUtils.getTempDirectoryPath();

    @Test
    public void exportExcelFile() throws InstantiationException, IllegalAccessException, IOException, CommandException {
        executeCommand("excel-export", TEMP_DIR, "-l", "fr", "-e");
        logger.info("saved in: " + TEMP_DIR);
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
