/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.aspera.locapp.dao;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import de.aspera.locapp.util.Resources;

/**
 * Der H2DatabaseManager ist ein Singleton zur Verwendung der lokalen JPA
 * Datenbankverbindung.
 *
 * @author daniel
 */

public class H2DatabaseManager {

    private static H2DatabaseManager instance;
    private static EntityManager theManager;
    private static final Map<String, String> databaseProperties = new HashMap<>();

    private H2DatabaseManager() {
    }

    public static H2DatabaseManager getInstance() {
        if (instance == null) {
            instance = new H2DatabaseManager();
            try {
                instance.init();
            } catch (Exception e) {
                instance = null; // Reset instance on failure
                throw e;
            }
        }
        return instance;
    }

    /**
     * Voreinstellung f�r die Datenbank.
     */
    private void init() {

        Object dbname = System.getProperties().get("DBNAME");
        Object dbaction = System.getProperties().get("DBACTION");

        if (dbaction == null || "".equals(dbaction)) {
            dbaction = "create"; // "drop-and-create";
        }

        // AUTO_SERVER=TRUE erm�glicht den Zugriff f�r mehrere Apps auf die
        // gleich Datenbank!
        databaseProperties.put("jakarta.persistence.jdbc.url",
                "jdbc:h2:~/." + Resources.PROJECT_NAME + "/"
                        + (dbname != null ? dbname.toString() : Resources.getInstance().getProperty("db-name"))
                        + ";AUTO_SERVER=TRUE;NON_KEYWORDS=KEY,VALUE");
        databaseProperties.put("jakarta.persistence.jdbc.user", Resources.getInstance().getProperty("db-user"));
        databaseProperties.put("jakarta.persistence.jdbc.password", Resources.getInstance().getProperty("db-password"));

        databaseProperties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        databaseProperties.put("jakarta.persistence.schema-generation.database.action", dbaction.toString());
        // databaseProperties.put("eclipselink.logging.level", "WARNING");
        // databaseProperties.put("eclipselink.logging.parameters", "true");

        try {
            EntityManagerFactory factory = Persistence.createEntityManagerFactory("h2locapp", databaseProperties);
            theManager = factory.createEntityManager();
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Unsupported database file version")) {
                throw new RuntimeException(
                    "H2 database file is incompatible with H2 2.x. " +
                    "Please delete the old database files in ~/.locapp/ or backup and migrate your data. " +
                    "Database location: " + databaseProperties.get("jakarta.persistence.jdbc.url"), e);
            }
            throw new RuntimeException("Failed to initialize H2 database: " + errorMsg, e);
        }
    }

    public EntityManager getEntityManager() {
        return H2DatabaseManager.theManager;
    }

}
