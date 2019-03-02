package org.locapp.cmd;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.locapp.dao.DatabaseException;
import org.locapp.dao.LocalizationFacade;

public class ClearLocalizationCommand implements CommandRunnable {

    private static final Logger LOGGER = Logger.getLogger(ClearLocalizationCommand.class.getName());

    @Override
    public void run() {
        try {
            new LocalizationFacade().removeAll();
            LOGGER.log(Level.INFO, "All Localization entries were deleted!");
        } catch (DatabaseException e) {
            LOGGER.severe(e.getMessage());
        }
    }
}
