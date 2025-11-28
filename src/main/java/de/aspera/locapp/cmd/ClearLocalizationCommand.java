package de.aspera.locapp.cmd;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.LocalizationDao;

import picocli.CommandLine.Command;

@Command(
    name = "clear-loc",
    aliases = {"cl"},
    description = "Delete all(!) entries for Localization!",
    mixinStandardHelpOptions = true
)
public class ClearLocalizationCommand implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClearLocalizationCommand.class.getName());

    @Override
    public void run() {
        try {
            new LocalizationDao().removeAll();
            LOGGER.log(Level.INFO, "All Localization entries were deleted!");
        } catch (DatabaseException e) {
            LOGGER.severe(e.getMessage());
        }
    }
}
