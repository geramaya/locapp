package de.aspera.locapp.cmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import de.aspera.locapp.dto.Localization;
import de.aspera.locapp.util.HelperUtil;

public final class FailedLocalization {

	public static void createFailedLocalizationsCSV(List<Localization> localizations) throws CommandException {

		if (localizations == null || localizations.size() <= 0) {
			throw new CommandException("No localizations was found!");
		}

		String currentDir = System.getProperty("user.dir");
		String errorCSV = currentDir + File.separator + "localization_failed_" + HelperUtil.currentTimestamp() + ".csv";

		try (CSVPrinter printer = new CSVPrinter(new FileWriter(errorCSV), CSVFormat.DEFAULT)) {
			// write header
			printer.printRecord("FILENAME, PROPERTY_KEY,VALUE,LOCALE,FULLPATH");
			for (Localization loc : localizations) {
				printer.printRecord(loc.getFileName(), loc.getKey(), loc.getValue(), loc.getLocale(),
						loc.getFullPath());
			}
			printer.flush();
		} catch (IOException e) {
			throw new CommandException(e.getMessage(), e);
		}
	}

}
