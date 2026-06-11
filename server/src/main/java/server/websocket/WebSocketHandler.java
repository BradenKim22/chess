package server.websocket;

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
        // The actual game connection happens after the client sends CONNECT.
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

        String message = auth.username() + " connected to the game.";
        notifyOthers(command.getGameID(), connection, message);
    }

    private void makeMove(WsMessageContext ctx, UserGameCommand command) throws DataAccessException {
        Connection sender = connectionManager.getBySession(ctx.sessionId());

        if (sender == null) {
            throw new DataAccessException("unauthorized");
        }

        GameData updatedGame = gameService.makeMove(
                command.getAuthToken(),
                command.getGameID(),
                command.getMove()
        );

        sendLoadGameToAll(command.getGameID(), updatedGame);

        String moveMessage = sender.username() + " made a move.";
        notifyOthers(command.getGameID(), sender, moveMessage);

        if (gameService.isInCheckmate(updatedGame)) {
            sendNotificationToAll(command.getGameID(), "A player is in checkmate.");
        } else if (gameService.isInCheck(updatedGame)) {
            sendNotificationToAll(command.getGameID(), "A player is in check.");
        }
    }

    private void leave(WsMessageContext ctx, UserGameCommand command) throws DataAccessException {
        Connection sender = connectionManager.getBySession(ctx.sessionId());

        if (sender == null) {
            throw new DataAccessException("unauthorized");
        }

        String username = gameService.leave(command.getAuthToken(), command.getGameID());

        connectionManager.remove(sender);

        String message = username + " left the game.";
        sendNotificationToAll(command.getGameID(), message);
    }

    private void resign(WsMessageContext ctx, UserGameCommand command) throws DataAccessException {
        Connection sender = connectionManager.getBySession(ctx.sessionId());

        if (sender == null) {
            throw new DataAccessException("unauthorized");
        }

        gameService.resign(command.getAuthToken(), command.getGameID());

        String message = sender.username() + " resigned.";
        sendNotificationToAll(command.getGameID(), message);
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