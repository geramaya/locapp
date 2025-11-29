package de.aspera.locapp.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Search command for finding translations across keys and values.
 * Displays results in a dynamic, multilingual ASCII table with highlighted matches.
 */
@Command(
    name = "search",
    aliases = {"s", "find"},
    description = "Search for translations by key or value.",
    mixinStandardHelpOptions = true
)
public class SearchCommand implements Runnable {

    private static final Logger logger = Logger.getLogger(SearchCommand.class.getName());
    private static final int DEFAULT_PAGE_SIZE = 50;
    
    // ANSI color codes for highlighting
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED_BOLD = "\u001B[1;31m";
    
    private LocalizationDao localizationDao = new LocalizationDao();

    @Parameters(index = "0", arity = "0..1", description = "Search term to find in keys and values")
    private String query;

    @Option(names = {"-a", "--all"}, description = "Show all results without paging confirmation", defaultValue = "false")
    private boolean showAll;

    @Override
    public void run() {
        try {
            String searchTerm = query;

            // Interactive mode: prompt user if query is null
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                searchTerm = promptForSearchTerm();
            }

            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                System.out.println("No search term provided.");
                return;
            }

            performSearch(searchTerm.trim());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Prompt user for search term using JLine LineReader.
     */
    private String promptForSearchTerm() {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            return reader.readLine("Enter search term: ");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not create interactive prompt", e);
            return null;
        }
    }

    /**
     * Perform the search and display results.
     */
    private void performSearch(String searchTerm) throws DatabaseException {
        List<Localization> results = localizationDao.searchLocalizations(searchTerm);

        if (results.isEmpty()) {
            System.out.println("No results found for: " + searchTerm);
            return;
        }

        // Group results by fileName + key (pivot)
        Map<String, Map<String, String>> groupedResults = groupResults(results);
        Set<String> allLocales = collectAllLocales(results);

        int totalRows = groupedResults.size();
        System.out.println("\nFound " + totalRows + " matching key(s) for: \"" + searchTerm + "\"");

        // Handle paging for large result sets
        if (totalRows > DEFAULT_PAGE_SIZE && !showAll) {
            if (!confirmShowAll(totalRows)) {
                System.out.println("Search cancelled. Use -a or --all to show all results.");
                return;
            }
        }

        // Render ASCII table
        renderAsciiTable(groupedResults, allLocales, searchTerm);
    }

    /**
     * Group localization results by fileName + key.
     * Returns Map<UniqueKey, Map<Locale, Value>>
     */
    private Map<String, Map<String, String>> groupResults(List<Localization> results) {
        Map<String, Map<String, String>> grouped = new LinkedHashMap<>();

        for (Localization loc : results) {
            String uniqueKey = loc.getFileName() + "|" + loc.getKey();
            grouped.computeIfAbsent(uniqueKey, k -> new LinkedHashMap<>());
            String locale = loc.getLocale() != null ? loc.getLocale().toUpperCase() : "-";
            String value = loc.getValue() != null ? loc.getValue() : "";
            grouped.get(uniqueKey).put(locale, value);
        }

        return grouped;
    }

    /**
     * Collect all unique locales from results.
     */
    private Set<String> collectAllLocales(List<Localization> results) {
        Set<String> locales = new LinkedHashSet<>();
        for (Localization loc : results) {
            String locale = loc.getLocale() != null ? loc.getLocale().toUpperCase() : "-";
            locales.add(locale);
        }
        return locales;
    }

    /**
     * Prompt user to confirm showing all results.
     */
    private boolean confirmShowAll(int totalRows) {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            String response = reader.readLine("Show all " + totalRows + " results? (y/n): ");
            return response != null && (response.trim().equalsIgnoreCase("y") || response.trim().equalsIgnoreCase("yes"));
        } catch (IOException e) {
            // Default to showing all if prompt fails
            return true;
        }
    }

    /**
     * Render the results as an ASCII table with dynamic columns.
     */
    private void renderAsciiTable(Map<String, Map<String, String>> groupedResults, 
                                   Set<String> locales, String searchTerm) {
        List<String> localeList = new ArrayList<>(locales);
        
        // Calculate column widths
        int fileNameWidth = "Filename".length();
        int keyWidth = "Key".length();
        Map<String, Integer> localeWidths = new LinkedHashMap<>();
        
        for (String locale : localeList) {
            localeWidths.put(locale, locale.length());
        }

        // First pass: calculate maximum widths
        for (Map.Entry<String, Map<String, String>> entry : groupedResults.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            String fileName = parts[0];
            String key = parts.length > 1 ? parts[1] : "";
            
            fileNameWidth = Math.max(fileNameWidth, fileName.length());
            keyWidth = Math.max(keyWidth, key.length());
            
            Map<String, String> localeValues = entry.getValue();
            for (String locale : localeList) {
                String value = localeValues.getOrDefault(locale, "-");
                // Strip ANSI codes for width calculation
                int valueLength = value.length();
                localeWidths.put(locale, Math.max(localeWidths.get(locale), Math.min(valueLength, 40)));
            }
        }

        // Limit column widths for readability
        fileNameWidth = Math.min(fileNameWidth, 30);
        keyWidth = Math.min(keyWidth, 40);
        for (String locale : localeList) {
            localeWidths.put(locale, Math.min(localeWidths.get(locale), 40));
        }

        // Build the table
        StringBuilder separator = buildSeparator(fileNameWidth, keyWidth, localeWidths, localeList);
        
        // Print header
        System.out.println(separator);
        System.out.print("| " + padRight("Filename", fileNameWidth) + " | " + padRight("Key", keyWidth) + " |");
        for (String locale : localeList) {
            System.out.print(" " + padRight(locale, localeWidths.get(locale)) + " |");
        }
        System.out.println();
        System.out.println(separator);

        // Print data rows
        for (Map.Entry<String, Map<String, String>> entry : groupedResults.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            String fileName = truncate(parts[0], fileNameWidth);
            String key = parts.length > 1 ? truncate(parts[1], keyWidth) : "";
            
            // Highlight search term in fileName and key
            String highlightedFileName = highlightMatch(fileName, searchTerm);
            String highlightedKey = highlightMatch(key, searchTerm);
            
            System.out.print("| " + padRightWithAnsi(highlightedFileName, fileNameWidth) + " | " 
                    + padRightWithAnsi(highlightedKey, keyWidth) + " |");
            
            Map<String, String> localeValues = entry.getValue();
            for (String locale : localeList) {
                String value = localeValues.getOrDefault(locale, "-");
                value = truncate(value, localeWidths.get(locale));
                String highlightedValue = highlightMatch(value, searchTerm);
                System.out.print(" " + padRightWithAnsi(highlightedValue, localeWidths.get(locale)) + " |");
            }
            System.out.println();
        }

        System.out.println(separator);
    }

    /**
     * Build the separator line for the table.
     */
    private StringBuilder buildSeparator(int fileNameWidth, int keyWidth, 
                                         Map<String, Integer> localeWidths, List<String> localeList) {
        StringBuilder separator = new StringBuilder();
        separator.append("+").append("-".repeat(fileNameWidth + 2));
        separator.append("+").append("-".repeat(keyWidth + 2));
        for (String locale : localeList) {
            separator.append("+").append("-".repeat(localeWidths.get(locale) + 2));
        }
        separator.append("+");
        return separator;
    }

    /**
     * Highlight the search term in the text with ANSI colors.
     */
    private String highlightMatch(String text, String searchTerm) {
        if (text == null || searchTerm == null || text.isEmpty() || searchTerm.isEmpty()) {
            return text;
        }
        
        String lowerText = text.toLowerCase();
        String lowerSearch = searchTerm.toLowerCase();
        
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        int index;
        
        while ((index = lowerText.indexOf(lowerSearch, lastEnd)) != -1) {
            result.append(text, lastEnd, index);
            result.append(ANSI_RED_BOLD);
            result.append(text, index, index + searchTerm.length());
            result.append(ANSI_RESET);
            lastEnd = index + searchTerm.length();
        }
        result.append(text.substring(lastEnd));
        
        return result.toString();
    }

    /**
     * Pad string to the right with spaces.
     */
    private String padRight(String text, int width) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= width) {
            return text;
        }
        return text + " ".repeat(width - text.length());
    }

    /**
     * Pad string to the right, accounting for ANSI escape codes.
     */
    private String padRightWithAnsi(String text, int width) {
        if (text == null) {
            text = "";
        }
        // Calculate visible length (without ANSI codes)
        String stripped = text.replaceAll("\u001B\\[[;\\d]*m", "");
        int visibleLength = stripped.length();
        
        if (visibleLength >= width) {
            return text;
        }
        return text + " ".repeat(width - visibleLength);
    }

    /**
     * Truncate text to a maximum width.
     */
    private String truncate(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxWidth) {
            return text;
        }
        return text.substring(0, maxWidth - 3) + "...";
    }

    /**
     * Sets the query programmatically for testing.
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Sets the showAll flag programmatically for testing.
     */
    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
    }

    /**
     * Sets the LocalizationDao for testing.
     */
    public void setLocalizationDao(LocalizationDao localizationDao) {
        this.localizationDao = localizationDao;
    }
}
