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

    public void joinGame(String authToken, String playerColor, int gameID) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(authToken);

        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        GameData game = dataAccess.getGame(gameID);

        if (game == null || playerColor == null) {
            throw new DataAccessException("bad request");
        }

        String whiteUsername = game.whiteUsername();
        String blackUsername = game.blackUsername();

        if (playerColor.equals("WHITE")) {
            if (whiteUsername != null) {
                throw new DataAccessException("already taken");
            }

            whiteUsername = auth.username();
        } else if (playerColor.equals("BLACK")) {
            if (blackUsername != null) {
                throw new DataAccessException("already taken");
            }

            blackUsername = auth.username();
        } else {
            throw new DataAccessException("bad request");
        }

        GameData updatedGame = new GameData(
                game.gameID(),
                whiteUsername,
                blackUsername,
                game.gameName(),
                game.game()
        );

        dataAccess.updateGame(updatedGame);
    }
}