package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;
import de.aspera.locapp.util.HelperUtil;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "excel-import",
    aliases = {"ei"},
    description = "Import properties from an excel file.",
    mixinStandardHelpOptions = true
)
public class ExcelImportCommand implements CommandRunnable, Runnable {
    private static final int COL_KEY = 1;
    private static final Logger logger = Logger.getLogger(ExcelExportCommand.class.getName());
    private LocalizationDao locFacade = new LocalizationDao();
    private Map<String, Integer> languagePositonMap = new HashMap<>();

    @Parameters(index = "0", description = "Path to the Excel file to import", arity = "0..1")
    private Path importFile;

    @Override
    public void run() {
        try {
            long start = System.currentTimeMillis();
            importExcel();
            long end = System.currentTimeMillis() - start;
            logger.log(Level.INFO, "Import Excel file in ms: " + end);
        } catch (DatabaseException | IOException | CommandException exp) {
            logger.log(Level.SEVERE, exp.getMessage(), exp);
        }
    }
    
    /**
     * Sets the import file programmatically for testing or legacy support.
     */
    public void setImportFile(Path importFile) {
        this.importFile = importFile;
    }

    private void importExcel() throws DatabaseException, IOException, CommandException {
        String importPath;
        if (importFile != null) {
            importPath = importFile.toString();
        } else {
            importPath = CommandContext.getInstance().nextArgument();
        }
        
        if (StringUtils.isEmpty(importPath) || (!importPath.endsWith(".xlsx") && !importPath.endsWith(".xls"))) {
            logger.severe("No excel file found to import! Please define the full path to excel import file.");
            return;
        }

        FileInputStream excelFile = new FileInputStream(new File(importPath));
        Workbook workbook = WorkbookFactory.create(excelFile);
        Sheet datatypeSheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = datatypeSheet.iterator();
        List<Localization> importLocs = new ArrayList<>();
        int lastVersion = locFacade.lastVersion(Status.XLS) + 1;

        while (iterator.hasNext()) {
            Row row = iterator.next();

            if (row != null) {
                if (StringUtils.isEmpty(getStringValue(row.getCell(0)))) {
                    continue;
                } else if (row.getCell(0).getStringCellValue().toUpperCase().contains("FILENAME")) {
                    for (int i = 0; i < row.getLastCellNum(); i++) {
                        buildLanguagePosMap(row.getCell(i).getStringCellValue(), i);
                    }
                    continue;
                }
                for (String language : languagePositonMap.keySet()) {
                    Localization loc = new Localization();
                    loc.setCreationDate(new Date());
                    String fileName = row.getCell(0).getStringCellValue();
                    loc.setFileName(language.equals(Locale.ENGLISH.toString()) ? fileName
                            : HelperUtil.replaceFullPath(fileName, language));
                    loc.setKey(row.getCell(COL_KEY).getStringCellValue());
                    Cell cellByLanguage = row.getCell(languagePositonMap.get(language));
                    String cellValueByLanguage = getStringValue(cellByLanguage);
                    loc.setValue(cellValueByLanguage != null ? cellValueByLanguage : EMPTY_VALUE);
                    loc.setLocale(language);
                    String fullPath = row.getCell(row.getLastCellNum() - 1).getStringCellValue();
                    loc.setFullPath(language.equals(Locale.ENGLISH.toString()) ? fullPath
                            : HelperUtil.replaceFullPath(fullPath, language));
                    loc.setVersion(lastVersion);
                    loc.setStatus(Localization.Status.XLS);
                    if (!loc.getValue().equals(EMPTY_VALUE)) {
                        importLocs.add(loc);
                    }
                }
            }
        }
        locFacade.saveLocalizations(importLocs);
        workbook.close();
    }

    private Map<String, Integer> buildLanguagePosMap(String cellValue, int cellPos) {
        String language = Locale.ENGLISH.toString();
        String valueIdent = "value_";
        cellValue = cellValue.trim().toLowerCase();
		if (cellValue.contains(valueIdent)) {
            language = cellValue.substring(6,8); // get Locale as String (de, en, fr)
            if (cellValue.trim().toLowerCase().contains(language)) {
                languagePositonMap.put(language, cellPos);
            }
        }
        return languagePositonMap;
    }

    /**
     * @param aDouble
     * @return
     */
    private String getStringFrom(double aDouble) {
        return Double.toString(aDouble);
    }

    private String getStringValue(Cell cell) {
        if (cell != null) {
            switch (cell.getCellType()) {
            case BOOLEAN:
                return cell.getBooleanCellValue() ? "true" : "false";
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell) && cell.getDateCellValue() != null) {
                    return Long.toString(cell.getDateCellValue().getTime());
                }
                return getStringFrom(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            case BLANK:
                return "";
            case ERROR:
                cell.getErrorCellValue();
                return "";
            // CELL_TYPE_FORMULA will never occur
            case FORMULA:
                return "";
            case _NONE:
                break;
            }
        }
        return null;
    }
}
