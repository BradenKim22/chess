package client.repl;

import client.server.ResponseException;
import client.server.ServerFacade;
import result.AuthResult;

import java.util.Scanner;

public class Repl {

    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade serverFacade;

    private boolean loggedIn = false;
    private String authToken;
    private String username;

    public Repl(String serverUrl) {
        serverFacade = new ServerFacade(serverUrl);
    }

    public void run() {
        System.out.println("Welcome to 240 Chess!");
        System.out.println("Type help to get started.");

        boolean running = true;

        while (running) {
            printPrompt();

            String input = scanner.nextLine().trim();
            String[] tokens = input.split("\\s+");

            if (input.isEmpty()) {
                continue;
            }

            String command = tokens[0].toLowerCase();

            switch (command) {
                case "quit" -> running = false;
                case "help" -> printHelp();
                case "register" -> register(tokens);
                case "login" -> login(tokens);
                default -> System.out.println("Unknown command. Type help.");
            }
        }

        System.out.println("Goodbye!");
    }

    private void register(String[] tokens) {
        if (tokens.length != 4) {
            System.out.println("Usage: register <USERNAME> <PASSWORD> <EMAIL>");
            return;
        }

        try {
            AuthResult result = serverFacade.register(tokens[1], tokens[2], tokens[3]);
            saveLogin(result);
            System.out.println("Registered and logged in as " + username + ".");
        } catch (ResponseException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void login(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Usage: login <USERNAME> <PASSWORD>");
            return;
        }

        try {
            AuthResult result = serverFacade.login(tokens[1], tokens[2]);
            saveLogin(result);
            System.out.println("Logged in as " + username + ".");
        } catch (ResponseException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private void saveLogin(AuthResult result) {
        authToken = result.authToken();
        username = result.username();
        loggedIn = true;
    }

    private void printPrompt() {
        if (loggedIn) {
            System.out.print("[LOGGED_IN as " + username + "] >>> ");
        } else {
            System.out.print("[LOGGED_OUT] >>> ");
        }
    }

    private void printHelp() {
        if (loggedIn) {
            printLoggedInHelp();
        } else {
            printLoggedOutHelp();
        }
    }

    private void printLoggedOutHelp() {
        System.out.println("register <USERNAME> <PASSWORD> <EMAIL> - create an account");
        System.out.println("login <USERNAME> <PASSWORD> - log in");
        System.out.println("quit - exit the program");
        System.out.println("help - show possible commands");
    }

    private void printLoggedInHelp() {
        System.out.println("create <GAME_NAME> - create a new game");
        System.out.println("list - list available games");
        System.out.println("play <GAME_NUMBER> <WHITE|BLACK> - join a game");
        System.out.println("observe <GAME_NUMBER> - observe a game");
        System.out.println("logout - log out");
        System.out.println("quit - exit the program");
        System.out.println("help - show possible commands");
    }
}