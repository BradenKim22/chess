package client.websocket;

public interface ServerMessageObserver {
    void notify(String message);
}