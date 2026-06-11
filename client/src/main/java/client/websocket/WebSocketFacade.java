package client.websocket;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;

import jakarta.websocket.*;
import java.net.URI;

public class WebSocketFacade extends Endpoint {

    private final Gson gson = new Gson();
    private final String serverUrl;
    private final ServerMessageObserver observer;

    private Session session;

    public WebSocketFacade(String serverUrl, ServerMessageObserver observer) {
        this.serverUrl = serverUrl;
        this.observer = observer;
    }

    public void connect(String authToken, int gameID) throws Exception {
        URI uri = new URI(serverUrl.replace("http", "ws") + "/ws");

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        session = container.connectToServer(this, uri);

        send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID));
    }

    public void makeMove(UserGameCommand command) throws Exception {
        send(command);
    }

    public void leave(String authToken, int gameID) throws Exception {
        send(new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID));
    }

    public void resign(String authToken, int gameID) throws Exception {
        send(new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID));
    }

    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;

        session.addMessageHandler((MessageHandler.Whole<String>) message -> {
            if (observer != null) {
                observer.notify(message);
            }
        });
    }

    private void send(UserGameCommand command) throws Exception {
        session.getBasicRemote().sendText(gson.toJson(command));
    }
}