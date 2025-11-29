package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization.Status;
import de.aspera.locapp.testutil.LocalizationTestGenerator;

/**
 * Integration test that verifies the complete import/export roundtrip for properties files.
 * 
 * This test validates:
 * 1. Generation of test data using LocalizationTestGenerator
 * 2. Import of generated properties via CLI commands (init, files, cl, ip)
 * 3. Export through Excel workflow (ee, ei)
 * 4. Export back to properties format (ep)
 * 5. Comparison of exported properties with original test data
 * 6. Cleanup of all temporary files and directories
 */
public class PropertiesImportExportRoundtripTest extends BasicFacadeTest {

    private Path tempBasePath;
    private Path testDataDir;
    private Path exportDir;
    private File exportedExcelFile;
    
    /** Map of original properties: filename -> Properties */
    private Map<String, Properties> originalProperties;
    
    /** List of files that were generated */
    private List<String> generatedFileNames;

    @Before
    public void setUp() throws Exception {
        // Create unique temporary directories for this test
        tempBasePath = Files.createTempDirectory("locapp_properties_roundtrip_test");
        exportDir = tempBasePath.resolve("export");
        Files.createDirectories(exportDir);
        
        // Generate realistic test data using LocalizationTestGenerator
        testDataDir = LocalizationTestGenerator.generateRealisticPropertiesFiles(tempBasePath);
        
        // Store the original properties for later comparison
        originalProperties = new HashMap<>();
        generatedFileNames = new ArrayList<>();
        loadOriginalProperties();
        
        logger.info("Test setup complete - Test data: " + testDataDir + ", Files loaded: " + originalProperties.size());
    }

    @After
    public void cleanup() {
        // Clean up exported Excel file
        if (exportedExcelFile != null && exportedExcelFile.exists()) {
            exportedExcelFile.delete();
        }
        
        // Clean up the entire temporary directory created for this test
        if (tempBasePath != null) {
            try {
                FileUtils.deleteDirectory(tempBasePath.toFile());
            } catch (IOException e) {
                logger.warning("Failed to cleanup test directory: " + e.getMessage());
            }
        }
        
        // Verify cleanup was successful
        if (tempBasePath != null && Files.exists(tempBasePath)) {
            logger.warning("WARNING: Temporary directory still exists after cleanup: " + tempBasePath);
        } else {
            logger.info("SUCCESS: All temporary test files have been cleaned up");
        }
    }

    /**
     * Full roundtrip test for properties import and export.
     * 
     * Steps:
     * 1. Generate test data (done in setUp)
     * 2. Initialize config
     * 3. Scan files
     * 4. Clear database
     * 5. Import properties (SRC status)
     * 6. Export to Excel
     * 7. Import from Excel (XLS status)
     * 8. Export to properties
     * 9. Compare exported properties with original
     */
    @Test
    public void testPropertiesImportExportRoundtrip() throws Exception {
        // Step 1: Initialize configuration
        logger.info("Step 1: Initializing configuration...");
        executeCommand("init");
        
        // Step 2: Scan for properties files
        logger.info("Step 2: Scanning for properties files...");
        executeCommand("files", testDataDir.toString());
        
        // Step 3: Clear database
        logger.info("Step 3: Clearing database...");
        executeCommand("clear-loc");
        
        // Step 4: Import properties (creates entries with SRC status)
        logger.info("Step 4: Importing properties...");
        executeCommand("import-properties");
        
        // Step 5: Export to Excel (reads SRC status)
        logger.info("Step 5: Exporting to Excel...");
        executeCommand("excel-export", exportDir.toString());
        
        // Find the exported Excel file
        exportedExcelFile = findExportedFile(exportDir.toString(), "-export-all.xlsx");
        Assert.assertNotNull("Exported Excel file should exist", exportedExcelFile);
        logger.info("Excel export complete: " + exportedExcelFile.getAbsolutePath());
        
        // Step 6: Import from Excel (creates entries with XLS status)
        logger.info("Step 6: Importing from Excel...");
        executeCommand("excel-import", exportedExcelFile.getAbsolutePath());
        
        // Verify XLS entries were created
        LocalizationDao locFacade = new LocalizationDao();
        int xlsVersion = locFacade.lastVersion(Status.XLS);
        var xlsLocs = locFacade.getLocalizations(xlsVersion, Status.XLS, false, null);
        Assert.assertTrue("XLS entries should exist after Excel import", xlsLocs.size() > 0);
        logger.info("XLS entries count: " + xlsLocs.size());
        
        // Step 7: Export to properties format
        logger.info("Step 7: Exporting to properties format...");
        executeCommand("export-properties", exportDir.toString());
        
        // Step 8: Compare exported properties with original test data
        logger.info("Step 8: Comparing exported properties with original test data...");
        List<String> errors = compareExportedProperties();
        
        if (errors.isEmpty()) {
            logger.info("SUCCESS: All properties match! Import/Export roundtrip test PASSED!");
        } else {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("FAILURE: Properties comparison failed with ").append(errors.size()).append(" error(s):\n");
            for (String error : errors) {
                errorMessage.append("  - ").append(error).append("\n");
            }
            Assert.fail(errorMessage.toString());
        }
    }

    /**
     * Loads the original properties files into memory for later comparison.
     */
    private void loadOriginalProperties() throws IOException {
        String[] locales = {"", "_en", "_de", "_fr"};
        
        for (String locale : locales) {
            String fileName = LocalizationTestGenerator.NAMESPACE + locale + ".properties";
            Path filePath = testDataDir.resolve(fileName);
            
            if (Files.exists(filePath)) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                    props.load(fis);
                }
                originalProperties.put(fileName, props);
                generatedFileNames.add(fileName);
            }
        }
    }

    /**
     * Compares the exported properties files with the original test data.
     * Returns a list of error messages if any differences are found.
     */
    private List<String> compareExportedProperties() throws IOException {
        List<String> errors = new ArrayList<>();
        
        for (String fileName : generatedFileNames) {
            Properties original = originalProperties.get(fileName);
            if (original == null) {
                errors.add("Original properties not found for: " + fileName);
                continue;
            }
            
            // Find the exported properties file
            Path exportedFilePath = findExportedPropertiesFile(fileName);
            
            if (exportedFilePath == null || !Files.exists(exportedFilePath)) {
                errors.add("Exported properties file not found: " + fileName);
                continue;
            }
            
            Properties exported = new Properties();
            try (FileInputStream fis = new FileInputStream(exportedFilePath.toFile())) {
                exported.load(fis);
            }
            
            logger.info("Comparing " + fileName + ": original=" + original.size() + " keys, exported=" + exported.size() + " keys");
            
            // Track if any errors found for this file
            int errorCountBefore = errors.size();
            
            // Compare keys and values
            for (String key : original.stringPropertyNames()) {
                String originalValue = original.getProperty(key);
                String exportedValue = exported.getProperty(key);
                
                if (exportedValue == null) {
                    errors.add(fileName + ": Missing key '" + key + "'");
                } else if (!originalValue.equals(exportedValue)) {
                    errors.add(fileName + ": Value mismatch for key '" + key + "': expected='" + originalValue + "', actual='" + exportedValue + "'");
                }
            }
            
            // Check for extra keys in exported file
            for (String key : exported.stringPropertyNames()) {
                if (!original.containsKey(key)) {
                    errors.add(fileName + ": Unexpected extra key in exported file: '" + key + "'");
                }
            }
            
            if (errors.size() == errorCountBefore) {
                logger.info("  " + fileName + ": PASSED");
            }
        }
        
        return errors;
    }

    /**
     * Finds the exported properties file for a given filename.
     * Handles the path transformation done by the export command.
     */
    private Path findExportedPropertiesFile(String fileName) {
        // Try multiple possible locations
        
        // First try: directly under export dir
        Path flatPath = exportDir.resolve(fileName);
        if (Files.exists(flatPath)) {
            return flatPath;
        }
        
        // Second try: search recursively
        try {
            return Files.walk(exportDir)
                .filter(p -> p.getFileName().toString().equals(fileName))
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            logger.warning("Error searching for file " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to find the most recently created export file with a specific suffix.
     */
    private File findExportedFile(String directory, String suffix) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            return null;
        }

        File[] matchingFiles = dir.listFiles((dir1, name) -> name.endsWith(suffix));
        if (matchingFiles == null || matchingFiles.length == 0) {
            return null;
        }

        // Return the most recently modified file
        File mostRecent = matchingFiles[0];
        for (File file : matchingFiles) {
            if (file.lastModified() > mostRecent.lastModified()) {
                mostRecent = file;
            }
        }
        return mostRecent;
    }

    /**
     * Test that verifies exported properties files have their keys sorted alphabetically (A-Z).
     * 
     * Steps:
     * 1. Generate test data and import through the full workflow
     * 2. Export to properties format
     * 3. Read the exported properties file line by line
     * 4. Extract keys (ignoring comments and empty lines)
     * 5. Assert that each key is alphabetically greater than or equal to the previous key
     */
    @Test
    public void testExportedPropertiesAreSortedAlphabetically() throws Exception {
        // Step 1: Initialize configuration
        logger.info("Step 1: Initializing configuration...");
        executeCommand("init");
        
        // Step 2: Scan for properties files
        logger.info("Step 2: Scanning for properties files...");
        executeCommand("files", testDataDir.toString());
        
        // Step 3: Clear database
        logger.info("Step 3: Clearing database...");
        executeCommand("clear-loc");
        
        // Step 4: Import properties (creates entries with SRC status)
        logger.info("Step 4: Importing properties...");
        executeCommand("import-properties");
        
        // Step 5: Export to Excel (reads SRC status)
        logger.info("Step 5: Exporting to Excel...");
        executeCommand("excel-export", exportDir.toString());
        
        // Find the exported Excel file
        exportedExcelFile = findExportedFile(exportDir.toString(), "-export-all.xlsx");
        Assert.assertNotNull("Exported Excel file should exist", exportedExcelFile);
        
        // Step 6: Import from Excel (creates entries with XLS status)
        logger.info("Step 6: Importing from Excel...");
        executeCommand("excel-import", exportedExcelFile.getAbsolutePath());
        
        // Step 7: Export to properties format
        logger.info("Step 7: Exporting to properties format...");
        executeCommand("export-properties", exportDir.toString());
        
        // Step 8: Verify alphabetical sorting of exported properties
        logger.info("Step 8: Verifying alphabetical sorting of exported properties...");
        
        // Check the English properties file (app_en.properties)
        Path exportedEnFile = findExportedPropertiesFile("app_en.properties");
        Assert.assertNotNull("Exported app_en.properties should exist", exportedEnFile);
        Assert.assertTrue("Exported file should exist", Files.exists(exportedEnFile));
        
        // Read the file and verify keys are sorted
        List<String> keys = extractKeysFromPropertiesFile(exportedEnFile);
        Assert.assertFalse("Properties file should have keys", keys.isEmpty());
        logger.info("Found " + keys.size() + " keys in exported properties file");
        
        // Verify that each key is alphabetically >= the previous key
        for (int i = 1; i < keys.size(); i++) {
            String previousKey = keys.get(i - 1);
            String currentKey = keys.get(i);
            
            int comparison = previousKey.compareTo(currentKey);
            Assert.assertTrue(
                "Keys should be in alphabetical order: '" + previousKey + "' should come before or equal to '" + currentKey + "'",
                comparison <= 0
            );
        }
        
        logger.info("SUCCESS: All keys in exported properties file are sorted alphabetically (A-Z)!");
    }

    /**
     * Extracts property keys from a .properties file, ignoring comments and empty lines.
     * 
     * @param propertiesFile the path to the properties file
     * @return a list of keys in the order they appear in the file
     */
    private List<String> extractKeysFromPropertiesFile(Path propertiesFile) throws IOException {
        List<String> keys = new ArrayList<>();
        List<String> lines = Files.readAllLines(propertiesFile);
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip empty lines
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // Skip comment lines (starting with # or !)
            if (trimmedLine.startsWith("#") || trimmedLine.startsWith("!")) {
                continue;
            }
            
            // Extract the key (everything before the first = or :)
            int equalsIndex = trimmedLine.indexOf('=');
            int colonIndex = trimmedLine.indexOf(':');
            
            // Determine the separator index (first valid separator, or skip if none found)
            int separatorIndex = findFirstSeparatorIndex(equalsIndex, colonIndex);
            if (separatorIndex == -1) {
                continue;
            }
            
            String key = trimmedLine.substring(0, separatorIndex).trim();
            if (!key.isEmpty()) {
                keys.add(key);
            }
        }
        
        return keys;
    }

    /**
     * Finds the index of the first valid separator (= or :) in a properties file line.
     * Returns -1 if no separator is found.
     * 
     * @param equalsIndex index of '=' character, or -1 if not found
     * @param colonIndex index of ':' character, or -1 if not found
     * @return the index of the first separator, or -1 if neither is found
     */
    private int findFirstSeparatorIndex(int equalsIndex, int colonIndex) {
        if (equalsIndex == -1 && colonIndex == -1) {
            return -1;
        }
        if (equalsIndex == -1) {
            return colonIndex;
        }
        if (colonIndex == -1) {
            return equalsIndex;
        }
        return Math.min(equalsIndex, colonIndex);
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
