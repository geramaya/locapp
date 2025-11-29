package de.aspera.locapp.cmd;

import picocli.CommandLine.Command;

@Command(
    name = "quit",
    aliases = {"q"},
    description = "Quit the program.",
    mixinStandardHelpOptions = true
)
public class QuitCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("quit. good bye!");
        System.exit(0);
    }
}
