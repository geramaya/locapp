package de.aspera.locapp.cmd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;
import de.aspera.locapp.util.HelperUtil;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This class export all property files into a directory.
 *
 * @author Daniel.Weiss
 *
 */
@Command(
    name = "export-properties",
    aliases = {"ep"},
    description = "Iterate known properties and save into directory. Use --delta to export only modified files.",
    mixinStandardHelpOptions = true
)
public class ExportPropertiesCommand implements Runnable {

	public static final String EMPTY_VALUE = "";
	private static final Logger logger = Logger.getLogger(ExportPropertiesCommand.class.getName());
	private LocalizationDao locFacade = new LocalizationDao();
	private List<Localization> allLocalizations;

	@Parameters(index = "0", description = "Export directory path", arity = "0..1")
	private Path exportDir;

	@Option(names = {"-d", "--delta"}, description = "Export only files containing keys modified since the previous SRC version.")
	private boolean deltaExport;

	@Override
	public void run() {
		try {
			exportPropertiesFiles();
		} catch (CommandException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	/**
	 * Sets the export directory programmatically for testing or legacy support.
	 */
	public void setExportDir(Path exportDir) {
		this.exportDir = exportDir;
	}

	/**
	 * Sets the delta export flag programmatically for testing.
	 */
	public void setDeltaExport(boolean deltaExport) {
		this.deltaExport = deltaExport;
	}

	private void exportPropertiesFiles() throws CommandException {
		long start = System.currentTimeMillis();
		String exportPath;
		if (exportDir != null) {
			exportPath = exportDir.toString();
		} else {
			logger.warning("No export path provided. Use: export-properties <path>");
			return;
		}

		if (StringUtils.isEmpty(exportPath)) {
			logger.warning("No export path found! Please define a export path.");
			return;
		}
		
		// Ensure exportPath ends with a path separator (consistent with ExcelExportCommand)
		if (!exportPath.endsWith("/") && !exportPath.endsWith("\\")) {
			exportPath += SystemUtils.IS_OS_WINDOWS ? "\\" : "/";
		}

		OutputStream outStream = null;
		InputStream inputStream = null;

		try {
			allLocalizations = locFacade.getLocalizations(locFacade.lastVersion(Status.XLS), Status.XLS, false, null);
			Set<String> defaultPathFiles = locFacade.getFiles(Locale.ENGLISH, true);
			List<String> languages = locFacade.getLanguages(true);

			// For delta export, determine which files contain modified keys
			Set<String> modifiedFiles = new HashSet<>();
			if (deltaExport) {
				modifiedFiles = getFilesWithModifiedKeys();
				if (modifiedFiles.isEmpty()) {
					logger.info("Delta export: No files have modified keys. Nothing to export.");
					return;
				}
				logger.info("Delta export: " + modifiedFiles.size() + " files contain modified keys.");
			}

			int filesExported = 0;
			for (String defaultPathFile : defaultPathFiles) {
				for (String local : languages) {
					if (skipPropertyFile(defaultPathFile, languages)) {
						continue; // skip unnecessary languages and files
					}
					String replacedFile = replaceFilePathWithLocale(defaultPathFile, local);

					// Delta export: skip files that were not modified in XLS compared to SRC
					if (deltaExport && !modifiedFiles.contains(replacedFile)) {
						continue;
					}

					File exportPropertyFile = new File(exportPath + replacedFile);
					Properties prop = new Properties() {
						private static final long serialVersionUID = 7103264221960600113L;

						// this sort the keys of a property file.
						@Override
						public synchronized Enumeration<Object> keys() {
							return Collections.enumeration(new TreeSet<Object>(super.keySet()));
						}
					};

					if (exportPropertyFile.exists()) {
						inputStream = FileUtils.openInputStream(exportPropertyFile);
						if (inputStream != null) {
							prop.load(inputStream);
						}
					}

					String locFilename = null;
					for (Localization loc : getLocalization(replacedFile)) {
						logger.fine("save -> file: " + replacedFile + " >> key: " + loc.getKey() + " ; value: "
								+ loc.getValue());
						if (prop.containsKey(loc.getKey())) {
							if (!prop.get(loc.getKey()).equals(loc.getValue())) {
								prop.setProperty(loc.getKey(), loc.getValue());
							}
						} else {
							prop.put(loc.getKey(), loc.getValue());
						}
						locFilename = loc.getFileName();
					}
					outStream = FileUtils.openOutputStream(exportPropertyFile);
					if (prop.size() >= 1) {
						prop.store(outStream, "SLC property file " + locFilename != null ? locFilename : "");
					} else {
						outStream.close();
						FileUtils.forceDelete(exportPropertyFile);
					}
					
					// close file handles
					if (outStream != null)
						outStream.close();
					
					if (inputStream != null)
						inputStream.close();
				}
			}
		} catch (IOException | DatabaseException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {

			try {
				if (outStream != null)
					outStream.close();
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				throw new CommandException(e.getMessage(), e);
			}
		}

		long end = System.currentTimeMillis() - start;
		logger.log(Level.INFO, "Export properties fileset into a directory [" + exportPath + "] in ms: " + end);
	}

	private boolean skipPropertyFile(String defaultFilePath, List<String> languages) {
		for (Localization loc : allLocalizations) {
			if (HelperUtil.removeLanguageFromPath(loc.getFullPath()).equals(defaultFilePath)
					&& !languages.contains(loc.getLocale())) {
				return true; // skip unnecessary languages
			}
		}
		return false;
	}

	private String replaceFilePathWithLocale(String myfile, String local) {
		if (!local.contains(Locale.ENGLISH.toString())) {
			return HelperUtil.replaceLanguageFromPath(myfile, local);
		}
		return myfile;
	}

	private List<Localization> getLocalization(String filePath) {
		List<Localization> locs = new ArrayList<>();
		for (Localization loc : allLocalizations) {
			if (loc.getFullPath().equals(filePath)) {
				locs.add(loc);
			}
		}
		return locs;
	}

	/**
	 * Gets the set of files that have been modified in XLS compared to SRC.
	 * Returns the full file paths (with language suffixes) so that only the specific
	 * language-variant files that were actually modified will be exported.
	 */
	private Set<String> getFilesWithModifiedKeys() throws DatabaseException {
		int srcVersion = locFacade.lastVersion(Status.SRC);
		int xlsVersion = locFacade.lastVersion(Status.XLS);
		
		if (xlsVersion == 0) {
			logger.warning("No XLS version found for delta comparison.");
			return new HashSet<>();
		}
		
		logger.info("Delta comparison: SRC v" + srcVersion + " -> XLS v" + xlsVersion);
		
		List<Localization> differences = locFacade.getXlsToSrcDifferences(srcVersion, xlsVersion);
		
		// Collect the full file paths (with language suffix) of modified files
		Set<String> modifiedFiles = new HashSet<>();
		for (Localization loc : differences) {
			modifiedFiles.add(loc.getFullPath());
		}
		
		return modifiedFiles;
	}
}
