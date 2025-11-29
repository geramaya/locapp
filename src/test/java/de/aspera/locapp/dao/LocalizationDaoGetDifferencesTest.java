package de.aspera.locapp.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.cmd.CommandException;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;

/**
 * Unit test for the getLocalizationDifferences method in LocalizationDao.
 * This test validates that the delta detection logic correctly identifies
 * differences between SRC and XLS versions.
 */
public class LocalizationDaoGetDifferencesTest extends BasicFacadeTest {

    private LocalizationDao locFacade;

    @Before
    public void init() throws Exception {
        locFacade = new LocalizationDao();
    }

    /**
     * Test that getLocalizationDifferences correctly identifies a modified value.
     */
    @Test
    public void testGetLocalizationDifferencesWithModification() throws DatabaseException, CommandException {
        // Create SRC entries version 1
        List<Localization> srcEntries = new ArrayList<>();
        srcEntries.add(createLocalization("key.test1", "en", "OriginalValue", ".//test.properties", Status.SRC, 1));
        srcEntries.add(createLocalization("key.test1", "de", "OriginalerWert", ".//test_de.properties", Status.SRC, 1));
        locFacade.saveLocalizations(srcEntries);

        // Create XLS entries version 1 with a modified German value
        List<Localization> xlsEntries = new ArrayList<>();
        xlsEntries.add(createLocalization("key.test1", "en", "OriginalValue", ".//test.properties", Status.XLS, 1));
        xlsEntries.add(createLocalization("key.test1", "de", "ModifiedValue", ".//test_de.properties", Status.XLS, 1));
        locFacade.saveLocalizations(xlsEntries);

        // Get differences
        List<Localization> differences = locFacade.getLocalizationDifferences(1, 1);

        // Should find the modified German entry
        Assert.assertEquals("Should find exactly 1 difference", 1, differences.size());
        
        Localization diff = differences.get(0);
        Assert.assertEquals("Should be the German locale", "de", diff.getLocale());
        Assert.assertEquals("Should be the modified value", "ModifiedValue", diff.getValue());
    }

    /**
     * Test that getLocalizationDifferences returns empty when values are the same.
     */
    @Test
    public void testGetLocalizationDifferencesNoChanges() throws DatabaseException, CommandException {
        // Create SRC entries version 2 (to avoid conflict with previous test)
        List<Localization> srcEntries = new ArrayList<>();
        srcEntries.add(createLocalization("key.test2", "en", "SameValue", ".//test.properties", Status.SRC, 2));
        srcEntries.add(createLocalization("key.test2", "de", "SameValue", ".//test_de.properties", Status.SRC, 2));
        locFacade.saveLocalizations(srcEntries);

        // Create XLS entries version 2 with SAME values
        List<Localization> xlsEntries = new ArrayList<>();
        xlsEntries.add(createLocalization("key.test2", "en", "SameValue", ".//test.properties", Status.XLS, 2));
        xlsEntries.add(createLocalization("key.test2", "de", "SameValue", ".//test_de.properties", Status.XLS, 2));
        locFacade.saveLocalizations(xlsEntries);

        // Get differences
        List<Localization> differences = locFacade.getLocalizationDifferences(2, 2);

        // Should find no differences
        Assert.assertEquals("Should find 0 differences when values are the same", 0, differences.size());
    }

    /**
     * Test that getLocalizationDifferences detects new keys in XLS.
     */
    @Test
    public void testGetLocalizationDifferencesNewKey() throws DatabaseException, CommandException {
        // Create SRC entries version 3
        List<Localization> srcEntries = new ArrayList<>();
        srcEntries.add(createLocalization("key.existing", "en", "ExistingValue", ".//test.properties", Status.SRC, 3));
        locFacade.saveLocalizations(srcEntries);

        // Create XLS entries version 3 with an additional new key
        List<Localization> xlsEntries = new ArrayList<>();
        xlsEntries.add(createLocalization("key.existing", "en", "ExistingValue", ".//test.properties", Status.XLS, 3));
        xlsEntries.add(createLocalization("key.newKey", "en", "NewValue", ".//test.properties", Status.XLS, 3));
        locFacade.saveLocalizations(xlsEntries);

        // Get differences
        List<Localization> differences = locFacade.getLocalizationDifferences(3, 3);

        // Should find the new key
        Assert.assertEquals("Should find 1 difference for the new key", 1, differences.size());
        Assert.assertEquals("Should be the new key", "key.newKey", differences.get(0).getKey());
    }

    /**
     * Test that getLocalizationDifferences returns all entries when no previous version exists.
     */
    @Test
    public void testGetLocalizationDifferencesNoPreviousVersion() throws DatabaseException, CommandException {
        // Create only XLS entries version 4 (no SRC entries)
        List<Localization> xlsEntries = new ArrayList<>();
        xlsEntries.add(createLocalization("key.only1", "en", "Value1", ".//test.properties", Status.XLS, 4));
        xlsEntries.add(createLocalization("key.only2", "en", "Value2", ".//test.properties", Status.XLS, 4));
        locFacade.saveLocalizations(xlsEntries);

        // Get differences with srcVersion = 0 (no previous version)
        List<Localization> differences = locFacade.getLocalizationDifferences(0, 4);

        // Should return all XLS entries when there's no previous version
        Assert.assertEquals("Should return all 2 XLS entries", 2, differences.size());
    }

    /**
     * Helper method to create a Localization object.
     */
    private Localization createLocalization(String key, String locale, String value, String fullPath, 
            Status status, int version) {
        Localization loc = new Localization();
        loc.setKey(key);
        loc.setLocale(locale);
        loc.setValue(value);
        loc.setFullPath(fullPath);
        loc.setFileName(fullPath.substring(fullPath.lastIndexOf("/") + 1));
        loc.setStatus(status);
        loc.setVersion(version);
        loc.setCreationDate(new Date());
        return loc;
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
