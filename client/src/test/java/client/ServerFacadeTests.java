package client;

import chess.ChessGame;
import client.server.ResponseException;
import client.server.ServerFacade;
import org.junit.jupiter.api.*;
import result.AuthResult;
import server.Server;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);

        facade = new ServerFacade("http://localhost:" + port);

        System.out.println("Started test HTTP server on " + port);
    }

    @BeforeEach
    public void clearDatabase() throws ResponseException {
        facade.clear();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    public void sampleTest() {
        Assertions.assertTrue(true);
    }

    @Test
    public void registerSuccess() throws ResponseException {
        AuthResult result = facade.register("bob", "password", "bob@email.com");

        Assertions.assertEquals("bob", result.username());
        Assertions.assertNotNull(result.authToken());
    }

    @Test
    public void registerAlreadyTaken() throws ResponseException {
        facade.register("bob", "password", "bob@email.com");

        Assertions.assertThrows(ResponseException.class, () ->
                facade.register("bob", "password", "bob@email.com")
        );
    }

    @Test
    public void loginSuccess() throws ResponseException {
        facade.register("bob", "password", "bob@email.com");

        AuthResult result = facade.login("bob", "password");

        Assertions.assertEquals("bob", result.username());
        Assertions.assertNotNull(result.authToken());
    }

    @Test
    public void loginBadPassword() throws ResponseException {
        facade.register("bob", "password", "bob@email.com");

        Assertions.assertThrows(ResponseException.class, () ->
                facade.login("bob", "wrongPassword")
        );
    }

    @Test
    public void createGameSuccess() throws ResponseException {
        AuthResult auth = facade.register("bob", "password", "bob@email.com");

        var result = facade.createGame("My Game", auth.authToken());

        Assertions.assertTrue(result.gameID() > 0);
    }

    @Test
    public void createGameUnauthorized() {
        Assertions.assertThrows(ResponseException.class, () ->
                facade.createGame("My Game", "bad-token")
        );
    }

    @Test
    public void listGamesSuccess() throws ResponseException {
        AuthResult auth = facade.register("bob", "password", "bob@email.com");

        facade.createGame("My Game", auth.authToken());

        var result = facade.listGames(auth.authToken());

        Assertions.assertNotNull(result.games());
    }

    @Test
    public void listGamesUnauthorized() {
        Assertions.assertThrows(ResponseException.class, () ->
                facade.listGames("bad-token")
        );
    }

    @Test
    public void logoutSuccess() throws ResponseException {
        AuthResult auth = facade.register("bob", "password", "bob@email.com");

        facade.logout(auth.authToken());

        Assertions.assertThrows(ResponseException.class, () ->
                facade.listGames(auth.authToken())
        );
    }

    @Test
    public void logoutUnauthorized() {
        Assertions.assertThrows(ResponseException.class, () ->
                facade.logout("bad-token")
        );
    }

    @Test
    public void joinGameSuccess() throws ResponseException {
        AuthResult auth = facade.register("bob", "password", "bob@email.com");

        var game = facade.createGame("My Game", auth.authToken());

        facade.joinGame(game.gameID(), ChessGame.TeamColor.WHITE, auth.authToken());
    }

    @Test
    public void joinGameUnauthorized() throws ResponseException {
        AuthResult auth = facade.register("bob", "password", "bob@email.com");

        var game = facade.createGame("My Game", auth.authToken());

        Assertions.assertThrows(ResponseException.class, () ->
                facade.joinGame(game.gameID(), ChessGame.TeamColor.WHITE, "bad-token")
        );
    }

    @Test
    public void joinGameAlreadyTaken() throws ResponseException {
        AuthResult bob = facade.register("bob", "password", "bob@email.com");

        var game = facade.createGame("My Game", bob.authToken());

        facade.joinGame(game.gameID(), ChessGame.TeamColor.WHITE, bob.authToken());
        facade.logout(bob.authToken());

        AuthResult amy = facade.register("amy", "password", "amy@email.com");

        Assertions.assertThrows(ResponseException.class, () ->
                facade.joinGame(game.gameID(), ChessGame.TeamColor.WHITE, amy.authToken())
        );
    }
}