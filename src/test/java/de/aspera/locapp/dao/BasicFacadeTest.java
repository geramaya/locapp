
package de.aspera.locapp.dao;

import java.util.logging.Logger;

import de.aspera.locapp.cmd.LocAppCLI;
import de.aspera.locapp.util.Resources;
import picocli.CommandLine;

/**
 * The test creates a test database in h2 for JUnit tests.
 *
 * @author daniel
 */
public abstract class BasicFacadeTest {
    static {
        Resources.getInstance();
        System.getProperties().put("DBNAME", "test-database");
        System.getProperties().put("DBACTION", "drop-and-create");
        // System.getProperties().put("DBACTION", "create");
    }
    
    /**
     * Picocli CommandLine for executing commands in tests.
     */
    public static final CommandLine PICOCLI = new CommandLine(new LocAppCLI());
    
    protected final Logger logger = Logger.getLogger(getLoggerClass().getName());

    public abstract Class<?> getLoggerClass();
    
    /**
     * Execute a command using Picocli.
     * @param args the command and its arguments
     * @return the exit code
     */
    protected static int executeCommand(String... args) {
        return PICOCLI.execute(args);
    }
}
