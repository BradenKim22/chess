package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class MySQLDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public MySQLDataAccess() throws DataAccessException {
        DatabaseManager.createDatabase();
        createTables();
    }

    private void createTables() throws DataAccessException {
        String[] createStatements = {
                """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(255) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    PRIMARY KEY (username)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS auths (
                    authToken VARCHAR(255) NOT NULL,
                    username VARCHAR(255) NOT NULL,
                    PRIMARY KEY (authToken)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS games (
                    gameID INT NOT NULL AUTO_INCREMENT,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255) NOT NULL,
                    game TEXT NOT NULL,
                    PRIMARY KEY (gameID)
                )
                """
        };

        try (Connection conn = DatabaseManager.getConnection()) {
            for (String statement : createStatements) {
                try (PreparedStatement preparedStatement = conn.prepareStatement(statement)) {
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("failed to create tables", e);
        }
    }

    @Override
    public void clear() throws DataAccessException {
        String[] clearStatements = {
                "DELETE FROM auths",
                "DELETE FROM games",
                "DELETE FROM users"
        };

        try (Connection conn = DatabaseManager.getConnection()) {
            for (String statement : clearStatements) {
                try (PreparedStatement preparedStatement = conn.prepareStatement(statement)) {
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("failed to clear database", e);
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        String sql = """
            INSERT INTO users (username, password, email)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, user.username());
            statement.setString(2, user.password());
            statement.setString(3, user.email());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("failed to create user", e);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = """
            SELECT username, password, email
            FROM users
            WHERE username = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email")
                    );
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("failed to get user", e);
        }

        return null;
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        String sql = """
            INSERT INTO auths (authToken, username)
            VALUES (?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, auth.authToken());
            statement.setString(2, auth.username());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("failed to create auth", e);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = """
            SELECT authToken, username
            FROM auths
            WHERE authToken = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, authToken);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("authToken"),
                            rs.getString("username")
                    );
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("failed to get auth", e);
        }

        return null;
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = """
            DELETE FROM auths
            WHERE authToken = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, authToken);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("failed to delete auth", e);
        }
    }

    @Override
    public int createGame(String gameName) throws DataAccessException {
        String sql = """
            INSERT INTO games (whiteUsername, blackUsername, gameName, game)
            VALUES (?, ?, ?, ?)
            """;

        ChessGame chessGame = new ChessGame();
        String gameJson = gson.toJson(chessGame);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, null);
            statement.setString(2, null);
            statement.setString(3, gameName);
            statement.setString(4, gameJson);

            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("failed to create game", e);
        }

        throw new DataAccessException("failed to create game");
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return null;
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        return null;
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {

    }
}