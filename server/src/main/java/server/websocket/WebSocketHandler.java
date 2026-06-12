package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsMessageContext;
import model.AuthData;
import model.GameData;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

public class WebSocketHandler {

    private final Gson gson = new Gson();
    private final GameService gameService;
    private final ConnectionManager connectionManager = new ConnectionManager();

    public WebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public void onConnect(WsConnectContext ctx) {
        // Wait for the client to send a CONNECT command.
    }

    public void onMessage(WsMessageContext ctx) {
        try {
            UserGameCommand command = gson.fromJson(ctx.message(), UserGameCommand.class);

            switch (command.getCommandType()) {
                case CONNECT -> connect(ctx, command);
                case MAKE_MOVE -> makeMove(ctx, command);
                case LEAVE -> leave(ctx, command);
                case RESIGN -> resign(ctx, command);
            }
        } catch (DataAccessException e) {
            sendError(ctx, e.getMessage());
        } catch (Exception e) {
            sendError(ctx, "error");
        }
    }

    public void onClose(WsCloseContext ctx) {
        connectionManager.removeBySession(ctx.sessionId());
    }

    private void connect(WsMessageContext ctx, UserGameCommand command) throws DataAccessException {
        AuthData auth = gameService.validateAuth(command.getAuthToken());
        GameData game = gameService.connect(command.getAuthToken(), command.getGameID());

        Connection connection = new Connection(auth.username(), command.getGameID(), ctx);
        connectionManager.add(connection);

        send(connection, new LoadGameMessage(game));

        String role = getRole(auth.username(), game);
        notifyOthers(command.getGameID(), connection, auth.username() + " joined as " + role + ".");
    }

    private void makeMove(WsMessageContext ctx, UserGameCommand command) throws DataAccessException {
        Connection sender = connectionManager.getBySession(ctx.sessionId());

        if (sender == null) {
            throw new DataAccessException("unauthorized");
        }

        GameData gameBeforeMove = gameService.connect(command.getAuthToken(), command.getGameID());
        ChessMove move = command.getMove();
        ChessPiece piece = gameBeforeMove.game().getBoard().getPiece(move.getStartPosition());

        GameData updatedGame = gameService.makeMove(
                command.getAuthToken(),
                command.getGameID(),
                move
        );

        sendLoadGameToAll(command.getGameID(), updatedGame);

        String moveMessage = sender.username()
                + " moved " + pieceName(piece)
                + " from " + positionName(move.getStartPosition())
                + " to " + positionName(move.getEndPosition()) + ".";

        notifyOthers(command.getGameID(), sender, moveMessage);

        if (gameService.isInCheckmate(updatedGame)) {
            sendNotificationToAll(command.getGameID(),
                    checkedPlayerName(updatedGame) + " is in checkmate.");
        } else if (gameService.isInCheck(updatedGame)) {
            sendNotificationToAll(command.getGameID(),
                    checkedPlayerName(updatedGame) + " is in check.");
        }
    }

    private void leave(WsMessageContext ctx, UserGameCommand command) throws DataAccessException {
        Connection sender = connectionManager.getBySession(ctx.sessionId());

        if (sender == null) {
            throw new DataAccessException("unauthorized");
        }

        String username = gameService.leave(command.getAuthToken(), command.getGameID());
        connectionManager.remove(sender);

        sendNotificationToAll(command.getGameID(), username + " left the game.");
    }

    private void resign(WsMessageContext ctx, UserGameCommand command) throws DataAccessException {
        Connection sender = connectionManager.getBySession(ctx.sessionId());

        if (sender == null) {
            throw new DataAccessException("unauthorized");
        }

        gameService.resign(command.getAuthToken(), command.getGameID());

        sendNotificationToAll(command.getGameID(), sender.username() + " resigned.");
    }

    private String getRole(String username, GameData game) {
        if (username.equals(game.whiteUsername())) {
            return "WHITE";
        }

        if (username.equals(game.blackUsername())) {
            return "BLACK";
        }

        return "an observer";
    }

    private String checkedPlayerName(GameData game) {
        ChessGame.TeamColor checkedColor = game.game().getTeamTurn();

        if (checkedColor == ChessGame.TeamColor.WHITE) {
            return game.whiteUsername();
        }

        return game.blackUsername();
    }

    private String pieceName(ChessPiece piece) {
        if (piece == null) {
            return "piece";
        }

        return piece.getPieceType().name().toLowerCase();
    }

    private String positionName(ChessPosition position) {
        char file = (char) ('a' + position.getColumn() - 1);
        return "" + file + position.getRow();
    }

    private void sendLoadGameToAll(int gameID, GameData game) {
        for (Connection connection : connectionManager.getConnectionsForGame(gameID)) {
            send(connection, new LoadGameMessage(game));
        }
    }

    private void sendNotificationToAll(int gameID, String message) {
        for (Connection connection : connectionManager.getConnectionsForGame(gameID)) {
            send(connection, new NotificationMessage(message));
        }
    }

    private void notifyOthers(int gameID, Connection sender, String message) {
        for (Connection connection : connectionManager.getConnectionsForGame(gameID)) {
            if (!connection.session().sessionId().equals(sender.session().sessionId())) {
                send(connection, new NotificationMessage(message));
            }
        }
    }

    private void send(Connection connection, Object message) {
        connection.send(gson.toJson(message));
    }

    private void sendError(WsMessageContext ctx, String message) {
        ctx.send(gson.toJson(new ErrorMessage("Error: " + message)));
    }
}