package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.aspera.locapp.dao.ConfigDao;
import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;
import de.aspera.locapp.util.HelperUtil;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "excel-export",
    aliases = {"ee"},
    description = "Export properties into an excel file (all or by language ISOCODE, search for empty values).",
    mixinStandardHelpOptions = true
)
public class ExcelExportCommand implements CommandRunnable, Runnable {

    private static final String    HEADER        = "header";
    private static final String    STYLE_YELLOW  = "style_yellow";
    private static final int       ROWGAP_HEADER = 0;
    private static final Logger    logger        = Logger.getLogger(ExcelExportCommand.class.getName());
    private LocalizationDao     locFacade     = new LocalizationDao();
    private ConfigDao           configFacade  = new ConfigDao();
    private Map<String, CellStyle> styleMap      = new HashMap<>();
    private String                 fileName;

    @Parameters(index = "0", description = "Export directory path", arity = "0..1")
    private Path exportPath;

    @Option(names = {"-l", "--language"}, description = "Language ISO code (e.g., de, en, fr)")
    private String language;

    @Option(names = {"-e", "--empty"}, description = "Export only empty properties", defaultValue = "false")
    private boolean emptyProperties;

    public void initStyles(Workbook wb) {
        CellStyle header = wb.createCellStyle();
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font_header = wb.createFont();
        font_header.setBold(true);
        font_header.setColor(IndexedColors.BLACK.getIndex());
        header.setFont(font_header);
        header.setAlignment(HorizontalAlignment.CENTER);

        CellStyle style_yellow = wb.createCellStyle();
        style_yellow.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style_yellow.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font_green = wb.createFont();
        font_green.setBold(true);
        font_green.setColor(IndexedColors.GREEN.getIndex());
        style_yellow.setFont(font_green);
        style_yellow.setAlignment(HorizontalAlignment.LEFT);

        this.styleMap.put(HEADER, header);
        this.styleMap.put(STYLE_YELLOW, style_yellow);
    }

    private void reformatSheets(List<Sheet> sheets, int cols) {
        for (Sheet sheet : sheets) {
            for (int j = 0; j < cols; j++) {
                sheet.setColumnWidth(j, 8000);
            }
        }
    }

    @Override
    public void run() {
        try {
            long start = System.currentTimeMillis();
            
            // Handle Picocli parameters or fall back to legacy CommandContext
            String[] options;
            if (exportPath != null) {
                // Using Picocli parameters
                List<String> optionList = new ArrayList<>();
                optionList.add(exportPath.toString());
                if (language != null) {
                    optionList.add(language);
                }
                if (emptyProperties) {
                    if (language == null) {
                        optionList.add("1");
                    } else {
                        optionList.add("1");
                    }
                }
                options = optionList.toArray(new String[0]);
            } else {
                // Legacy support: use CommandContext
                options = CommandContext.getInstance().allArguments();
            }
            
            doExport(options);
            long end = System.currentTimeMillis() - start;
            logger.log(Level.INFO, "Export Excel file in ms: " + end);
        } catch (IOException | DatabaseException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    
    /**
     * Sets the export path programmatically for testing or legacy support.
     */
    public void setExportPath(Path exportPath) {
        this.exportPath = exportPath;
    }
    
    /**
     * Sets the language programmatically for testing or legacy support.
     */
    public void setLanguage(String language) {
        this.language = language;
    }
    
    /**
     * Sets the empty properties flag programmatically for testing or legacy support.
     */
    public void setEmptyProperties(boolean emptyProperties) {
        this.emptyProperties = emptyProperties;
    }

    private void doExport(String... options) throws IOException, DatabaseException {
        if (options == null || options.length == 0) {
            logger.warning("No command parameters was found!");
            return;
        }

        String exportPath = options[0];
        String language = null;
        boolean emptyProperties = false;

        if (StringUtils.isEmpty(exportPath)) {
            logger.severe("No export path found!");
            return;
        }

        if (options.length == 2) {
            if (options[1].equals("1")) {
                emptyProperties = true;
            } else {
                language = options[1];
            }
        } else if (options.length == 3) {
            language = options[1];
            emptyProperties = options[2].equals("1");
        }

        if (language != null) {
            fileName = HelperUtil.currentTimestamp() + "-export-" + language + ".xlsx";
        } else {
            fileName = HelperUtil.currentTimestamp() + "-export-all.xlsx";
        }
        
        Locale defaultLocale = Locale.ENGLISH;
        Set<String> fullPaths = locFacade.getFiles(defaultLocale, false);

        if (language != null) {
            defaultLocale = new Locale(configFacade.getDefaultLanguage());
        }

        if (fullPaths.isEmpty()) {
            logger.log(Level.SEVERE, "No localization files for the locale [" + defaultLocale.getLanguage() + "].");
        }
        exportPath += SystemUtils.IS_OS_WINDOWS ? "\\" : "/";
        int rowcount = ROWGAP_HEADER;

        Workbook wb = new XSSFWorkbook();
        initStyles(wb);
        Sheet sheet = wb.createSheet("SLC Properties");
        List<Sheet> sheets = new ArrayList<>();
        sheets.add(sheet);
        List<String> knownLanguages = locFacade.getLanguages(false);

        // create header row
        Row rowheader = sheets.get(0).createRow(rowcount++);
        List<String> headers = createHeaders(knownLanguages, language);

        int headercount = 0;
        for (String header : headers) {
            Cell cell = rowheader.createCell(headercount++);
            cell.setCellValue(header);
            cell.setCellStyle(styleMap.get(HEADER));
        }

        reformatSheets(sheets, headers.size());
        int lastVersion = locFacade.lastVersion(Status.SRC);
        List<Localization> allLocalizations = locFacade.getLocalizations(lastVersion, Status.SRC, false, null);

        for (String fullPath : fullPaths) {
            String defaultLocFullPath = defaultLocale.equals(Locale.ENGLISH)
                ? fullPath
                : HelperUtil.replaceLanguageFromPath(fullPath, defaultLocale.getLanguage());

            for (Localization savedDefLocalization : locFacade.getLocalizations(lastVersion, Status.SRC, false,
                    fullPath)) {
                int cell = 0;
                Row row = sheets.get(0).createRow(rowcount++);
                row.createCell(cell++).setCellValue(savedDefLocalization.getFileName());
                row.createCell(cell++).setCellValue(savedDefLocalization.getKey());
                if (StringUtils.isEmpty(language)) {
                    for (String lang : knownLanguages) {
                        if (lang.equals(Locale.ENGLISH.toString())) {
                            Cell valueCell = row.createCell(cell++);
                            if (savedDefLocalization.getValue().equals(EMPTY_VALUE)) {
                                valueCell.setCellStyle(styleMap.get(STYLE_YELLOW));
                            }
                            valueCell.setCellValue(savedDefLocalization.getValue());
                        } else {
                            Cell valueCell = row.createCell(cell++);
                            Localization localization = getLoc(allLocalizations,
                                    HelperUtil.replaceLanguageFromPath(savedDefLocalization.getFullPath(), lang),
                                    savedDefLocalization.getKey(), null);
                            if (localization.getValue().equals(EMPTY_VALUE)) {
                                valueCell.setCellStyle(styleMap.get(STYLE_YELLOW));
                            }
                            valueCell.setCellValue(localization.getValue());
                        }
                    }
                } else {
                    Cell valueCell = row.createCell(cell++);
                    if (!language.equalsIgnoreCase(defaultLocale.toString())) {
                        Localization localization = getLoc(
                            allLocalizations,
                            HelperUtil.replaceLanguageFromPath(fullPath, language),
                            savedDefLocalization.getKey(), 
                            language
                        );

                        Localization defaultLocalization = getLoc(
                            allLocalizations, 
                            defaultLocFullPath,
                            savedDefLocalization.getKey(), 
                            defaultLocale.getLanguage()
                        );

                        if (emptyProperties && !localization.getValue().equals(EMPTY_VALUE)) {
                            sheets.get(0).removeRow(row);
                            rowcount--;
                            continue;
                        }
                        if (localization.getValue().equals(EMPTY_VALUE)) {
                            valueCell.setCellStyle(styleMap.get(STYLE_YELLOW));
                            valueCell.setCellValue("[" + defaultLocale.getLanguage() + "]" + defaultLocalization.getValue());
                        } else {
                            valueCell.setCellValue(localization.getValue());
                        }

                    } else {
                        if (emptyProperties && !savedDefLocalization.getValue().equals(EMPTY_VALUE)) {
                            sheets.get(0).removeRow(row);
                            rowcount--;
                            continue;
                        }
                        if (savedDefLocalization.getValue().equals(EMPTY_VALUE)) {
                            valueCell.setCellStyle(styleMap.get(STYLE_YELLOW));
                        }
                        valueCell.setCellValue(savedDefLocalization.getValue());
                    }
                }
                row.createCell(cell++).setCellValue(fullPath);
            }
        }

        // write the sheet out ...
        FileOutputStream outStream = null;
        try {
            outStream = FileUtils.openOutputStream(new File(exportPath + fileName));
            wb.write(outStream);
        } catch (IOException e) {
            logger.severe(
                    "The excel file is already in use! Please close the export.xls and try again." + e.getMessage());
        } finally {
            if (outStream != null) {
                outStream.close();
            }
            wb.close();
        }
    }

    private List<String> createHeaders(List<String> knownLanguages, String language) {
        List<String> headers = new LinkedList<>();
        headers.add("Filename");
        headers.add("Key");
        if (StringUtils.isNotEmpty(language)) {
            headers.add("VALUE_" + language.toUpperCase());
        } else {
            for (String lang : knownLanguages) {
                headers.add("VALUE_" + lang.toUpperCase());
            }
        }
        headers.add("FullPath");
        return headers;
    }

    private Localization getLoc(List<Localization> allLocalizations, String fullPath, String key, String language) {
        Localization loc = searchLocation(allLocalizations, fullPath, key, language);
        if (loc == null) {
            loc = searchLocation(allLocalizations, HelperUtil.removeLanguageFromPath(fullPath), key, language);
            if (loc == null) {
                loc = new Localization();
                loc.setValue(EMPTY_VALUE);
                return loc;
            }
            loc.setKey(key);
            loc.setValue(EMPTY_VALUE);
            if (language != null) {
                loc.setLocale(language.toLowerCase());
            }
        }
        locFacade.detach(loc);
        return loc;
    }

    /**
     * @param allLocalizations
     * @param fullPath
     * @param key
     */
    private Localization searchLocation(List<Localization> allLocalizations, String fullPath, String key,
            String language) {
        for (Localization loc : allLocalizations) {
            if (StringUtils.isNotEmpty(language)) {
                if (loc.getFullPath().equals(fullPath) && loc.getKey() != null && loc.getKey().equals(key)
                        && loc.getLocale().equals(language)) {
                    return loc;
                }
            }
            if (loc.getFullPath().equals(fullPath) && loc.getKey() != null && loc.getKey().equals(key)
                    && StringUtils.isEmpty(language)) {
                return loc;
            }
        }
        return null;
    }
}
