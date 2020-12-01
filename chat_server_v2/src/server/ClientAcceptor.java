package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientAcceptor implements Runnable {

    private final SocketChannel channel;
    private final ChatServer server;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(128);
    private final int id;
    private static final ByteBuffer enterYourName = ByteBuffer.wrap("Enter your name: ".getBytes());

    public ClientAcceptor(ChatServer server, SocketChannel client, int port) {
        this.server = server;
        this.channel = client;
        this.id = port;
    }

    @Override
    public void run() {
        String name;
        try {
            while (enterYourName.hasRemaining())
                channel.write(enterYourName);
            enterYourName.rewind();
            readBuffer.clear();
            channel.read(readBuffer);
            name = new String(readBuffer.array()).trim();
//            System.out.println("New user: " + name);
            Client newUser = new Client(id, name, channel);
//            server.addUser(id, newUser);
            //server.addFreeUser(id);
            //try to get companion or room
            server.pool.execute(new ChatSelector(this.server, this.channel, newUser));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
