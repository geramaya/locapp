package de.aspera.locapp.cmd;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

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

public class CSVImportCommand implements CommandRunnable {

	private static final Logger logger = Logger.getLogger(CSVImportCommand.class.getName());
	private LocalizationDao locFacade = new LocalizationDao();

	@Override
	public void run() throws CommandException {
		long start = System.currentTimeMillis();
		importCSV();
		long end = System.currentTimeMillis() - start;
		logger.log(Level.INFO, "Import CSV file in ms: " + end);
	}

	private void importCSV() throws CommandException {
		String importPath = CommandContext.getInstance().nextArgument();
		if (StringUtils.isEmpty(importPath) || !FileUtils.getFile(importPath).exists()) {
			logger.severe("No csv file found to import! Please define the full path to csv import file.");
			return;
		}

		try (FileReader fileReader = new FileReader(importPath);
				CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT)) {

			List<Localization> localizations = new ArrayList<>();
			int lastVersion = locFacade.lastVersion(Status.CSV) + 1;

			int valueColumnCount = 0;
			int fullPathColumnIndex = -1;
			List<String> headers = new ArrayList<>();
			for (CSVRecord csvRecord : csvParser.getRecords()) {
				if (csvRecord.getRecordNumber() == 1) {
					headers = csvRecord.toList();
					for (int i = 2; i < csvRecord.size(); i++) {
						String columnName = csvRecord.get(i);
						if (columnName.trim().substring(0, 6).equals("VALUE_")) {
							valueColumnCount++;
						} else if (columnName.trim().equals("FULLPATH")) {
							fullPathColumnIndex = i;
						}
					}
					continue;
				}
				String filename = csvRecord.get(0);
				String propertyKey = csvRecord.get(1);
				String fullPath = csvRecord.get(fullPathColumnIndex);
				for (int i = 2; i < 2 + valueColumnCount; i++) {
					Localization loc = new Localization();
					loc.setCreationDate(new Date());
					loc.setStatus(Localization.Status.CSV);
					loc.setVersion(lastVersion);
					loc.setFileName(filename);
					loc.setKey(propertyKey);
					String columnValue = csvRecord.get(i);
					String locale = headers.get(i).substring(6, 8).toLowerCase(); // get language
					loc.setLocale(locale);
					loc.setFullPath(locale.equals(Locale.ENGLISH.toString()) ? fullPath
							: HelperUtil.replaceFullPath(fullPath, locale));
					loc.setValue(columnValue);
					localizations.add(loc);
				}
			}
			locFacade.saveLocalizations(localizations);

		} catch (IOException | DatabaseException | CommandException e) {
			throw new CommandException(e.getMessage(), e);
		}
	}

}
