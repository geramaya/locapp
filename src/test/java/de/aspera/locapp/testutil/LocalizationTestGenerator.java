package de.aspera.locapp.testutil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Utility class for generating realistic test localization data for integration tests.
 * Generates properties files with 100 unique, realistic localization keys across
 * multiple languages.
 */
public class LocalizationTestGenerator {

    /**
     * The fixed key used for testing edit/save functionality.
     */
    public static final String FIXED_KEY = "button.submit";

    /**
     * The original English value for the fixed key.
     */
    public static final String ORIGINAL_EN_VALUE = "Submit";

    /**
     * The original German value for the fixed key.
     */
    public static final String ORIGINAL_DE_VALUE = "Absenden";

    /**
     * The original French value for the fixed key.
     */
    public static final String ORIGINAL_FR_VALUE = "Soumettre";

    private static final String NAMESPACE = "app";

    /**
     * Generates realistic properties files for testing the full roundtrip process.
     * Creates a subdirectory 'testdata_roundtrip' inside targetDir and populates it
     * with four properties files (app.properties, app_de.properties, app_en.properties,
     * app_fr.properties) containing 100 unique, realistic localization keys.
     *
     * @param targetDir the directory where the test data will be created
     * @return the path to the generated properties directory (testdata_roundtrip)
     * @throws IOException if file creation fails
     */
    public static Path generateRealisticPropertiesFiles(Path targetDir) throws IOException {
        Path testDataDir = targetDir.resolve("testdata_roundtrip");
        Files.createDirectories(testDataDir);

        // Generate properties for each language
        Properties enProps = generateEnglishProperties();
        Properties deProps = generateGermanProperties();
        Properties frProps = generateFrenchProperties();
        Properties defaultProps = new Properties();
        defaultProps.putAll(enProps); // Default is same as English

        // Write properties files
        writeProperties(testDataDir.resolve(NAMESPACE + ".properties"), defaultProps);
        writeProperties(testDataDir.resolve(NAMESPACE + "_en.properties"), enProps);
        writeProperties(testDataDir.resolve(NAMESPACE + "_de.properties"), deProps);
        writeProperties(testDataDir.resolve(NAMESPACE + "_fr.properties"), frProps);

        return testDataDir;
    }

    private static void writeProperties(Path filePath, Properties props) throws IOException {
        try (OutputStream os = Files.newOutputStream(filePath)) {
            props.store(os, "Generated test properties");
        }
    }

    private static Properties generateEnglishProperties() {
        Properties props = new Properties();

        // Fixed key for testing
        props.setProperty(FIXED_KEY, ORIGINAL_EN_VALUE);

        // Page titles (10 keys)
        props.setProperty("page.login.title", "Login");
        props.setProperty("page.dashboard.title", "Dashboard");
        props.setProperty("page.settings.title", "Settings");
        props.setProperty("page.profile.title", "Profile");
        props.setProperty("page.help.title", "Help");
        props.setProperty("page.about.title", "About");
        props.setProperty("page.contact.title", "Contact Us");
        props.setProperty("page.register.title", "Register");
        props.setProperty("page.search.title", "Search Results");
        props.setProperty("page.error.title", "Error");

        // Error messages (15 keys)
        props.setProperty("error.input.required", "This field is required");
        props.setProperty("error.input.invalid", "Invalid input");
        props.setProperty("error.input.too.short", "Input is too short");
        props.setProperty("error.input.too.long", "Input is too long");
        props.setProperty("error.email.invalid", "Please enter a valid email address");
        props.setProperty("error.password.weak", "Password is too weak");
        props.setProperty("error.password.mismatch", "Passwords do not match");
        props.setProperty("error.login.failed", "Login failed. Please check your credentials");
        props.setProperty("error.session.expired", "Your session has expired");
        props.setProperty("error.network.unavailable", "Network unavailable");
        props.setProperty("error.server.internal", "Internal server error");
        props.setProperty("error.permission.denied", "Permission denied");
        props.setProperty("error.file.not.found", "File not found");
        props.setProperty("error.file.upload.failed", "File upload failed");
        props.setProperty("error.data.corrupted", "Data is corrupted");

        // Buttons (15 keys)
        props.setProperty("button.save", "Save");
        props.setProperty("button.cancel", "Cancel");
        props.setProperty("button.delete", "Delete");
        props.setProperty("button.edit", "Edit");
        props.setProperty("button.close", "Close");
        props.setProperty("button.apply", "Apply");
        props.setProperty("button.reset", "Reset");
        props.setProperty("button.confirm", "Confirm");
        props.setProperty("button.back", "Back");
        props.setProperty("button.next", "Next");
        props.setProperty("button.finish", "Finish");
        props.setProperty("button.retry", "Retry");
        props.setProperty("button.download", "Download");
        props.setProperty("button.upload", "Upload");
        props.setProperty("button.refresh", "Refresh");

        // Labels (20 keys)
        props.setProperty("label.username", "Username");
        props.setProperty("label.password", "Password");
        props.setProperty("label.email", "Email Address");
        props.setProperty("label.firstName", "First Name");
        props.setProperty("label.lastName", "Last Name");
        props.setProperty("label.phone", "Phone Number");
        props.setProperty("label.address", "Address");
        props.setProperty("label.city", "City");
        props.setProperty("label.country", "Country");
        props.setProperty("label.zipCode", "ZIP Code");
        props.setProperty("label.company", "Company");
        props.setProperty("label.department", "Department");
        props.setProperty("label.role", "Role");
        props.setProperty("label.status", "Status");
        props.setProperty("label.date", "Date");
        props.setProperty("label.time", "Time");
        props.setProperty("label.description", "Description");
        props.setProperty("label.comments", "Comments");
        props.setProperty("label.notes", "Notes");
        props.setProperty("label.attachments", "Attachments");

        // Messages (20 keys)
        props.setProperty("message.welcome", "Welcome to our application");
        props.setProperty("message.goodbye", "Goodbye and see you soon");
        props.setProperty("message.success", "Operation completed successfully");
        props.setProperty("message.loading", "Loading, please wait...");
        props.setProperty("message.saving", "Saving changes...");
        props.setProperty("message.confirm.delete", "Are you sure you want to delete this item?");
        props.setProperty("message.confirm.exit", "Are you sure you want to exit?");
        props.setProperty("message.no.results", "No results found");
        props.setProperty("message.empty.list", "The list is empty");
        props.setProperty("message.changes.saved", "Your changes have been saved");
        props.setProperty("message.password.changed", "Password changed successfully");
        props.setProperty("message.email.sent", "Email has been sent");
        props.setProperty("message.file.uploaded", "File uploaded successfully");
        props.setProperty("message.data.exported", "Data exported successfully");
        props.setProperty("message.data.imported", "Data imported successfully");
        props.setProperty("message.connection.restored", "Connection restored");
        props.setProperty("message.update.available", "An update is available");
        props.setProperty("message.maintenance", "System is under maintenance");
        props.setProperty("message.beta.feature", "This is a beta feature");
        props.setProperty("message.help.available", "Help is available");

        // Navigation (10 keys)
        props.setProperty("nav.home", "Home");
        props.setProperty("nav.menu", "Menu");
        props.setProperty("nav.settings", "Settings");
        props.setProperty("nav.profile", "My Profile");
        props.setProperty("nav.logout", "Logout");
        props.setProperty("nav.admin", "Administration");
        props.setProperty("nav.reports", "Reports");
        props.setProperty("nav.analytics", "Analytics");
        props.setProperty("nav.notifications", "Notifications");
        props.setProperty("nav.preferences", "Preferences");

        // Table headers (9 keys)
        props.setProperty("table.header.id", "ID");
        props.setProperty("table.header.name", "Name");
        props.setProperty("table.header.created", "Created");
        props.setProperty("table.header.modified", "Modified");
        props.setProperty("table.header.author", "Author");
        props.setProperty("table.header.type", "Type");
        props.setProperty("table.header.size", "Size");
        props.setProperty("table.header.actions", "Actions");
        props.setProperty("table.header.priority", "Priority");

        return props;
    }

    private static Properties generateGermanProperties() {
        Properties props = new Properties();

        // Fixed key for testing - includes Umlaute for encoding stability
        props.setProperty(FIXED_KEY, ORIGINAL_DE_VALUE);

        // Page titles (10 keys) - with Umlaute
        props.setProperty("page.login.title", "Anmeldung");
        props.setProperty("page.dashboard.title", "Übersicht");
        props.setProperty("page.settings.title", "Einstellungen");
        props.setProperty("page.profile.title", "Profil");
        props.setProperty("page.help.title", "Hilfe");
        props.setProperty("page.about.title", "Über uns");
        props.setProperty("page.contact.title", "Kontakt");
        props.setProperty("page.register.title", "Registrierung");
        props.setProperty("page.search.title", "Suchergebnisse");
        props.setProperty("page.error.title", "Fehler");

        // Error messages (15 keys) - with Umlaute
        props.setProperty("error.input.required", "Dieses Feld ist erforderlich");
        props.setProperty("error.input.invalid", "Ungültige Eingabe");
        props.setProperty("error.input.too.short", "Eingabe ist zu kurz");
        props.setProperty("error.input.too.long", "Eingabe ist zu lang");
        props.setProperty("error.email.invalid", "Bitte geben Sie eine gültige E-Mail-Adresse ein");
        props.setProperty("error.password.weak", "Passwort ist zu schwach");
        props.setProperty("error.password.mismatch", "Passwörter stimmen nicht überein");
        props.setProperty("error.login.failed", "Anmeldung fehlgeschlagen. Bitte überprüfen Sie Ihre Anmeldedaten");
        props.setProperty("error.session.expired", "Ihre Sitzung ist abgelaufen");
        props.setProperty("error.network.unavailable", "Netzwerk nicht verfügbar");
        props.setProperty("error.server.internal", "Interner Serverfehler");
        props.setProperty("error.permission.denied", "Zugriff verweigert");
        props.setProperty("error.file.not.found", "Datei nicht gefunden");
        props.setProperty("error.file.upload.failed", "Datei-Upload fehlgeschlagen");
        props.setProperty("error.data.corrupted", "Daten sind beschädigt");

        // Buttons (15 keys) - with Umlaute
        props.setProperty("button.save", "Speichern");
        props.setProperty("button.cancel", "Abbrechen");
        props.setProperty("button.delete", "Löschen");
        props.setProperty("button.edit", "Bearbeiten");
        props.setProperty("button.close", "Schließen");
        props.setProperty("button.apply", "Übernehmen");
        props.setProperty("button.reset", "Zurücksetzen");
        props.setProperty("button.confirm", "Bestätigen");
        props.setProperty("button.back", "Zurück");
        props.setProperty("button.next", "Weiter");
        props.setProperty("button.finish", "Fertigstellen");
        props.setProperty("button.retry", "Wiederholen");
        props.setProperty("button.download", "Herunterladen");
        props.setProperty("button.upload", "Hochladen");
        props.setProperty("button.refresh", "Aktualisieren");

        // Labels (20 keys) - with Umlaute
        props.setProperty("label.username", "Benutzername");
        props.setProperty("label.password", "Passwort");
        props.setProperty("label.email", "E-Mail-Adresse");
        props.setProperty("label.firstName", "Vorname");
        props.setProperty("label.lastName", "Nachname");
        props.setProperty("label.phone", "Telefonnummer");
        props.setProperty("label.address", "Adresse");
        props.setProperty("label.city", "Stadt");
        props.setProperty("label.country", "Land");
        props.setProperty("label.zipCode", "Postleitzahl");
        props.setProperty("label.company", "Unternehmen");
        props.setProperty("label.department", "Abteilung");
        props.setProperty("label.role", "Rolle");
        props.setProperty("label.status", "Status");
        props.setProperty("label.date", "Datum");
        props.setProperty("label.time", "Uhrzeit");
        props.setProperty("label.description", "Beschreibung");
        props.setProperty("label.comments", "Kommentare");
        props.setProperty("label.notes", "Notizen");
        props.setProperty("label.attachments", "Anhänge");

        // Messages (20 keys) - with Umlaute
        props.setProperty("message.welcome", "Willkommen in unserer Anwendung");
        props.setProperty("message.goodbye", "Auf Wiedersehen und bis bald");
        props.setProperty("message.success", "Vorgang erfolgreich abgeschlossen");
        props.setProperty("message.loading", "Laden, bitte warten...");
        props.setProperty("message.saving", "Änderungen werden gespeichert...");
        props.setProperty("message.confirm.delete", "Möchten Sie diesen Eintrag wirklich löschen?");
        props.setProperty("message.confirm.exit", "Möchten Sie wirklich beenden?");
        props.setProperty("message.no.results", "Keine Ergebnisse gefunden");
        props.setProperty("message.empty.list", "Die Liste ist leer");
        props.setProperty("message.changes.saved", "Ihre Änderungen wurden gespeichert");
        props.setProperty("message.password.changed", "Passwort erfolgreich geändert");
        props.setProperty("message.email.sent", "E-Mail wurde gesendet");
        props.setProperty("message.file.uploaded", "Datei erfolgreich hochgeladen");
        props.setProperty("message.data.exported", "Daten erfolgreich exportiert");
        props.setProperty("message.data.imported", "Daten erfolgreich importiert");
        props.setProperty("message.connection.restored", "Verbindung wiederhergestellt");
        props.setProperty("message.update.available", "Ein Update ist verfügbar");
        props.setProperty("message.maintenance", "System wird gewartet");
        props.setProperty("message.beta.feature", "Dies ist eine Beta-Funktion");
        props.setProperty("message.help.available", "Hilfe ist verfügbar");

        // Navigation (10 keys) - with Umlaute
        props.setProperty("nav.home", "Startseite");
        props.setProperty("nav.menu", "Menü");
        props.setProperty("nav.settings", "Einstellungen");
        props.setProperty("nav.profile", "Mein Profil");
        props.setProperty("nav.logout", "Abmelden");
        props.setProperty("nav.admin", "Administration");
        props.setProperty("nav.reports", "Berichte");
        props.setProperty("nav.analytics", "Analysen");
        props.setProperty("nav.notifications", "Benachrichtigungen");
        props.setProperty("nav.preferences", "Präferenzen");

        // Table headers (9 keys) - with Umlaute
        props.setProperty("table.header.id", "ID");
        props.setProperty("table.header.name", "Name");
        props.setProperty("table.header.created", "Erstellt");
        props.setProperty("table.header.modified", "Geändert");
        props.setProperty("table.header.author", "Autor");
        props.setProperty("table.header.type", "Typ");
        props.setProperty("table.header.size", "Größe");
        props.setProperty("table.header.actions", "Aktionen");
        props.setProperty("table.header.priority", "Priorität");

        return props;
    }

    private static Properties generateFrenchProperties() {
        Properties props = new Properties();

        // Fixed key for testing
        props.setProperty(FIXED_KEY, ORIGINAL_FR_VALUE);

        // Page titles (10 keys)
        props.setProperty("page.login.title", "Connexion");
        props.setProperty("page.dashboard.title", "Tableau de bord");
        props.setProperty("page.settings.title", "Paramètres");
        props.setProperty("page.profile.title", "Profil");
        props.setProperty("page.help.title", "Aide");
        props.setProperty("page.about.title", "À propos");
        props.setProperty("page.contact.title", "Nous contacter");
        props.setProperty("page.register.title", "Inscription");
        props.setProperty("page.search.title", "Résultats de recherche");
        props.setProperty("page.error.title", "Erreur");

        // Error messages (15 keys)
        props.setProperty("error.input.required", "Ce champ est obligatoire");
        props.setProperty("error.input.invalid", "Entrée invalide");
        props.setProperty("error.input.too.short", "L'entrée est trop courte");
        props.setProperty("error.input.too.long", "L'entrée est trop longue");
        props.setProperty("error.email.invalid", "Veuillez entrer une adresse e-mail valide");
        props.setProperty("error.password.weak", "Le mot de passe est trop faible");
        props.setProperty("error.password.mismatch", "Les mots de passe ne correspondent pas");
        props.setProperty("error.login.failed", "Échec de connexion. Veuillez vérifier vos identifiants");
        props.setProperty("error.session.expired", "Votre session a expiré");
        props.setProperty("error.network.unavailable", "Réseau non disponible");
        props.setProperty("error.server.internal", "Erreur interne du serveur");
        props.setProperty("error.permission.denied", "Permission refusée");
        props.setProperty("error.file.not.found", "Fichier non trouvé");
        props.setProperty("error.file.upload.failed", "Échec du téléchargement du fichier");
        props.setProperty("error.data.corrupted", "Les données sont corrompues");

        // Buttons (15 keys)
        props.setProperty("button.save", "Enregistrer");
        props.setProperty("button.cancel", "Annuler");
        props.setProperty("button.delete", "Supprimer");
        props.setProperty("button.edit", "Modifier");
        props.setProperty("button.close", "Fermer");
        props.setProperty("button.apply", "Appliquer");
        props.setProperty("button.reset", "Réinitialiser");
        props.setProperty("button.confirm", "Confirmer");
        props.setProperty("button.back", "Retour");
        props.setProperty("button.next", "Suivant");
        props.setProperty("button.finish", "Terminer");
        props.setProperty("button.retry", "Réessayer");
        props.setProperty("button.download", "Télécharger");
        props.setProperty("button.upload", "Envoyer");
        props.setProperty("button.refresh", "Actualiser");

        // Labels (20 keys)
        props.setProperty("label.username", "Nom d'utilisateur");
        props.setProperty("label.password", "Mot de passe");
        props.setProperty("label.email", "Adresse e-mail");
        props.setProperty("label.firstName", "Prénom");
        props.setProperty("label.lastName", "Nom de famille");
        props.setProperty("label.phone", "Numéro de téléphone");
        props.setProperty("label.address", "Adresse");
        props.setProperty("label.city", "Ville");
        props.setProperty("label.country", "Pays");
        props.setProperty("label.zipCode", "Code postal");
        props.setProperty("label.company", "Entreprise");
        props.setProperty("label.department", "Département");
        props.setProperty("label.role", "Rôle");
        props.setProperty("label.status", "Statut");
        props.setProperty("label.date", "Date");
        props.setProperty("label.time", "Heure");
        props.setProperty("label.description", "Description");
        props.setProperty("label.comments", "Commentaires");
        props.setProperty("label.notes", "Notes");
        props.setProperty("label.attachments", "Pièces jointes");

        // Messages (20 keys)
        props.setProperty("message.welcome", "Bienvenue dans notre application");
        props.setProperty("message.goodbye", "Au revoir et à bientôt");
        props.setProperty("message.success", "Opération terminée avec succès");
        props.setProperty("message.loading", "Chargement, veuillez patienter...");
        props.setProperty("message.saving", "Enregistrement des modifications...");
        props.setProperty("message.confirm.delete", "Êtes-vous sûr de vouloir supprimer cet élément?");
        props.setProperty("message.confirm.exit", "Êtes-vous sûr de vouloir quitter?");
        props.setProperty("message.no.results", "Aucun résultat trouvé");
        props.setProperty("message.empty.list", "La liste est vide");
        props.setProperty("message.changes.saved", "Vos modifications ont été enregistrées");
        props.setProperty("message.password.changed", "Mot de passe modifié avec succès");
        props.setProperty("message.email.sent", "L'e-mail a été envoyé");
        props.setProperty("message.file.uploaded", "Fichier téléchargé avec succès");
        props.setProperty("message.data.exported", "Données exportées avec succès");
        props.setProperty("message.data.imported", "Données importées avec succès");
        props.setProperty("message.connection.restored", "Connexion restaurée");
        props.setProperty("message.update.available", "Une mise à jour est disponible");
        props.setProperty("message.maintenance", "Le système est en maintenance");
        props.setProperty("message.beta.feature", "Ceci est une fonctionnalité bêta");
        props.setProperty("message.help.available", "L'aide est disponible");

        // Navigation (10 keys)
        props.setProperty("nav.home", "Accueil");
        props.setProperty("nav.menu", "Menu");
        props.setProperty("nav.settings", "Paramètres");
        props.setProperty("nav.profile", "Mon profil");
        props.setProperty("nav.logout", "Déconnexion");
        props.setProperty("nav.admin", "Administration");
        props.setProperty("nav.reports", "Rapports");
        props.setProperty("nav.analytics", "Analyses");
        props.setProperty("nav.notifications", "Notifications");
        props.setProperty("nav.preferences", "Préférences");

        // Table headers (9 keys)
        props.setProperty("table.header.id", "ID");
        props.setProperty("table.header.name", "Nom");
        props.setProperty("table.header.created", "Créé");
        props.setProperty("table.header.modified", "Modifié");
        props.setProperty("table.header.author", "Auteur");
        props.setProperty("table.header.type", "Type");
        props.setProperty("table.header.size", "Taille");
        props.setProperty("table.header.actions", "Actions");
        props.setProperty("table.header.priority", "Priorité");

        return props;
    }
}
