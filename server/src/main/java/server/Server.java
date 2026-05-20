package server;

import com.google.gson.Gson;
import dataaccess.MemoryDataAccess;
import io.javalin.Javalin;
import service.ClearService;
import service.GameService;
import service.UserService;

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

        app.start(desiredPort);
        return app.port();
    }
}