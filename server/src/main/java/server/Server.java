package server;

import com.google.gson.Gson;
import dataaccess.MemoryDataAccess;
import io.javalin.Javalin;
import service.ClearService;
import service.GameService;
import service.UserService;

import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import request.RegisterRequest;
import result.AuthResult;
import result.ErrorResult;

import request.LoginRequest;

public class Server {

    private final Gson gson = new Gson();
    private final MemoryDataAccess dataAccess = new MemoryDataAccess();

    private final ClearService clearService = new ClearService(dataAccess);
    private final UserService userService = new UserService(dataAccess);
    private final GameService gameService = new GameService(dataAccess);

    public int run(int desiredPort) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("web");
        });

        app.delete("/db", ctx -> {
            clearService.clear();
            ctx.status(200);
            ctx.result("{}");
        });

        app.post("/user", ctx -> {
            try {
                RegisterRequest request = gson.fromJson(ctx.body(), RegisterRequest.class);

                UserData user = new UserData(
                        request.username(),
                        request.password(),
                        request.email()
                );

                AuthData auth = userService.register(user);

                ctx.status(200);
                ctx.json(new AuthResult(auth.username(), auth.authToken()));

            } catch (DataAccessException e) {
                if (e.getMessage().equals("bad request")) {
                    ctx.status(400);
                    ctx.json(new ErrorResult("Error: bad request"));
                } else if (e.getMessage().equals("already taken")) {
                    ctx.status(403);
                    ctx.json(new ErrorResult("Error: already taken"));
                } else {
                    ctx.status(500);
                    ctx.json(new ErrorResult("Error: " + e.getMessage()));
                }
            }
        });

        app.post("/session", ctx -> {
            try {
                LoginRequest request = gson.fromJson(ctx.body(), LoginRequest.class);

                AuthData auth = userService.login(request.username(), request.password());

                ctx.status(200);
                ctx.json(new AuthResult(auth.username(), auth.authToken()));

            } catch (DataAccessException e) {
                if (e.getMessage().equals("bad request")) {
                    ctx.status(400);
                    ctx.json(new ErrorResult("Error: bad request"));
                } else if (e.getMessage().equals("unauthorized")) {
                    ctx.status(401);
                    ctx.json(new ErrorResult("Error: unauthorized"));
                } else {
                    ctx.status(500);
                    ctx.json(new ErrorResult("Error: " + e.getMessage()));
                }
            }
        });

        app.delete("/session", ctx -> {
            try {
                String authToken = ctx.header("authorization");

                userService.logout(authToken);

                ctx.status(200);
                ctx.result("{}");

            } catch (DataAccessException e) {
                if (e.getMessage().equals("unauthorized")) {
                    ctx.status(401);
                    ctx.json(new ErrorResult("Error: unauthorized"));
                } else {
                    ctx.status(500);
                    ctx.json(new ErrorResult("Error: " + e.getMessage()));
                }
            }
        });

        app.start(desiredPort);
        return app.port();
    }
}