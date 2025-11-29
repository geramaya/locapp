package de.aspera.locapp.cmd;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;

public class ImportDeltaCommandTest extends BasicFacadeTest {

    LocalizationDao locaFacade;
    String deltafile;

    @Before
    public void init() throws InstantiationException, IllegalAccessException, DatabaseException, IOException, CommandException {

        String testfiles = ExcelExportCommandTest.class.getClassLoader().getResource("deltatest").getFile();
        String xlsfile = ExcelImportCommandTest.class.getClassLoader().getResource("slc_excel_to_import.xlsx").getFile();
        locaFacade = new LocalizationDao();

        executeCommand("init");
        executeCommand("clear-loc");
        executeCommand("files", testfiles);
        executeCommand("import-properties");

        updateFullPath(xlsfile);

        executeCommand("excel-import", xlsfile);
        executeCommand("export-delta", testfiles);

        deltafile = ExcelImportCommandTest.class.getClassLoader().getResource("deltatest/" + getDeltaFile()).getFile();

        executeCommand("import-delta", deltafile);
    }

    @Ignore
    @Test
    public void importDelta() throws InstantiationException, IllegalAccessException, DatabaseException {
        List<Localization> oldLocals;
        List<Localization> newLocals;
        oldLocals = locaFacade.getLocalizationsWithLastVersion(locaFacade.lastVersion(null) - 1);
        newLocals = locaFacade.getLocalizationsWithLastVersion(locaFacade.lastVersion(null));

        HashMap<String, Localization> newLocalHash = buildHash(newLocals);
        HashMap<String, Localization> oldLocalHash = buildHash(oldLocals);

        for (String key : oldLocalHash.keySet()) {
            Localization oldLoc = oldLocalHash.get(key);
            Localization newLoc = newLocalHash.get(key);
            assertTrue(newLoc != null);
            assertTrue(oldLoc.getFullPath().equals(newLoc.getFullPath()));

        }

        List<String> restValues = new ArrayList<>();
        for (String key : newLocalHash.keySet()) {
            restValues.add(newLocalHash.get(key).getValue().trim());
        }
        assertTrue(restValues.contains("geandert.it"));
        assertTrue(restValues.contains("geandert.fr"));
    }

    @After
    public void cleanUp() {
        File file = new File(deltafile);
        file.delete();
    }

    private HashMap<String, Localization> buildHash(List<Localization> localList) {
        HashMap<String, Localization> localHash = new HashMap<>();
        for (Localization local : localList) {
            localHash.put(local.getFullPath(), local);
        }

        return localHash;
    }

    private void updateFullPath(String xlsPath) throws IOException, DatabaseException {
        FileInputStream file = new FileInputStream(xlsPath);
        Workbook wb = WorkbookFactory.create(file);
        Sheet sheet = wb.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();
        LocalizationDao locaFacade = new LocalizationDao();
        List<Localization> localizations = locaFacade.getLocalizations(locaFacade.lastVersion(Status.SRC), Status.SRC, false, null);
        String fullPath = localizations.get(0).getFullPath();
        String resultFullPath;

        while (rows.hasNext()) {
            Row row = rows.next();

            if (row.getCell(0).getStringCellValue().equals("Filename")) {
                continue;
            }

            String rowFullPath = row.getCell(6).getStringCellValue();
            resultFullPath = fullPath.substring(0, fullPath.indexOf("\\deltatest"))
                    + rowFullPath.substring(rowFullPath.indexOf("\\deltatest"));
            row.getCell(6).setCellValue(resultFullPath);
        }
        FileOutputStream out = new FileOutputStream(xlsPath);
        wb.write(out);
        wb.close();
    }

    private String getDeltaFile() {
        File dir = new File("target/test-classes/deltatest");
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith("delta.xlsx")) {
                    return file.getName();
                }
            }
        }
        return null;
    }

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }

}
