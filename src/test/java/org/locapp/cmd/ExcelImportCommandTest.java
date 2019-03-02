package org.locapp.cmd;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.locapp.dao.BasicFacadeTest;
import org.locapp.dao.DatabaseException;
import org.locapp.dao.LocalizationFacade;
import org.locapp.dto.Localization.Status;

public class ExcelImportCommandTest extends BasicFacadeTest {
    @Test
    public void importExcelFile()
            throws InstantiationException, IllegalAccessException, IOException, DatabaseException {
        CMDCTX.addArgument("cl");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        CMDCTX.addArgument("ei");
        String file = ExcelImportCommandTest.class.getClassLoader().getResource("locapp_excel_export.xls").getFile();
        CMDCTX.addArgument(file);
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        // test the count of imported properties from a excel file.
        CMDCTX.addArgument("pc");
        CMDCTX.addArgument("xls");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        long countSRC = new LocalizationFacade().countOfProperties(Status.XLS, null, false);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(countSRC, 8);

    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }

}
