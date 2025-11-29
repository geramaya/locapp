package de.aspera.locapp.cmd;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.aspera.locapp.dao.BasicFacadeTest;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;

public class ExportDeltaCommandTest extends BasicFacadeTest {

    private static final String TEMP_DIR = FileUtils.getTempDirectoryPath();
    String xlsfile;
    FileInputStream inputStream;
    Workbook workbook;
    Sheet sheet;

    @Before
    public void init() throws InstantiationException, IllegalAccessException, IOException, DatabaseException, CommandException {
        String testfiles = ExcelExportCommandTest.class.getClassLoader().getResource("deltatest").getFile();
        String xlsfile = ExcelImportCommandTest.class.getClassLoader().getResource("slc_excel_to_import.xlsx").getFile();

        executeCommand("init");
        executeCommand("clear-loc");
        executeCommand("files", testfiles);
        executeCommand("import-properties");

        updateFullPath(xlsfile);

        executeCommand("excel-import", xlsfile);

    }

    @Ignore
    @Test
    public void exportDelta() throws InstantiationException, IllegalAccessException, IOException, CommandException {
        executeCommand("export-delta", TEMP_DIR);
        logger.info("saved in: " + TEMP_DIR);

        Cell cell = getCell(2, 5);
        Assert.assertTrue(cell.getStringCellValue().trim().equals("geandert.it"));

        cell = getCell(2, 6);
        Assert.assertTrue(cell.getStringCellValue().trim().equals("geandert.fr"));
    }

    private Cell getCell(int rownum, int colnum) throws IOException {
        FileInputStream file = new FileInputStream(TEMP_DIR + "/delta.xlsx");
        Workbook wb = WorkbookFactory.create(file);
        Sheet sheet = wb.getSheetAt(0);
        Row row = sheet.getRow(rownum);
        Cell cell = row.getCell(colnum);
        wb.close();
        return cell;
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

    @Override
    public Class<?> getLoggerClass() {
        return this.getClass();
    }

}
