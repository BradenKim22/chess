package service;

import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private DataAccess dataAccess;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        dataAccess = new MemoryDataAccess();
        userService = new UserService(dataAccess);
    }

    @Test
    public void registerSuccess() throws DataAccessException {
        UserData user = new UserData("bob", "password", "bob@email.com");

        AuthData auth = userService.register(user);

        assertNotNull(auth.authToken());
        assertEquals("bob", auth.username());
        assertNotNull(dataAccess.getUser("bob"));
        assertNotNull(dataAccess.getAuth(auth.authToken()));
    }

    @Test
    public void registerDuplicateUsernameFails() throws DataAccessException {
        UserData user = new UserData("bob", "password", "bob@email.com");

        userService.register(user);

        assertThrows(DataAccessException.class, () -> {
            userService.register(user);
        });
    }

    @Test
    public void loginSuccess() throws DataAccessException {
        UserData user = new UserData("bob", "password", "bob@email.com");

        userService.register(user);

        AuthData auth = userService.login("bob", "password");

        assertNotNull(auth.authToken());
        assertEquals("bob", auth.username());
    }

    @Test
    public void loginWrongPasswordFails() throws DataAccessException {
        UserData user = new UserData("bob", "password", "bob@email.com");

        userService.register(user);

        assertThrows(DataAccessException.class, () -> {
            userService.login("bob", "wrongpassword");
        });
    }

    @Test
    public void logoutSuccess() throws DataAccessException {
        UserData user = new UserData("bob", "password", "bob@email.com");

        AuthData auth = userService.register(user);

        userService.logout(auth.authToken());

        assertNull(dataAccess.getAuth(auth.authToken()));
    }

    @Test
    public void logoutBadTokenFails() {
        assertThrows(DataAccessException.class, () -> {
            userService.logout("bad-token");
        });
    }
}