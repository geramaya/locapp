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
    description = "Import properties from an excel file using MERGE strategy (inherits from previous XLS version).",
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

    /**
     * Generates a unique key for a localization entry for MERGE operations.
     * Format: fileName|key|locale
     */
    private String generateMergeKey(Localization loc) {
        return loc.getFileName() + "|" + loc.getKey() + "|" + loc.getLocale();
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

        // MERGE Strategy: Load inherited entries from previous XLS version
        int previousVersion = locFacade.lastVersion(Status.XLS);
        int newVersion = previousVersion + 1;
        
        // Map to store inherited entries (keyed by fileName|key|locale)
        Map<String, Localization> inheritedMap = new HashMap<>();
        if (previousVersion > 0) {
            List<Localization> inheritedLocs = locFacade.getLocalizations(previousVersion, Status.XLS, false, null);
            logger.log(Level.INFO, "Inheriting " + inheritedLocs.size() + " entries from XLS version " + previousVersion);
            
            for (Localization loc : inheritedLocs) {
                String key = generateMergeKey(loc);
                inheritedMap.put(key, loc);
            }
        }

        FileInputStream excelFile = new FileInputStream(new File(importPath));
        Workbook workbook = WorkbookFactory.create(excelFile);
        Sheet datatypeSheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = datatypeSheet.iterator();
        List<Localization> importLocs = new ArrayList<>();
        // Track which inherited entries have been updated by Excel data
        java.util.Set<String> updatedFromExcel = new java.util.HashSet<>();

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
                    loc.setVersion(newVersion);
                    loc.setStatus(Localization.Status.XLS);
                    importLocs.add(loc);
                    
                    // Track this key as coming from Excel
                    updatedFromExcel.add(generateMergeKey(loc));
                }
            }
        }
        
        // Add inherited entries that were NOT in the Excel (MERGE)
        int inheritedCount = 0;
        for (Map.Entry<String, Localization> entry : inheritedMap.entrySet()) {
            if (!updatedFromExcel.contains(entry.getKey())) {
                Localization inheritedLoc = entry.getValue();
                // Create new localization for new version
                Localization loc = new Localization();
                loc.setCreationDate(new Date());
                loc.setFileName(inheritedLoc.getFileName());
                loc.setKey(inheritedLoc.getKey());
                loc.setValue(inheritedLoc.getValue());
                loc.setLocale(inheritedLoc.getLocale());
                loc.setFullPath(inheritedLoc.getFullPath());
                loc.setVersion(newVersion);
                loc.setStatus(Status.XLS);
                importLocs.add(loc);
                inheritedCount++;
            }
        }
        
        if (inheritedCount > 0) {
            logger.log(Level.INFO, "MERGE: Added " + inheritedCount + " inherited entries not present in Excel");
        }
        
        locFacade.saveLocalizations(importLocs);
        workbook.close();
        logger.log(Level.INFO, "Import complete: " + importLocs.size() + " total entries in XLS version " + newVersion);
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
