package de.aspera.locapp.cmd;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization.Status;

public class ExcelImportCommandTest extends BasicFacadeTest {
    @Test
    public void importExcelFile()
            throws InstantiationException, IllegalAccessException, IOException, DatabaseException, CommandException {
        executeCommand("clear-loc");
        String file = ExcelImportCommandTest.class.getClassLoader().getResource("slc_excel_export.xlsx").getFile();
        executeCommand("excel-import", file);

        // test the count of imported properties from a excel file.
        executeCommand("properties-count", "xls");
        long countSRC = new LocalizationDao().countOfProperties(Status.XLS, null, false);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(countSRC, 8);

    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }

}
