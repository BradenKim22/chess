package client.repl;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import client.websocket.ServerMessageObserver;
import client.websocket.WebSocketFacade;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.GameData;
import ui.BoardPrinter;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import java.util.Collection;
import java.util.Scanner;

public class GameplayRepl implements ServerMessageObserver {

    private final Gson gson = new Gson();
    private final Scanner scanner;
    private final BoardPrinter boardPrinter = new BoardPrinter();

    private final String serverUrl;
    private final String authToken;
    private final int gameID;
    private final ChessGame.TeamColor perspective;

    private WebSocketFacade webSocketFacade;
    private GameData currentGame;
    private boolean running = true;

    public GameplayRepl(String serverUrl, String authToken, int gameID,
                        ChessGame.TeamColor perspective, Scanner scanner) {
        this.serverUrl = serverUrl;
        this.authToken = authToken;
        this.gameID = gameID;
        this.perspective = perspective;
        this.scanner = scanner;
    }

    public void run() {
        try {
            webSocketFacade = new WebSocketFacade(serverUrl, this);
            webSocketFacade.connect(authToken, gameID);
        } catch (Exception e) {
            System.out.println("Could not connect to game: " + e.getMessage());
            return;
        }

        printHelp();

        while (running) {
            System.out.print("[GAME] >>> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] tokens = input.split("\\s+");
            String command = tokens[0].toLowerCase();

            switch (command) {
                case "help" -> printHelp();
                case "redraw" -> redrawBoard();
                case "leave" -> leaveGame();
                case "move" -> makeMove(tokens);
                case "resign" -> resign();
                case "highlight" -> highlightMoves(tokens);
                default -> System.out.println("Unknown command. Type help.");
            }
        }
    }

    @Override
    public void notify(String message) {
        JsonObject json = JsonParser.parseString(message).getAsJsonObject();
        String messageType = json.get("serverMessageType").getAsString();

        switch (messageType) {
            case "LOAD_GAME" -> handleLoadGame(message);
            case "NOTIFICATION" -> handleNotification(message);
            case "ERROR" -> handleError(message);
            default -> System.out.println("Unknown server message.");
        }
    }

    private void handleLoadGame(String message) {
        LoadGameMessage loadGameMessage = gson.fromJson(message, LoadGameMessage.class);
        currentGame = loadGameMessage.getGame();
        redrawBoard();
    }

    private void handleNotification(String message) {
        NotificationMessage notification = gson.fromJson(message, NotificationMessage.class);
        System.out.println(notification.getMessage());
    }

    private void handleError(String message) {
        ErrorMessage error = gson.fromJson(message, ErrorMessage.class);
        System.out.println(error.getErrorMessage());
    }

    private void redrawBoard() {
        if (currentGame == null) {
            System.out.println("Board has not loaded yet.");
            return;
        }

        boardPrinter.printBoard(currentGame, perspective);
    }

    private void leaveGame() {
        try {
            webSocketFacade.leave(authToken, gameID);
            webSocketFacade.close();
            running = false;
            System.out.println("Left the game.");
        } catch (Exception e) {
            System.out.println("Leave failed: " + e.getMessage());
        }
    }

    private void makeMove(String[] tokens) {
        if (tokens.length < 3 || tokens.length > 4) {
            System.out.println("Usage: move <START> <END> [PROMOTION]");
            System.out.println("Example: move e2 e4");
            return;
        }

        try {
            ChessPosition start = parsePosition(tokens[1]);
            ChessPosition end = parsePosition(tokens[2]);
            ChessPiece.PieceType promotion = null;

            if (tokens.length == 4) {
                promotion = parsePromotion(tokens[3]);
            }

            ChessMove move = new ChessMove(start, end, promotion);
            UserGameCommand command = new UserGameCommand(authToken, gameID, move);

            webSocketFacade.makeMove(command);
        } catch (Exception e) {
            System.out.println("Move failed: " + e.getMessage());
        }
    }

    private void resign() {
        System.out.print("Are you sure you want to resign? Type yes to confirm: ");
        String answer = scanner.nextLine().trim();

        if (!answer.equalsIgnoreCase("yes")) {
            System.out.println("Resign cancelled.");
            return;
        }

        try {
            webSocketFacade.resign(authToken, gameID);
        } catch (Exception e) {
            System.out.println("Resign failed: " + e.getMessage());
        }
    }

    private void highlightMoves(String[] tokens) {
        if (tokens.length != 2) {
            System.out.println("Usage: highlight <POSITION>");
            System.out.println("Example: highlight e2");
            return;
        }

        if (currentGame == null) {
            System.out.println("Board has not loaded yet.");
            return;
        }

        try {
            ChessPosition position = parsePosition(tokens[1]);
            Collection<ChessMove> validMoves = currentGame.game().validMoves(position);

            if (validMoves == null || validMoves.isEmpty()) {
                System.out.println("No legal moves for that position.");
                return;
            }

            boardPrinter.printBoard(currentGame, perspective, validMoves);
        } catch (Exception e) {
            System.out.println("Highlight failed: " + e.getMessage());
        }
    }

    private ChessPosition parsePosition(String text) {
        if (text.length() != 2) {
            throw new IllegalArgumentException("Position must look like e2.");
        }

        char file = Character.toLowerCase(text.charAt(0));
        char rank = text.charAt(1);

        int column = file - 'a' + 1;
        int row = rank - '0';

        if (column < 1 || column > 8 || row < 1 || row > 8) {
            throw new IllegalArgumentException("Position must be between a1 and h8.");
        }

        return new ChessPosition(row, column);
    }

    private ChessPiece.PieceType parsePromotion(String text) {
        return switch (text.toLowerCase()) {
            case "queen", "q" -> ChessPiece.PieceType.QUEEN;
            case "rook", "r" -> ChessPiece.PieceType.ROOK;
            case "bishop", "b" -> ChessPiece.PieceType.BISHOP;
            case "knight", "n" -> ChessPiece.PieceType.KNIGHT;
            default -> throw new IllegalArgumentException("Promotion must be queen, rook, bishop, or knight.");
        };
    }

    private void printHelp() {
        System.out.println("redraw - redraw the chess board");
        System.out.println("move <START> <END> [PROMOTION] - make a move, example: move e2 e4");
        System.out.println("highlight <POSITION> - highlight legal moves for a piece");
        System.out.println("resign - resign from the game");
        System.out.println("leave - leave the game");
        System.out.println("help - show possible commands");
    }
}