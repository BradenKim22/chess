package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySQLDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import model.AuthData;
import model.GameData;
import model.UserData;
import request.CreateGameRequest;
import request.JoinGameRequest;
import request.LoginRequest;
import request.RegisterRequest;
import result.AuthResult;
import result.CreateGameResult;
import result.ErrorResult;
import result.GameSummary;
import result.ListGamesResult;
import server.websocket.WebSocketHandler;
import service.ClearService;
import service.GameService;
import service.UserService;

import java.util.ArrayList;
import java.util.Collection;

public class Server {

    private final Gson gson = new Gson();
    private final DataAccess dataAccess;
    private final ClearService clearService;
    private final UserService userService;
    private final GameService gameService;
    private final WebSocketHandler webSocketHandler;

    private Javalin app;

    public Server() {
        try {
            dataAccess = new MySQLDataAccess();
            clearService = new ClearService(dataAccess);
            userService = new UserService(dataAccess);
            gameService = new GameService(dataAccess);
            webSocketHandler = new WebSocketHandler(gameService);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public int run(int desiredPort) {
        app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/web";
                staticFiles.location = Location.CLASSPATH;
            });
        });

        registerRoutes();
        registerWebSocket();

        app.start(desiredPort);
        return app.port();
    }

    public void stop() {
        app.stop();
    }

    private void registerRoutes() {
        app.delete("/db", this::clearDatabase);
        app.post("/user", this::registerUser);
        app.post("/session", this::loginUser);
        app.delete("/session", this::logoutUser);
        app.get("/game", this::listGames);
        app.post("/game", this::createGame);
        app.put("/game", this::joinGame);
    }

    private void registerWebSocket() {
        app.ws("/ws", ws -> {
            ws.onConnect(webSocketHandler::onConnect);
            ws.onMessage(webSocketHandler::onMessage);
            ws.onClose(webSocketHandler::onClose);
        });
    }

    private void clearDatabase(Context ctx) {
        try {
            clearService.clear();
            sendEmptySuccess(ctx);
        } catch (DataAccessException e) {
            sendServerError(ctx, e);
        }
    }

    private void registerUser(Context ctx) {
        try {
            RegisterRequest request = gson.fromJson(ctx.body(), RegisterRequest.class);

            UserData user = new UserData(
                    request.username(),
                    request.password(),
                    request.email()
            );

            AuthData auth = userService.register(user);

            sendJson(ctx, new AuthResult(auth.username(), auth.authToken()));
        } catch (DataAccessException e) {
            handleDataAccessException(ctx, e);
        }
    }

    private void loginUser(Context ctx) {
        try {
            LoginRequest request = gson.fromJson(ctx.body(), LoginRequest.class);

            AuthData auth = userService.login(request.username(), request.password());

            sendJson(ctx, new AuthResult(auth.username(), auth.authToken()));
        } catch (DataAccessException e) {
            handleDataAccessException(ctx, e);
        }
    }

    private void logoutUser(Context ctx) {
        try {
            String authToken = ctx.header("authorization");

            userService.logout(authToken);

            sendEmptySuccess(ctx);
        } catch (DataAccessException e) {
            handleDataAccessException(ctx, e);
        }
    }

    private void listGames(Context ctx) {
        try {
            String authToken = ctx.header("authorization");

            Collection<GameData> games = gameService.listGames(authToken);
            Collection<GameSummary> gameSummaries = new ArrayList<>();

            for (GameData game : games) {
                gameSummaries.add(new GameSummary(
                        game.gameID(),
                        game.whiteUsername(),
                        game.blackUsername(),
                        game.gameName()
                ));
            }

            sendJson(ctx, new ListGamesResult(gameSummaries));
        } catch (DataAccessException e) {
            handleDataAccessException(ctx, e);
        }
    }

    private void createGame(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            CreateGameRequest request = gson.fromJson(ctx.body(), CreateGameRequest.class);

            int gameID = gameService.createGame(authToken, request.gameName());

            sendJson(ctx, new CreateGameResult(gameID));
        } catch (DataAccessException e) {
            handleDataAccessException(ctx, e);
        }
    }

    private void joinGame(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            JoinGameRequest request = gson.fromJson(ctx.body(), JoinGameRequest.class);

            gameService.joinGame(authToken, request.playerColor(), request.gameID());

            sendEmptySuccess(ctx);
        } catch (DataAccessException e) {
            handleDataAccessException(ctx, e);
        }
    }

    private void handleDataAccessException(Context ctx, DataAccessException e) {
        switch (e.getMessage()) {
            case "bad request" -> sendError(ctx, 400, "bad request");
            case "unauthorized" -> sendError(ctx, 401, "unauthorized");
            case "already taken" -> sendError(ctx, 403, "already taken");
            default -> sendServerError(ctx, e);
        }
    }

    private void sendJson(Context ctx, Object responseBody) {
        ctx.status(200);
        ctx.result(gson.toJson(responseBody));
    }

    private void sendEmptySuccess(Context ctx) {
        ctx.status(200);
        ctx.result("{}");
    }

    private void sendError(Context ctx, int statusCode, String message) {
        ctx.status(statusCode);
        ctx.result(gson.toJson(new ErrorResult("Error: " + message)));
    }

    private void sendServerError(Context ctx, Exception e) {
        sendError(ctx, 500, e.getMessage());
    }
}