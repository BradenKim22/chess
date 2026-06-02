package client;

import client.repl.Repl;

public class ClientMain {
    public static void main(String[] args) {
        String serverUrl = "http://localhost:8080";
        new Repl(serverUrl).run();
    }
}