package server.websocket;

import io.javalin.websocket.WsContext;

public class Connection {

    private final String username;
    private final int gameID;
    private final WsContext session;

    public Connection(String username, int gameID, WsContext session) {
        this.username = username;
        this.gameID = gameID;
        this.session = session;
    }

    public String username() {
        return username;
    }

    public int gameID() {
        return gameID;
    }

    public WsContext session() {
        return session;
    }

    public void send(String message) {
        session.send(message);
    }
}