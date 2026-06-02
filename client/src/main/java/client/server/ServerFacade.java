package client.server;

import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;

public class ServerFacade {

    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    private <T> T makeRequest(String method, String path, Object request, Class<T> responseClass,
                              String authToken) throws ResponseException {
        try {
            URI uri = new URI(serverUrl + path);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

            connection.setRequestMethod(method);
            connection.setDoOutput(request != null);

            if (authToken != null) {
                connection.addRequestProperty("authorization", authToken);
            }

            if (request != null) {
                connection.addRequestProperty("Content-Type", "application/json");

                try (OutputStream requestBody = connection.getOutputStream()) {
                    requestBody.write(gson.toJson(request).getBytes());
                }
            }

            connection.connect();

            int statusCode = connection.getResponseCode();

            if (statusCode / 100 == 2) {
                return readResponse(connection, responseClass);
            }

            throw new ResponseException(statusCode, readError(connection));

        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseException(500, e.getMessage());
        }
    }

    private <T> T readResponse(HttpURLConnection connection, Class<T> responseClass) throws IOException {
        if (responseClass == null) {
            return null;
        }

        try (InputStream responseBody = connection.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(responseBody);
            return gson.fromJson(reader, responseClass);
        }
    }

    private String readError(HttpURLConnection connection) throws IOException {
        InputStream errorStream = connection.getErrorStream();

        if (errorStream == null) {
            return "Error: request failed";
        }

        try (InputStreamReader reader = new InputStreamReader(errorStream)) {
            return gson.fromJson(reader, Object.class).toString();
        }
    }
}