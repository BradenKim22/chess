package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {

    private DataAccess dataAccess;
    private UserService userService;
    private GameService gameService;
    private ClearService clearService;

    @BeforeEach
    public void setUp() {
        dataAccess = new MemoryDataAccess();
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);
        clearService = new ClearService(dataAccess);
    }

    @Test
    public void clearSuccess() throws DataAccessException {
        UserData user = new UserData("bob", "password", "bob@email.com");

        AuthData auth = userService.register(user);

        gameService.createGame(auth.authToken(), "My Game");

        clearService.clear();

        assertNull(dataAccess.getUser("bob"));
        assertNull(dataAccess.getAuth(auth.authToken()));
        assertTrue(dataAccess.listGames().isEmpty());
    }
}