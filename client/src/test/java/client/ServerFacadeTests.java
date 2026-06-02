package client;

import client.server.ResponseException;
import client.server.ServerFacade;
import org.junit.jupiter.api.*;
import result.AuthResult;
import server.Server;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);

        facade = new ServerFacade("http://localhost:" + port);

        System.out.println("Started test HTTP server on " + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    public void sampleTest() {
        Assertions.assertTrue(true);
    }

    @Test
    public void registerSuccess() throws ResponseException {
        AuthResult result =
                facade.register(
                        "bob",
                        "password",
                        "bob@email.com"
                );

        Assertions.assertEquals("bob", result.username());
        Assertions.assertNotNull(result.authToken());
    }
}