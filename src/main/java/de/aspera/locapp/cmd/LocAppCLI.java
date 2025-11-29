package de.aspera.locapp.cmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * Root command for LocApp CLI using Picocli.
 * All subcommands are registered here for the interactive shell.
 */
@Command(
    name = "locapp",
    description = "LocApp - Localization Application for Properties Files",
    version = "2.0.1-SNAPSHOT",
    mixinStandardHelpOptions = true,
    subcommands = {
        HelpCommand.class,
        FilesCommand.class,
        ImportPropertiesCommand.class,
        ExcelExportCommand.class,
        ExcelImportCommand.class,
        ExportPropertiesCommand.class,
        ConfigInitCommand.class,
        ExportDeltaCommand.class,
        ImportDeltaCommand.class,
        PropertiesCounterCommand.class,
        MergeCommand.class,
        CheckIntegrityCommand.class,
        ClearLocalizationCommand.class,
        ImportIgnoredItemsCommand.class,
        ClearIgnoreListCommand.class,
        SetDefaultLanguageCommand.class,
        CSVImportCommand.class,
        SearchCommand.class,
        QuitCommand.class
    }
)
public class LocAppCLI implements Runnable {

    @Override
    public void run() {
        // When no subcommand is given, print help
        CommandLine.usage(this, System.out);
    }
}
