package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client {

    private final int id;
    public final SocketChannel channel;
    private final String name;
    public boolean hasCompanion = false;
    public boolean isInRoom = false;
    public SelectionKey key;

    public Client companion = null;
    public List<Room> rooms = null;

    public Client(int id, String name, SocketChannel socket) {
        this.id = id;
        this.name = name;
        this.channel = socket;
    }

    //Sends message FROM this user
    public void sendUserMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        if (this.hasCompanion) {
            this.companion.sendMessage(buffer);
        } else if (this.isInRoom) {
            for (Room room : rooms)
                room.sendRoomMessage(this, buffer);
        }
    }

    //Sends message TO this user
    public void sendMessage(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            this.channel.write(buffer);
        }
        buffer.rewind();
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return this.id;
    }

    public void addRoom(Room room) {
        if (this.rooms == null)
            this.rooms = new ArrayList<>();
        this.rooms.add(room);
        room.addUser(this);
    }

    public void removeRooms() {
        rooms = null;
    }

    @Override
    public String toString() {
        return "ID '" + this.id + "' , Name: " + this.name;
    }

}
