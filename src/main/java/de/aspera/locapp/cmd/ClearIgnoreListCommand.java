package de.aspera.locapp.cmd;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.aspera.locapp.dao.DatabaseException;
import de.aspera.locapp.dao.IgnoredItemDao;

import picocli.CommandLine.Command;

@Command(
    name = "clear-ignore-list",
    aliases = {"cil"},
    description = "Clear list with ignored files.",
    mixinStandardHelpOptions = true
)
public class ClearIgnoreListCommand implements CommandRunnable, Runnable {
    private static final Logger logger = Logger.getLogger(ClearIgnoreListCommand.class.getName());

    @Override
    public void run() {
        try {
            new IgnoredItemDao().removeAll();
            logger.log(Level.INFO, "All IgnoredItem entries were deleted!");
        } catch (DatabaseException e) {
            logger.severe(e.getMessage());
        }
    }
}
