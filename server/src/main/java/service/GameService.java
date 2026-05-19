package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

import java.util.Collection;

public class GameService {

    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public Collection<GameData> listGames(String authToken) throws DataAccessException {
        if (dataAccess.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }

        return dataAccess.listGames();
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {
        if (dataAccess.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }

        if (gameName == null) {
            throw new DataAccessException("bad request");
        }

        return dataAccess.createGame(gameName);
    }
}