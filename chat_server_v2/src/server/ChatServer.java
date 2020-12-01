package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private ServerSocketChannel server;
    private final int port;
    public final ExecutorService pool;
//    private final Map<Integer, Client> users = new ConcurrentHashMap<>();
    private final Map<Integer, Client> freeUsers = new ConcurrentHashMap<>();
//    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Room> availableRooms = new ConcurrentHashMap<>();
    public final Selector selector;

    public ChatServer(int port) throws IOException {
        this.port = port;
//        pool = Executors.newFixedThreadPool(400); //todo maybe change to cachedThreadPool or ThreadPoolExecutor
        pool = Executors.newCachedThreadPool();
        this.selector = Selector.open();
    }

    public void startServer() {
        try {
            this.server = ServerSocketChannel.open();
            this.server.bind(new InetSocketAddress("localhost", this.port));
            System.out.println("Server started at port " + this.port + ".");
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Iterator<SelectionKey> keyIterator;
        SelectionKey key;

        try {
            while (server.isOpen()) {
                int selected = selector.selectNow();
                if (selected == 0)
                    continue;
                keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    key = keyIterator.next();
                    keyIterator.remove();

                    if (key.isAcceptable()) {
                        SocketChannel channel = server.accept();
                        channel.configureBlocking(true);
                        int port = channel.socket().getPort();
                        System.out.println("Socket accepted: " + port);
                        pool.execute(new ClientAcceptor(this, channel, port));
                    }

                    if (key.isReadable()) {
                        //read message and send to user or room
                        pool.execute(new MessageTransmitter(key, this));
                    }
                }

            }
        } catch (CancelledKeyException e) {
            System.out.println("Disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public Client addUser(int id, Client client) {
//        return users.put(id, client);
//    }

//    public boolean removeUser(int id, Client client) {
//        return users.remove(id, client);
//    }

    public void addFreeUser(Client client) {
        freeUsers.put(client.getId(), client);
    }

    public void removeFreeUser(int id) {
        freeUsers.remove(id);
    }

    public Client getFreeUser(int id) {
        return freeUsers.get(id);
    }

    public Room getAvailableRoom(String roomName) {
        return availableRooms.get(roomName);
    }


    public String printFreeUsers(int id) {
        if (freeUsers.size() == 0)
            return "No free users.";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Client> entry : freeUsers.entrySet()) {
            if (entry.getKey() != id) {
                sb.append(entry.getValue().toString());
                sb.append("; ");
            }
        }
        sb.setCharAt(sb.length() - 2, '.');
        sb.setCharAt(sb.length() - 1, '\n');
//        System.out.println(sb.toString());
        return sb.toString();
    }

    public String printRooms() {
        if (availableRooms.size() == 0)
            return "No rooms available.\n";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Room> entry : availableRooms.entrySet()) {
            sb.append(entry.getValue().toString());
            sb.append("; ");
        }
        sb.setCharAt(sb.length() - 2, '.');
        sb.setCharAt(sb.length() - 1, '\n');
//        System.out.println(sb.toString());
        return sb.toString();
    }

    public boolean hasFreeUsers() {
        return freeUsers.size() > 0;
    }

    public boolean hasAvailableRooms() {
        return availableRooms.size() > 0;
    }

    public void addAvailableRoom(Room room) {
        availableRooms.put(room.getRoomName(), room);
    }

    public void removeAvailableRoom(Room room) {
        availableRooms.remove(room.getRoomName());
    }
}
