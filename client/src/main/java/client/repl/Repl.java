package client.repl;

import java.util.Scanner;

public class Repl {

    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);

    public Repl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void run() {
        System.out.println("Welcome to 240 Chess!");
        System.out.println("Type help to get started.");

        boolean running = true;

        while (running) {
            System.out.print("[LOGGED_OUT] >>> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                running = false;
            } else if (input.equalsIgnoreCase("help")) {
                printHelp();
            } else {
                System.out.println("Unknown command. Type help.");
            }
        }

        System.out.println("Goodbye!");
    }

    private void printHelp() {
        System.out.println("register <USERNAME> <PASSWORD> <EMAIL> - create an account");
        System.out.println("login <USERNAME> <PASSWORD> - log in");
        System.out.println("quit - exit the program");
        System.out.println("help - show possible commands");
    }
}