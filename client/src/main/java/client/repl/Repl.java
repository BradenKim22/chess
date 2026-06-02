package client.repl;

import client.server.ResponseException;
import client.server.ServerFacade;
import result.AuthResult;
import result.CreateGameResult;
import result.GameSummary;
import result.ListGamesResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

public class Repl {

    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade serverFacade;

    private boolean loggedIn = false;
    private String authToken;
    private String username;
    private final ArrayList<GameSummary> listedGames = new ArrayList<>();

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

            if (input.isEmpty()) {
                continue;
            }

            String[] tokens = input.split("\\s+");
            String command = tokens[0].toLowerCase();

            switch (command) {
                case "quit" -> running = false;
                case "help" -> printHelp();
                case "register" -> register(tokens);
                case "login" -> login(tokens);
                case "logout" -> logout();
                case "create" -> createGame(tokens);
                case "list" -> listGames();
                default -> System.out.println("Unknown command. Type help.");
            }
        }

        System.out.println("Goodbye!");
    }

    private void register(String[] tokens) {
        if (loggedIn) {
            System.out.println("You are already logged in.");
            return;
        }

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
        if (loggedIn) {
            System.out.println("You are already logged in.");
            return;
        }

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

    private void logout() {
        if (!loggedIn) {
            System.out.println("You are not logged in.");
            return;
        }

        try {
            serverFacade.logout(authToken);
            authToken = null;
            username = null;
            loggedIn = false;
            listedGames.clear();
            System.out.println("Logged out.");
        } catch (ResponseException e) {
            System.out.println("Logout failed: " + e.getMessage());
        }
    }

    private void createGame(String[] tokens) {
        if (!requireLogin()) {
            return;
        }

        if (tokens.length < 2) {
            System.out.println("Usage: create <GAME_NAME>");
            return;
        }

        String gameName = String.join(" ", copyAfterFirst(tokens));

        try {
            CreateGameResult result = serverFacade.createGame(gameName, authToken);
            System.out.println("Created game '" + gameName + "' with game ID " + result.gameID() + ".");
            System.out.println("Use 'list' to see the updated game list.");
        } catch (ResponseException e) {
            System.out.println("Create game failed: " + e.getMessage());
        }
    }

    private void listGames() {
        if (!requireLogin()) {
            return;
        }

        try {
            ListGamesResult result = serverFacade.listGames(authToken);
            listedGames.clear();
            listedGames.addAll(result.games());

            if (listedGames.isEmpty()) {
                System.out.println("No games found.");
                return;
            }

            printGames(listedGames);
        } catch (ResponseException e) {
            System.out.println("List games failed: " + e.getMessage());
        }
    }

    private void printGames(Collection<GameSummary> games) {
        int displayNumber = 1;

        for (GameSummary game : games) {
            System.out.println(displayNumber + ". " + game.gameName()
                    + " | White: " + displayName(game.whiteUsername())
                    + " | Black: " + displayName(game.blackUsername()));
            displayNumber++;
        }
    }

    private String displayName(String name) {
        if (name == null) {
            return "empty";
        }

        return name;
    }

    private boolean requireLogin() {
        if (!loggedIn) {
            System.out.println("You must be logged in to use this command.");
            return false;
        }

        return true;
    }

    private String[] copyAfterFirst(String[] tokens) {
        String[] result = new String[tokens.length - 1];

        for (int i = 1; i < tokens.length; i++) {
            result[i - 1] = tokens[i];
        }

        return result;
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