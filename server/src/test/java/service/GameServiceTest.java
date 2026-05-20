package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {

    private DataAccess dataAccess;
    private UserService userService;
    private GameService gameService;
    private AuthData auth;

    @BeforeEach
    public void setUp() throws DataAccessException {
        dataAccess = new MemoryDataAccess();
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);

        UserData user = new UserData("bob", "password", "bob@email.com");
        auth = userService.register(user);
    }

    @Test
    public void listGamesSuccess() throws DataAccessException {
        gameService.createGame(auth.authToken(), "Game One");
        gameService.createGame(auth.authToken(), "Game Two");

        Collection<GameData> games = gameService.listGames(auth.authToken());

        assertEquals(2, games.size());
    }

    @Test
    public void listGamesUnauthorizedFails() {
        assertThrows(DataAccessException.class, () -> {
            gameService.listGames("bad-token");
        });
    }

    @Test
    public void createGameSuccess() throws DataAccessException {
        int gameID = gameService.createGame(auth.authToken(), "My Game");

        GameData game = dataAccess.getGame(gameID);

        assertNotNull(game);
        assertEquals("My Game", game.gameName());
    }

    @Test
    public void createGameUnauthorizedFails() {
        assertThrows(DataAccessException.class, () -> {
            gameService.createGame("bad-token", "My Game");
        });
    }

    @Test
    public void joinGameWhiteSuccess() throws DataAccessException {
        int gameID = gameService.createGame(auth.authToken(), "My Game");

        gameService.joinGame(auth.authToken(), "WHITE", gameID);

        GameData game = dataAccess.getGame(gameID);

        assertEquals("bob", game.whiteUsername());
        assertNull(game.blackUsername());
    }

    @Test
    public void joinGameAlreadyTakenFails() throws DataAccessException {
        int gameID = gameService.createGame(auth.authToken(), "My Game");

        gameService.joinGame(auth.authToken(), "WHITE", gameID);

        UserData secondUser = new UserData("sue", "password", "sue@email.com");
        AuthData secondAuth = userService.register(secondUser);

        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame(secondAuth.authToken(), "WHITE", gameID);
        });
    }

    @Test
    public void joinGameBadGameIDFails() {
        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame(auth.authToken(), "WHITE", 999);
        });
    }

    @Test
    public void joinGameBadColorFails() throws DataAccessException {
        int gameID = gameService.createGame(auth.authToken(), "My Game");

        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame(auth.authToken(), "GREEN", gameID);
        });
    }
}