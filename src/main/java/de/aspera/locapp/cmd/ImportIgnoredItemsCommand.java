package de.aspera.locapp.cmd;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.IgnoredItemDao;
import de.aspera.locapp.dto.IgnoredItem;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "import-ignore-list",
    aliases = {"iil"},
    description = "Import list of files that are to be excluded from the translation process.",
    mixinStandardHelpOptions = true
)
public class ImportIgnoredItemsCommand implements Runnable {
	private static final Logger logger = Logger.getLogger(ImportIgnoredItemsCommand.class.getName());

	private IgnoredItemDao ignoredItemFacade = new IgnoredItemDao();

	@Parameters(index = "0", description = "Path to the ignore list file", arity = "0..1")
	private Path ignoreListFile;

	@Override
	public void run() {
		String ignoredItemsListPath;
		if (ignoreListFile != null) {
			ignoredItemsListPath = ignoreListFile.toString();
		} else {
			logger.warning("No ignore list file provided. Use: import-ignore-list <path>");
			return;
		}
		
		var ignoredItems = readIgnoredItemsFile(ignoredItemsListPath);
		try {
			for (var ignoredItem : ignoredItems) {
				ignoredItemFacade.create(ignoredItem);
				System.out.println(" >> Add ignore entry: " + ignoredItem.getFileName() + " to be ignored on export. <<");
			}
			if (ignoredItems != null && ignoredItems.size() >= 0)
				logger.log(Level.INFO, "Marked " + ignoredItems.size() + " files to be ignored.");
		} catch (DatabaseException e) {
			logger.log(Level.SEVERE, "Error while saving IgnoredItem entity.", e);
			return;
		}
	}
	
	/**
	 * Sets the ignore list file programmatically for testing or legacy support.
	 */
	public void setIgnoreListFile(Path ignoreListFile) {
		this.ignoreListFile = ignoreListFile;
	}

	private List<IgnoredItem> readIgnoredItemsFile(String filePath) {
		List<IgnoredItem> ignoredItems = new ArrayList<>();
		final Set<String> linesOfFile;
		try {
			linesOfFile = new HashSet<>(FileUtils.readLines(new File(filePath)));
		} catch (Exception e) {
			logger.log(Level.WARNING, "File " + filePath + " could not found or was invalid!.");
			return ignoredItems;
		}
		
		Set<String> savedIgnoreFiles = getSavedIgnoreFiles();
		for (Iterator<String> iterator = linesOfFile.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			if (savedIgnoreFiles.contains(string))
				continue;
			var item = new IgnoredItem();
			item.setFileName(string);
			ignoredItems.add(item);
		}
		return ignoredItems;
	}

	private Set<String> getSavedIgnoreFiles() {
		List<IgnoredItem> ignored = ignoredItemFacade.findAll();
		Set<String> knownIgnoreFiles = new HashSet<>();
		for (IgnoredItem ignoreItem : ignored) {
			knownIgnoreFiles.add(ignoreItem.getFileName());
		}
		return knownIgnoreFiles;
	}
}
