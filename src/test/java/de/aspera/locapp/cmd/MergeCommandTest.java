package de.aspera.locapp.cmd;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;

public class MergeCommandTest extends BasicFacadeTest {

    @Before
    public void init() throws InstantiationException, IllegalAccessException, CommandException {

        executeCommand("init");
        executeCommand("clear-loc");

        String[] files = new String[]{"export_all_edit.xlsx", "export_de_edit.xlsx", "export_it_edit.xlsx"};
        for (String file : files) {
            String filePath = ExcelImportCommandTest.class.getClassLoader().getResource(file).getFile();
            executeCommand("excel-import", filePath);
        }
    }

    @Test
    public void countSRCPropertiesFiles()
            throws InstantiationException, IllegalAccessException, IOException, DatabaseException, CommandException {
        executeCommand("merge-properties", "xls");
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
