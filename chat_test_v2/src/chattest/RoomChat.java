package chattest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomChat extends Thread {

    private final  ByteBuffer buffer = ByteBuffer.wrap("Hi!!!".getBytes());
    public static AtomicInteger messageSent = new AtomicInteger(0);
    private final  Map<String, SocketChannel> roommates;
    private static int waitTime = 5000 / Main.roomCapacity;

    public RoomChat(Map<String, SocketChannel> roommates) {
        this.roommates = new HashMap<>(roommates);
    }
    @Override
    public void run() {
        try {
            while (true) {
                for (Map.Entry<String, SocketChannel> entry : roommates.entrySet()) {
                    while (buffer.hasRemaining())
                        entry.getValue().write(buffer);
                    buffer.rewind();
                    sleep(waitTime);
                }
                messageSent.updateAndGet(n -> n + Main.roomCapacity);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
