package de.aspera.locapp.cmd;

import java.util.List;
import java.util.Set;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;

public class DebugLocsTest extends BasicFacadeTest {

    @Before
    public void init() throws Exception {
        String testfiles = DebugLocsTest.class.getClassLoader().getResource("testfiles").getFile();
        executeCommand("init");
        executeCommand("files", testfiles);
        executeCommand("clear-loc");
        executeCommand("import-properties");
    }

    @Test
    public void checkLocalizations() throws Exception {
        LocalizationDao locFacade = new LocalizationDao();
        
        int lastVersion = locFacade.lastVersion(Status.SRC);
        System.out.println("Last version: " + lastVersion);
        
        Set<String> files = locFacade.getFiles(Locale.ENGLISH, false);
        System.out.println("Files for EN: " + files);
        
        for (String fullPath : files) {
            System.out.println("\n--- Localizations for path: " + fullPath + " ---");
            List<Localization> locs = locFacade.getLocalizations(lastVersion, Status.SRC, false, fullPath);
            for (Localization loc : locs) {
                System.out.println("Key: " + loc.getKey() + ", Locale: " + loc.getLocale() + 
                                   ", Value: '" + loc.getValue() + "', FullPath: " + loc.getFullPath());
            }
        }
        
        System.out.println("\n--- All localizations ---");
        List<Localization> allLocs = locFacade.getLocalizations(lastVersion, Status.SRC, false, null);
        for (Localization loc : allLocs) {
            System.out.println("Key: " + loc.getKey() + ", Locale: " + loc.getLocale() + 
                               ", Value: '" + loc.getValue() + "', FullPath: " + loc.getFullPath());
        }
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
