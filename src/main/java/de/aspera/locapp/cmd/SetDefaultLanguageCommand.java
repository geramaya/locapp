package de.aspera.locapp.cmd;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.aspera.locapp.dao.ConfigDao;
import de.aspera.locapp.dao.DatabaseException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "set-default-language",
    aliases = {"sdl"},
    description = "Set default language for excel export (LANG=[en,de...]).",
    mixinStandardHelpOptions = true
)
public class SetDefaultLanguageCommand implements CommandRunnable, Runnable {
    private ConfigDao configFacade = new ConfigDao();
    private Logger logger = Logger.getLogger(SetDefaultLanguageCommand.class.getName());

    @Parameters(index = "0", description = "Language ISO code (e.g., de, en, fr)", arity = "0..1")
    private String languageParam;

    @Override
    public void run() {
        try {
            setDefaultLanguage();
        } catch (DatabaseException | CommandException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    
    /**
     * Sets the language parameter programmatically for testing or legacy support.
     */
    public void setLanguageParam(String languageParam) {
        this.languageParam = languageParam;
    }

    private void setDefaultLanguage() 
        throws CommandException, DatabaseException {
        String selectedLanguage;
        if (languageParam != null) {
            selectedLanguage = languageParam;
        } else {
            selectedLanguage = CommandContext.getInstance().nextArgument();
        }
        
		var language = selectedLanguage != null ? selectedLanguage.toLowerCase()
				: ConfigDao.DEFAULT_LANGUAGE.toLowerCase();
		
		Locale locale;
		try {
			locale = new Locale(language);
			locale.getISO3Language();
		} catch (MissingResourceException e) {
			logger.log(Level.SEVERE, "Language [" + language + "] is invalid. [" + ConfigDao.DEFAULT_LANGUAGE.toLowerCase() + "] was selected as default language!");
			locale = new Locale(ConfigDao.DEFAULT_LANGUAGE.toLowerCase());
		}
        configFacade.setDefaultLanguage(locale);
        logger.log(Level.INFO, "The configured default language is [" + locale.getLanguage() + "]");
    }
}
