package chattest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class Main {

    public static int PORT = 8080;
    public static int roomCapacity = 10;
    public static void main(String[] args) {    //todo maybe, add parsing of args to set port number and single room capacity

        System.out.println("Connecting to chat server at port " + Main.PORT);
        InetSocketAddress address = new InetSocketAddress("localhost", Main.PORT);
        RoomGenerator generator = new RoomGenerator(address);
        generator.start();

    }
}
