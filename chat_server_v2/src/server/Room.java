package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Room {

    private final ChatServer server;
    private final String roomName;
    public final List<Client> clients;
    public final int capacity;

    public Room(String name, int capacity, ChatServer server) {
        this.server = server;
        this.capacity = capacity;
        this.clients = new ArrayList<>(capacity);
        this.roomName = name;
        System.out.println("Room '" + roomName + "' was created. Capacity: " + this.capacity);
    }

    public void sendRoomMessage(Client sender, ByteBuffer buffer) throws IOException {
        for (Client client : clients) {
            if (client != sender) {
                client.sendMessage(buffer);
            }
        }
    }

//    public synchronized boolean isFull() {
//        return clients.size() == capacity;
//    }

    public void addUser(Client client) {
        clients.add(client);
        if (clients.size() == capacity)
            server.removeAvailableRoom(this);
    }

    public void removeUser(Client client) {
        clients.remove(client);
        if (clients.size() + 1 == capacity) {
            server.addAvailableRoom(this);
        }
    }

    public String getRoomName() {
        return this.roomName;
    }

    @Override
    public String toString() {
        return "Room: '" + this.roomName + "', Users: " + this.clients.size() + " of " + this.capacity;
    }
}
