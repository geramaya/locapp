
package org.locapp.dao;

import java.util.logging.Logger;

import org.locapp.cmd.CommandContext;
import org.locapp.util.Resources;

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
    public static final CommandContext CMDCTX = CommandContext.getInstance();
    protected final Logger logger = Logger.getLogger(getLoggerClass().getName());

    public abstract Class<?> getLoggerClass();
}
