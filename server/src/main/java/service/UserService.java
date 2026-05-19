package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;

import java.util.UUID;

public class UserService {

    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public AuthData register(UserData user) throws DataAccessException {
        if (user.username() == null || user.password() == null || user.email() == null) {
            throw new DataAccessException("bad request");
        }

        if (dataAccess.getUser(user.username()) != null) {
            throw new DataAccessException("already taken");
        }

        dataAccess.createUser(user);

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, user.username());

        dataAccess.createAuth(auth);

        return auth;
    }

    public AuthData login(String username, String password) throws DataAccessException {
        if (username == null || password == null) {
            throw new DataAccessException("bad request");
        }

        UserData user = dataAccess.getUser(username);

        if (user == null || !user.password().equals(password)) {
            throw new DataAccessException("unauthorized");
        }

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, username);

        dataAccess.createAuth(auth);

        return auth;
    }

    public void logout(String authToken) throws DataAccessException {
        if (dataAccess.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }

        dataAccess.deleteAuth(authToken);
    }
}