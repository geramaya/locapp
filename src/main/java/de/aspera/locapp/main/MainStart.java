package de.aspera.locapp.main;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.h2.tools.Server;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import de.aspera.locapp.cmd.CommandContext;
import de.aspera.locapp.cmd.CommandException;
import de.aspera.locapp.cmd.LocAppCLI;
import de.aspera.locapp.dao.H2DatabaseManager;
import de.aspera.locapp.util.Resources;
import picocli.CommandLine;

/**
 * The main class to start the application.
 * Modernized with Picocli for command parsing and JLine 3 for interactive shell.
 *
 * @author Daniel.Weiss
 *
 */
public class MainStart {
    private static final String BLANK = " ";
    private static Server H2Server;
    private static final Logger logger = Logger.getLogger(MainStart.class.getName());
    private static CommandLine commandLine;

    public static void main(String[] args) throws ParseException, SQLException {
        init();
        // After start -> hold the command cli in recursive mode.
        promptCLI();
    }

    private static void init() {
        try {
            splash();

            // Initialize Picocli CommandLine
            commandLine = new CommandLine(new LocAppCLI());
            
            // Legacy support: still execute help through context
            Resources.getInstance();
            loadDatabase();
            // Start the program with init parameters (e.g. blacklist for import filenames)
            CommandContext.getInstance().executeCommand("init");
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            System.exit(0);
        }
    }

    /**
     * This method presents the command input (CLI) of the application using JLine 3.
     * Provides modern interactive shell with TAB completion support.
     */
    private static void promptCLI() {
        try {
            // Build terminal
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            
            // Create Picocli CommandLine for execution
            CommandLine cmd = new CommandLine(new LocAppCLI());
            
            // Create a completer with all available command names and aliases
            Completer completer = new StringsCompleter(
                // Main commands
                "help", "h",
                "quit", "q",
                "files", "f",
                "import-properties", "ip",
                "excel-export", "ee",
                "excel-import", "ei",
                "export-properties", "ep",
                "init", "i",
                "export-delta", "ed",
                "import-delta", "id",
                "properties-count", "pc",
                "merge-properties", "mp",
                "check-integrity", "ci",
                "clear-loc", "cl",
                "import-ignore-list", "iil",
                "clear-ignore-list", "cil",
                "set-default-language", "sdl",
                "csv-import", "csvin",
                "cscmig",
                "search", "s", "find",
                // Common options
                "--help", "-h",
                "--version", "-V",
                "--language", "-l",
                "--empty", "-e",
                "--all", "-a"
            );
            
            // Build LineReader with completer for TAB completion
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .build();
            
            // Set prompt
            String prompt = "\n>> command: ";
            
            // Interactive loop
            while (true) {
                try {
                    String line = reader.readLine(prompt);
                    if (line == null || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    String cmdline = line.trim();
                    
                    // Parse the command line
                    String[] args = cmdline.split("\\s+");
                    
                    // Try to execute via Picocli first
                    try {
                        int exitCode = cmd.execute(args);
                        if (exitCode != 0) {
                            // If Picocli execution failed, try legacy CommandContext
                            executeLegacyCommand(cmdline);
                        }
                    } catch (Exception e) {
                        // Fall back to legacy CommandContext if Picocli fails
                        executeLegacyCommand(cmdline);
                    }
                    
                } catch (UserInterruptException e) {
                    // User pressed Ctrl+C
                    System.out.println("Use 'quit' or 'q' to exit the application.");
                } catch (EndOfFileException e) {
                    // User pressed Ctrl+D
                    System.out.println("quit. good bye!");
                    break;
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize terminal", e);
            // Fall back to legacy mode
            promptCLILegacy();
        }
    }
    
    /**
     * Legacy CLI mode using Scanner for environments where JLine may not work.
     */
    @SuppressWarnings("resource")
    private static void promptCLILegacy() {
        // Scanner wrapping System.in should not be closed as it would close System.in
        // which is a system resource that should remain open for the application lifetime
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        while (true) {
            System.out.print("\n>> command: ");
            String cmdline = scanner.nextLine().trim();
            executeLegacyCommand(cmdline);
        }
    }
    
    /**
     * Execute command using the legacy CommandContext.
     */
    private static void executeLegacyCommand(String cmdline) {
        if (cmdline.contains(BLANK)) {
            String[] args = cmdline.split(BLANK);
            for (int i = 0; i < args.length; i++) {
                CommandContext.getInstance().addArgument(args[i]);
            }
        } else {
            CommandContext.getInstance().addArgument(cmdline);
        }
        String cmd = CommandContext.getInstance().nextArgument();
        if (CommandContext.getInstance().isCommand(cmd)) {
            try {
                CommandContext.getInstance().executeCommand(cmd);
            } catch (CommandException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            logger.warning("Sorry! This command is unknown!");
        }
    }

    /**
     * Bootstrap Handling for the H2 Database
     *
     * @throws SQLException
     */
    private static void loadDatabase() throws SQLException {
        long currentTimeMillis = System.currentTimeMillis();
        H2Server = Server.createTcpServer().start();
        H2DatabaseManager.getInstance().getEntityManager();
        long diff = System.currentTimeMillis() - currentTimeMillis;
        logger.log(Level.INFO, "Start H2 Database and JPA Connection in " + diff + " milliseconds.");
    }

    /**
     * Just a gimmick :)
     *
     * @throws IOException
     */
    private static void splash() throws IOException {
        // need to adjust for width and height
        System.out.println("\n\n");
        BufferedImage image = new BufferedImage(144, 32, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setFont(new Font("Dialog", Font.PLAIN, 12));
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // the banner text may affect width and height
        graphics.drawString("LocApp", 6, 24);
        ImageIO.write(image, "png", File.createTempFile("AsciiBanner.png", null));

        // need to adjust for width and height
        for (int y = 0; y < 32; y++) {
            StringBuilder sb = new StringBuilder();
            // need to adjust for width and height
            for (int x = 0; x < 144; x++)
                sb.append(image.getRGB(x, y) == -16777216 ? BLANK : image.getRGB(x, y) == -1 ? "*" : "*");
            if (sb.toString().trim().isEmpty())
                continue;
            System.out.println(sb);
        }
        System.out.println("\n\n");
    }
}
