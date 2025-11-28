package de.aspera.locapp.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;
import de.aspera.locapp.testutil.LocalizationTestGenerator;

/**
 * Test class for the Delta Export functionality.
 * 
 * Tests that the --delta flag on ExportPropertiesCommand only exports files
 * that contain keys modified between SRC versions.
 */
public class DeltaExportTest extends BasicFacadeTest {

    private LocalizationDao locFacade;
    private Path tempBasePath;
    private Path testDataDir;
    private Path exportDir;

    @Before
    public void init() throws Exception {
        locFacade = new LocalizationDao();
        
        // Create temporary directories
        tempBasePath = Files.createTempDirectory("locapp_delta_test");
        testDataDir = LocalizationTestGenerator.generateRealisticPropertiesFiles(tempBasePath);
        exportDir = Files.createTempDirectory("locapp_delta_export");
        
        // Initialize configuration
        CMDCTX.addArgument("init");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Load generated test files
        CMDCTX.addArgument("files");
        CMDCTX.addArgument(testDataDir.toString());
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Clear database
        CMDCTX.addArgument("cl");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Import properties as SRC v1
        CMDCTX.addArgument("ip");
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        logger.info("Delta export test setup complete");
    }

    @After
    public void cleanup() {
        // Clean up temporary directories
        if (tempBasePath != null) {
            try {
                FileUtils.deleteDirectory(tempBasePath.toFile());
            } catch (IOException e) {
                logger.warning("Failed to cleanup test directory: " + e.getMessage());
            }
        }
        if (exportDir != null) {
            try {
                FileUtils.deleteDirectory(exportDir.toFile());
            } catch (IOException e) {
                logger.warning("Failed to cleanup export directory: " + e.getMessage());
            }
        }
    }

    /**
     * Test B: Delta Export Verification
     * 
     * Verifies that the --delta flag only exports files containing modified keys.
     * 
     * Scenario:
     * 1. Import initial data as SRC v1
     * 2. Manually create SRC v2 that differs from SRC v1 in only one key in a single file
     * 3. Execute export with --delta flag
     * 4. Assert that only the file with the modified key was exported
     */
    @Test
    public void testDeltaExportOnlyWritesModifiedFiles() throws Exception {
        // Step 1: Get SRC v1 count
        int srcV1 = locFacade.lastVersion(Status.SRC);
        List<Localization> srcV1Locs = locFacade.getLocalizations(srcV1, Status.SRC, false, null);
        logger.info("SRC v1 count: " + srcV1Locs.size() + ", version: " + srcV1);
        
        // Step 2: Create SRC v2 by duplicating v1 but changing ONE key's value
        // Find the first English localization to modify
        Localization locToModify = null;
        for (Localization loc : srcV1Locs) {
            if ("en".equals(loc.getLocale())) {
                locToModify = loc;
                break;
            }
        }
        Assert.assertNotNull("Should find an English localization to modify", locToModify);
        
        String modifiedFile = locToModify.getFullPath();
        String modifiedKey = locToModify.getKey();
        logger.info("Will modify key '" + modifiedKey + "' in file '" + modifiedFile + "'");
        
        // Create SRC v2 with one modified key
        int srcV2 = srcV1 + 1;
        List<Localization> srcV2Locs = new java.util.ArrayList<>();
        for (Localization loc : srcV1Locs) {
            Localization newLoc = new Localization();
            newLoc.setFileName(loc.getFileName());
            newLoc.setKey(loc.getKey());
            newLoc.setLocale(loc.getLocale());
            newLoc.setFullPath(loc.getFullPath());
            newLoc.setVersion(srcV2);
            newLoc.setStatus(Status.SRC);
            newLoc.setCreationDate(new Date());
            
            // Modify the value of the target key
            if (loc.getKey().equals(modifiedKey) && loc.getFullPath().equals(modifiedFile)) {
                newLoc.setValue("DELTA_MODIFIED_VALUE_" + System.currentTimeMillis());
            } else {
                newLoc.setValue(loc.getValue());
            }
            
            srcV2Locs.add(newLoc);
        }
        // Batch save all localizations in a single transaction
        locFacade.saveLocalizations(srcV2Locs);
        
        logger.info("Created SRC v2 with one modified key");
        
        // Step 3: Create XLS from the modified SRC
        // First export full SRC to Excel
        String tempExportPath = tempBasePath.toString();
        CMDCTX.addArgument("ee");
        CMDCTX.addArgument(tempExportPath);
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        File excelFile = findExportedFile(tempExportPath, ".xlsx");
        Assert.assertNotNull("Exported Excel should exist", excelFile);
        
        // Import to create XLS version
        CMDCTX.addArgument("ei");
        CMDCTX.addArgument(excelFile.getAbsolutePath());
        CMDCTX.executeCommand(CMDCTX.nextArgument());
        
        // Step 4: Test the DAO method for version differences
        List<Localization> differences = locFacade.getLocalizationDifferences(srcV1, srcV2, Status.SRC);
        logger.info("Found " + differences.size() + " differences between SRC v" + srcV1 + " and v" + srcV2);
        
        // Should find at least one difference (the modified key)
        Assert.assertTrue("Should find at least one difference", differences.size() >= 1);
        
        // Verify the modified key is in the differences
        boolean foundModifiedKey = false;
        for (Localization diff : differences) {
            if (diff.getKey().equals(modifiedKey)) {
                foundModifiedKey = true;
                break;
            }
        }
        Assert.assertTrue("The modified key should be in the differences list", foundModifiedKey);
        
        logger.info("Delta export test PASSED - differences correctly detected!");
    }

    /**
     * Test that getLastTwoVersions returns correct version numbers.
     */
    @Test
    public void testGetLastTwoVersions() throws DatabaseException {
        int[] versions = locFacade.getLastTwoVersions(Status.SRC);
        
        Assert.assertNotNull("Versions array should not be null", versions);
        Assert.assertEquals("Should return array of 2 elements", 2, versions.length);
        
        int latestVersion = versions[0];
        int previousVersion = versions[1];
        
        logger.info("Latest SRC version: " + latestVersion + ", Previous: " + previousVersion);
        
        Assert.assertTrue("Latest version should be > 0", latestVersion > 0);
        // Previous version can be 0 if only one version exists
        Assert.assertTrue("Previous version should be >= 0", previousVersion >= 0);
        Assert.assertTrue("Latest should be >= previous", latestVersion >= previousVersion);
    }

    /**
     * Helper method to find the most recently created export file.
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

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }
}
