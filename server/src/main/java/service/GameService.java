package service;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GameService {

    private final DataAccess dataAccess;
    private final Set<Integer> resignedGames = new HashSet<>();

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

    public AuthData validateAuth(String authToken) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(authToken);

        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        return auth;
    }

    public GameData connect(String authToken, int gameID) throws DataAccessException {
        validateAuth(authToken);

        GameData game = dataAccess.getGame(gameID);

        if (game == null) {
            throw new DataAccessException("bad request");
        }

        return game;
    }

    public GameData makeMove(String authToken, int gameID, ChessMove move) throws DataAccessException {
        AuthData auth = validateAuth(authToken);
        GameData gameData = getExistingGame(gameID);

        if (move == null) {
            throw new DataAccessException("bad request");
        }

        if (isGameOver(gameData.game()) || resignedGames.contains(gameID)) {
            throw new DataAccessException("game is over");
        }

        ChessGame.TeamColor playerColor = getPlayerColor(auth.username(), gameData);

        if (playerColor == null) {
            throw new DataAccessException("unauthorized");
        }

        if (gameData.game().getTeamTurn() != playerColor) {
            throw new DataAccessException("wrong turn");
        }

        ChessPiece piece = gameData.game().getBoard().getPiece(move.getStartPosition());

        if (piece == null || piece.getTeamColor() != playerColor) {
            throw new DataAccessException("invalid move");
        }

        try {
            gameData.game().makeMove(move);
        } catch (Exception e) {
            throw new DataAccessException("invalid move");
        }

        dataAccess.updateGame(gameData);
        return gameData;
    }

    public GameData resign(String authToken, int gameID) throws DataAccessException {
        AuthData auth = validateAuth(authToken);
        GameData gameData = getExistingGame(gameID);

        if (resignedGames.contains(gameID) || isGameOver(gameData.game())) {
            throw new DataAccessException("game is over");
        }

        ChessGame.TeamColor playerColor = getPlayerColor(auth.username(), gameData);

        if (playerColor == null) {
            throw new DataAccessException("unauthorized");
        }

        resignedGames.add(gameID);
        return gameData;
    }

    public String leave(String authToken, int gameID) throws DataAccessException {
        AuthData auth = validateAuth(authToken);
        GameData game = getExistingGame(gameID);

        String whiteUsername = game.whiteUsername();
        String blackUsername = game.blackUsername();

        if (auth.username().equals(whiteUsername)) {
            whiteUsername = null;
        }

        if (auth.username().equals(blackUsername)) {
            blackUsername = null;
        }

        GameData updatedGame = new GameData(
                game.gameID(),
                whiteUsername,
                blackUsername,
                game.gameName(),
                game.game()
        );

        dataAccess.updateGame(updatedGame);

        return auth.username();
    }

    public ChessGame.TeamColor getPlayerColor(String username, GameData game) {
        if (username.equals(game.whiteUsername())) {
            return ChessGame.TeamColor.WHITE;
        }

        if (username.equals(game.blackUsername())) {
            return ChessGame.TeamColor.BLACK;
        }

        return null;
    }

    public boolean isInCheck(GameData gameData) {
        return gameData.game().isInCheck(gameData.game().getTeamTurn());
    }

    public boolean isInCheckmate(GameData gameData) {
        return gameData.game().isInCheckmate(gameData.game().getTeamTurn());
    }

    private GameData getExistingGame(int gameID) throws DataAccessException {
        GameData game = dataAccess.getGame(gameID);

        if (game == null) {
            throw new DataAccessException("bad request");
        }

        return game;
    }

    private boolean isGameOver(ChessGame game) {
        return game.isInCheckmate(ChessGame.TeamColor.WHITE)
                || game.isInCheckmate(ChessGame.TeamColor.BLACK);
    }
}