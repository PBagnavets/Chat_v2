package server;

import java.io.IOException;

public class Main {

    private static final int port = 8080;
    public static void main(String[] args) {
        try {
            ChatServer chatServer = new ChatServer(port);
            chatServer.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
