package de.aspera.locapp.cmd;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dao.DatabaseException;

public class DebugLanguagesTest extends BasicFacadeTest {

    @Before
    public void init() throws Exception {
        String testfiles = DebugLanguagesTest.class.getClassLoader().getResource("testfiles").getFile();
        executeCommand("init");
        executeCommand("files", testfiles);
        executeCommand("clear-loc");
        executeCommand("import-properties");
    }

    @Test
    public void checkLanguagesOrder() throws Exception {
        LocalizationDao locFacade = new LocalizationDao();
        List<String> languages = locFacade.getLanguages(false);
        System.out.println("Languages order: " + languages);
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
