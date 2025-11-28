package de.aspera.locapp.cmd;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization.Status;

public class CSVImportCommandTest extends BasicFacadeTest {
	@Test
	public void importCSVFile()
			throws InstantiationException, IllegalAccessException, IOException, DatabaseException, CommandException {
		String file = CSVImportCommandTest.class.getClassLoader().getResource("csv_import_test.csv").getFile();
		executeCommand("csv-import", file);
        long countSRC = new LocalizationDao().countOfProperties(Status.CSV, null, false);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(countSRC, 9);
		
	}

	@Override
	public Class<?> getLoggerClass() {
		return this.getClass();
	}

}
