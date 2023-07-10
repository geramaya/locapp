package de.aspera.locapp.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;
import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.dto.Localization.Status;
import de.aspera.locapp.util.HelperUtil;

public class CSCMigrationCommand implements CommandRunnable {

	private static final Logger logger = Logger.getLogger(CSCMigrationCommand.class.getName());
	private LocalizationDao locFacade = new LocalizationDao();

	@Override
	public void run() throws CommandException {
		try {
			long start = System.currentTimeMillis();
			importCSV();
			long end = System.currentTimeMillis() - start;
			logger.log(Level.INFO, "Import Excel file in ms: " + end);
		} catch (Exception exp) {
			throw new CommandException(exp.getMessage(), exp);
		}

	}

	private void importCSV() throws DatabaseException {
		String importPath = CommandContext.getInstance().nextArgument();
		if (StringUtils.isEmpty(importPath) || !FileUtils.getFile(importPath).exists()) {
			logger.severe("No csv file found to import! Please define the full path to csv import file.");
			return;
		}

		List<Localization> localizations = locFacade.getLocalizationsWithLastVersion(locFacade.lastVersion(Status.SRC));
		List<Localization> mergedLocals = new ArrayList<>();

		Map<String, CSVRecord> uniqueFileKeyEntries = new HashedMap<>();
		Map<String, CSVRecord> duplicateFileKeyEntries = new HashedMap<>();
		
		int lastVersion = locFacade.lastVersion(Status.CSV) + 1;


		try (CSVParser parser = CSVParser.parse(new File(importPath), Charset.defaultCharset(), CSVFormat.DEFAULT)) {
			for (CSVRecord record : parser.getRecords()) {
				if (record.getRecordNumber() <= 1) {
					/*
						--- HEADER ----
						#1 FILENAME
						#2 PROPERTY_KEY
						#3 VALUE_DE
						#4 VALUE_EN
						#5 OLD_EN
					*/
					System.out.println("--- HEADER ----");
					System.out.println("#1 " + record.get(0));
					System.out.println("#2 " + record.get(1));
					System.out.println("#3 " + record.get(2));
					System.out.println("#4 " + record.get(3));
					System.out.println("#5 " + record.get(4));
					continue;
				}
				StringBuilder sb = new StringBuilder();
				sb.append(record.get(0)).append("\t");
				sb.append(record.get(1)).append("\t");
				sb.append(record.get(2)).append("\t");
				sb.append(record.get(3)).append("\t");
				sb.append(record.get(4));
				// key = filename + property
				String key = record.get(0) + "||" + record.get(1);
				if (!uniqueFileKeyEntries.containsKey(key)) {
					uniqueFileKeyEntries.put(key, record);
				} else {
					duplicateFileKeyEntries.put(key, record);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Set<String> keys = uniqueFileKeyEntries.keySet();
		keys.parallelStream()
		    .map(key -> key.split("\\|\\|"))
		    .forEach(parts -> {
		        String filename = parts[0];
		        String propertyKey = parts[1];
		        
		        localizations.parallelStream()
		            .filter(loc -> (loc.getFileName().equals(filename) || loc.getFileName().equals(filename.replace(".properties", "_de.properties"))) && loc.getKey().equals(propertyKey))
		            .forEach(savedLocalization -> {
		            	String valueDE = uniqueFileKeyEntries.get(filename + "||" + propertyKey).get(2);
		            	String valueEN = uniqueFileKeyEntries.get(filename + "||" + propertyKey).get(3);
		            	// DE Localization
		            	if (savedLocalization.getLocale().equals("de")) {
			            	mergedLocals.add(createLocalization(uniqueFileKeyEntries, lastVersion, filename,
									propertyKey, savedLocalization, valueDE, "de"));
		            	}
		            	if (savedLocalization.getLocale().equals("en")) {
		            	// EN Localization
			            	mergedLocals.add(createLocalization(uniqueFileKeyEntries, lastVersion, filename,
									propertyKey, savedLocalization, valueEN, "en"));
		            	}
		            });
		    });
		// locFacade.saveLocalizations(locs);
		System.out.println(mergedLocals.size());
	}

	private Localization createLocalization(Map<String, CSVRecord> uniqueFileKeyEntries, int lastVersion,
			String filename, String propertyKey, Localization loc, String value, String locale) {
		Localization newLocale = new Localization();
		newLocale.setCreationDate(new Date());
		newLocale.setStatus(Localization.Status.CSV);
		newLocale.setVersion(lastVersion);
		newLocale.setFileName(filename);
		newLocale.setKey(propertyKey);
		newLocale.setLocale(locale);
		newLocale.setFullPath(locale.equals(Locale.ENGLISH.toString()) ? loc.getFullPath()
				: HelperUtil.replaceFullPath(loc.getFullPath(), locale));
		newLocale.setValue(value);
		return newLocale;
	}

}
