package de.aspera.locapp.cmd;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;

/**
 * Tests for the SearchCommand and LocalizationDao.searchLocalizations method.
 */
public class SearchCommandTest extends BasicFacadeTest {

    private LocalizationDao localizationDao;

    @Before
    public void init() throws InstantiationException, IllegalAccessException, CommandException {
        String testfiles = SearchCommandTest.class.getClassLoader().getResource("testfiles").getFile();

        CMDCTX.addArgument("init");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("files");
        CMDCTX.addArgument(testfiles);
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("cl");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        CMDCTX.addArgument("ip");
        CMDCTX.executeCommand(CMDCTX.nextArgument());

        localizationDao = new LocalizationDao();
    }

    @Test
    public void testSearchByKey() throws DatabaseException {
        // Search for "foobar" - should find all entries with matching keys
        List<Localization> results = localizationDao.searchLocalizations("foobar");
        
        Assert.assertNotNull(results);
        Assert.assertFalse("Results should not be empty", results.isEmpty());
        
        // Should find entries across multiple languages
        boolean hasEnglish = false;
        boolean hasGerman = false;
        
        for (Localization loc : results) {
            if ("en".equalsIgnoreCase(loc.getLocale())) {
                hasEnglish = true;
            }
            if ("de".equalsIgnoreCase(loc.getLocale())) {
                hasGerman = true;
            }
        }
        
        Assert.assertTrue("Should find English translations", hasEnglish);
        Assert.assertTrue("Should find German translations", hasGerman);
    }

    @Test
    public void testSearchByValue() throws DatabaseException {
        // Search for "EN" in values - should find entries containing "EN" in value
        List<Localization> results = localizationDao.searchLocalizations("EN");
        
        Assert.assertNotNull(results);
        Assert.assertFalse("Results should not be empty", results.isEmpty());
        
        // The search finds entries where the value contains "EN"
        // Since "EN" appears in English property values like "foobar(EN)"
        // it will find those entries and return all translations for those keys/files
        boolean foundMatch = false;
        for (Localization loc : results) {
            if (loc.getValue() != null && loc.getValue().toUpperCase().contains("EN")) {
                foundMatch = true;
                break;
            }
        }
        
        Assert.assertTrue("Should find entries with 'EN' in value", foundMatch);
    }

    @Test
    public void testSearchCaseInsensitive() throws DatabaseException {
        // Search with different cases
        List<Localization> resultsLower = localizationDao.searchLocalizations("foobar");
        List<Localization> resultsUpper = localizationDao.searchLocalizations("FOOBAR");
        List<Localization> resultsMixed = localizationDao.searchLocalizations("FooBar");
        
        Assert.assertEquals("Case-insensitive search should return same results",
                resultsLower.size(), resultsUpper.size());
        Assert.assertEquals("Case-insensitive search should return same results",
                resultsLower.size(), resultsMixed.size());
    }

    @Test
    public void testSearchNoResults() throws DatabaseException {
        // Search for something that doesn't exist
        List<Localization> results = localizationDao.searchLocalizations("nonexistent_xyz_12345");
        
        Assert.assertNotNull(results);
        Assert.assertTrue("Should return empty list for no matches", results.isEmpty());
    }

    @Test
    public void testSearchEmptyQuery() throws DatabaseException {
        // Empty query should return empty list
        List<Localization> resultsNull = localizationDao.searchLocalizations(null);
        List<Localization> resultsEmpty = localizationDao.searchLocalizations("");
        List<Localization> resultsSpaces = localizationDao.searchLocalizations("   ");
        
        Assert.assertTrue("Null query should return empty list", resultsNull.isEmpty());
        Assert.assertTrue("Empty query should return empty list", resultsEmpty.isEmpty());
    }

    @Test
    public void testSearchCommandOutput() throws DatabaseException {
        // Capture System.out to verify command output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            SearchCommand searchCommand = new SearchCommand();
            searchCommand.setQuery("foobar");
            searchCommand.setShowAll(true);
            searchCommand.run();

            String output = outContent.toString();
            
            // Verify output contains expected elements
            Assert.assertTrue("Output should contain 'Found' message", 
                    output.contains("Found") && output.contains("matching key"));
            Assert.assertTrue("Output should contain table separator", 
                    output.contains("+--"));
            Assert.assertTrue("Output should contain 'Filename' header", 
                    output.contains("Filename"));
            Assert.assertTrue("Output should contain 'Key' header", 
                    output.contains("Key"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testSearchCommandNoResults() throws DatabaseException {
        // Capture System.out to verify command output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            SearchCommand searchCommand = new SearchCommand();
            searchCommand.setQuery("nonexistent_xyz_12345");
            searchCommand.run();

            String output = outContent.toString();
            
            // Verify output indicates no results
            Assert.assertTrue("Output should indicate no results", 
                    output.contains("No results found"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
