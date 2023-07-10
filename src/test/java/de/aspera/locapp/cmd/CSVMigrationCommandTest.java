package de.aspera.locapp.cmd;

import java.io.IOException;

import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;

public class CSVMigrationCommandTest extends BasicFacadeTest {
	

	public void initTest() throws CommandException {
        String testfiles = "/home/dweiss/_develop/SLC_MASTER/";

        CMDCTX.addArgument("init");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("cl");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("files");
        CMDCTX.addArgument(testfiles);
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("ip");
        CMDCTX.executeCommand(CMDCTX.nextArgument());		
	}
	
	
	@Test
	public void importCSVFile()
			throws InstantiationException, IllegalAccessException, IOException, DatabaseException, CommandException {
		// initTest();
		
		CMDCTX.addArgument("cscmig");
		String file = CSVMigrationCommandTest.class.getClassLoader().getResource("SAPOP_Translations_CSC.csv").getFile();
		CMDCTX.addArgument(file);
		CMDCTX.executeCommand(CMDCTX.nextArgument());
//        long countSRC = new LocalizationDao().countOfProperties(Status.CSV, null, false);
//        Assert.assertNotNull(countSRC);
//        Assert.assertEquals(countSRC, 9);
		
	}

	@Override
	public Class<?> getLoggerClass() {
		return this.getClass();
	}

}
