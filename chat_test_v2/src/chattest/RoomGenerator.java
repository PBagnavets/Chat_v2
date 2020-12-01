package chattest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class RoomGenerator extends Thread {

    private final InetSocketAddress address;
    private static int userId = 0;
    private static int roomId = 0;
    private final int roomCapacity = Main.roomCapacity;

    private final Map<String, SocketChannel> roommates = new HashMap<>();

    private static final int time = 30;

    public RoomGenerator(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public void run() {
        while (true) {
            try {
                SocketChannel client = SocketChannel.open(address);
                //send user name

                sleep(time);

                ByteBuffer buffer = ByteBuffer.wrap(("user" + userId).getBytes());
                while (buffer.hasRemaining())
                    client.write(buffer);
                sleep(time);

                roommates.put("user" + userId, client);

                //create room
                buffer.clear();
                buffer = ByteBuffer.wrap(("#c room" + roomId + " " + Main.roomCapacity).getBytes());
                while (buffer.hasRemaining())
                    client.write(buffer);
                sleep(time);

                roommates.put("user" + userId, client);
                userId++;

                sleep(time);
                //create 9 more roommates
                createUsers();

                roomId++;
                RoomChat chat = new RoomChat(roommates);
                roommates.clear();
                System.out.println("Users connected: " + userId + ", Message sent: " + RoomChat.messageSent.get());
                chat.start();

            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createUsers() throws IOException, InterruptedException {
        for (int i = 1; i < Main.roomCapacity; i++) {

            sleep(time);
            SocketChannel client = SocketChannel.open(address);
            //send user name

            ByteBuffer buffer = ByteBuffer.wrap(("user" + userId).getBytes());
            while (buffer.hasRemaining())
                client.write(buffer);
            sleep(time);

            roommates.put("user" + userId, client);

            //join room
            buffer.clear();
            buffer = ByteBuffer.wrap(("#j room" + roomId).getBytes());
            while (buffer.hasRemaining())
                client.write(buffer);
            sleep(time);

            userId++;
            sleep(time);
        }
    }

}
