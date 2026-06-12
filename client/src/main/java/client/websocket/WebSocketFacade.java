package client.websocket;

import com.google.gson.Gson;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import websocket.commands.UserGameCommand;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebSocketFacade extends Endpoint {

    private final Gson gson = new Gson();
    private final String serverUrl;
    private final ServerMessageObserver observer;
    private final ScheduledExecutorService keepAlive = Executors.newSingleThreadScheduledExecutor();

    private Session session;

    public WebSocketFacade(String serverUrl, ServerMessageObserver observer) {
        this.serverUrl = serverUrl;
        this.observer = observer;
    }

    public void connect(String authToken, int gameID) throws Exception {
        URI uri = new URI(serverUrl.replace("http", "ws") + "/ws");

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();

        session = container.connectToServer(this, config, uri);

        send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID));
        startKeepAlive();
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
        keepAlive.shutdownNow();

        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                if (observer != null) {
                    observer.notify(message);
                }
            }
        });
    }

    private void startKeepAlive() {
        keepAlive.scheduleAtFixedRate(() -> {
            try {
                if (session != null && session.isOpen()) {
                    session.getBasicRemote().sendPing(ByteBuffer.wrap(new byte[0]));
                }
            } catch (Exception ignored) {
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void send(UserGameCommand command) throws Exception {
        if (session == null || !session.isOpen()) {
            throw new IllegalStateException("WebSocket connection is closed.");
        }

        session.getBasicRemote().sendText(gson.toJson(command));
    }
}