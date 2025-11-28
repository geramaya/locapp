package de.aspera.locapp.cmd;

import java.io.IOException;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization.Status;

public class PropertiesCounterCommandTest extends BasicFacadeTest {

    @Before
    public void init() throws InstantiationException, IllegalAccessException, CommandException {

        String testfiles = PropertiesCounterCommandTest.class.getClassLoader().getResource("testfiles").getFile();

        executeCommand("init");
        executeCommand("files", testfiles);
        executeCommand("clear-loc");
        executeCommand("import-properties");
    }

    @Test
    public void countSRCPropertiesFiles()
            throws InstantiationException, IllegalAccessException, IOException, DatabaseException, CommandException {
        executeCommand("properties-count", "src");
        executeCommand("properties-count", "src", "-l", "fr", "-e");
        executeCommand("properties-count", "src", "-l", "fr");

        long countSRC = new LocalizationDao().countOfProperties(Status.SRC, null, false);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(10, countSRC);

        countSRC = new LocalizationDao().countOfProperties(Status.SRC, Locale.GERMAN, false);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(2, countSRC);

        countSRC = new LocalizationDao().countOfProperties(Status.SRC, Locale.ENGLISH, false);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(4, countSRC);

        countSRC = new LocalizationDao().countOfProperties(Status.SRC, Locale.FRENCH, false);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(3, countSRC);

        countSRC = new LocalizationDao().countOfProperties(Status.SRC, Locale.FRENCH, true);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(2, countSRC);

        countSRC = new LocalizationDao().countOfProperties(Status.SRC, Locale.ITALIAN, false);
        Assert.assertNotNull(countSRC);
        Assert.assertEquals(1, countSRC);

    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
