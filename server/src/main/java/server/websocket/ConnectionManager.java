package server.websocket;

import java.util.HashSet;
import java.util.Set;

public class ConnectionManager {

    private final Set<Connection> connections = new HashSet<>();

    public void add(Connection connection) {
        connections.add(connection);
    }

    public void remove(Connection connection) {
        connections.remove(connection);
    }

    public void removeBySession(String sessionId) {
        connections.removeIf(connection ->
                connection.session().sessionId().equals(sessionId)
        );
    }

    public Connection getBySession(String sessionId) {
        for (Connection connection : connections) {
            if (connection.session().sessionId().equals(sessionId)) {
                return connection;
            }
        }

        return null;
    }

    public Set<Connection> getConnectionsForGame(int gameID) {
        Set<Connection> result = new HashSet<>();

        for (Connection connection : connections) {
            if (connection.gameID() == gameID) {
                result.add(connection);
            }
        }

        return result;
    }
}