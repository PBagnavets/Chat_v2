package server;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChatSelector implements Runnable { //todo rewrite to non-blocking mode

    private final SocketChannel channel;
    private final ChatServer server;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(128);
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(128);
    private final Client client;
    private static final Lock lock = new ReentrantLock();
    private static final Condition noCompanion = lock.newCondition();


    //messages, wrapped into ByteBuffers
    private static final ByteBuffer enterCompanionName = ByteBuffer.wrap("Type \"#s <Free user ID>\" to start private chat\n".getBytes());
    private static final ByteBuffer waitCompanion = ByteBuffer.wrap("Type \"#w\" to wait for invite to private chat\n".getBytes());
    private static final ByteBuffer createRoom = ByteBuffer.wrap("Type \"#c <room_name> <room_capacity>\" to create room\n".getBytes());
    private static final ByteBuffer joinRoom = ByteBuffer.wrap("Type \"#j <room_name>\" to join room\n".getBytes());
    private static final ByteBuffer noFreeUsersAndRooms = ByteBuffer.wrap("There is no free users or available rooms\n".getBytes());
    private static final ByteBuffer freeUsers = ByteBuffer.wrap("Free users on server:\n".getBytes());
    private static final ByteBuffer freeRooms = ByteBuffer.wrap("Available rooms on server:\n".getBytes());
//    private static final ByteBuffer wrongCommand = ByteBuffer.wrap("Wrong command!".getBytes());

//    private static final String WAIT = "#wait";
//    private static final String CREATE = "#create";

    /*
    Commands from client :

        #c - create room
        #w - wait
        #j - join room
        #e - exit from room or private chat
        #r - show available rooms
        #s - select companion
        #q - quit from server

        #h - show list of commands. Implement on client side
     */

    public ChatSelector(ChatServer server, SocketChannel channel, Client user) {
        this.server = server;
        this.channel = channel;
        this.client = user;
    }

    /*
    - check free users and available room
        - if false, #w or #c
        - else #w or #c or (if rooms #j) or (if free users #s)
     */
    @Override
    public void run() {
        lock.lock();
        try {
            //print #w
            while (waitCompanion.hasRemaining())
                channel.write(waitCompanion);
            waitCompanion.rewind();

            //print #c
            while (createRoom.hasRemaining())
                channel.write(createRoom);
            createRoom.rewind();

            if (!server.hasFreeUsers() && ! server.hasAvailableRooms()) {
                //no free users and rooms
                noFreeUsersAndRooms();
            } else {
                //print free users and rooms
                freeUsersOrRooms();
            }
        } catch (SocketException e) {
            System.out.println("Client disconnected.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void noFreeUsersAndRooms() throws IOException, InterruptedException {
        String message;
        while (noFreeUsersAndRooms.hasRemaining())
            channel.write(noFreeUsersAndRooms);
        noFreeUsersAndRooms.rewind();

        //read from client and make decision
        readBuffer.clear();
        channel.read(readBuffer);
        message = new String(readBuffer.array()).trim();
        if (message.equals("#w")) {
            //lock
            waitForCompanion();
        } else if (message.startsWith("#c")) {
            //create room
            createRoom(message);

        } else {
            System.out.println("Client disconnected: " + client.getName());
        }
    }

    private void freeUsersOrRooms() throws IOException, InterruptedException{
        if (server.hasFreeUsers()) {
            //print users
            while (freeUsers.hasRemaining())
                channel.write(freeUsers);
            freeUsers.rewind();

            ByteBuffer buffer = ByteBuffer.wrap(server.printFreeUsers(client.getId()).getBytes());
            while (buffer.hasRemaining())
                channel.write(buffer);
            //print #s
            while (enterCompanionName.hasRemaining())
                channel.write(enterCompanionName);
            enterCompanionName.rewind();
        }
        if (server.hasAvailableRooms()) {
            //print rooms
            while (freeRooms.hasRemaining())
                channel.write(freeRooms);
            freeRooms.rewind();
            ByteBuffer buffer = ByteBuffer.wrap(server.printRooms().getBytes());
            while (buffer.hasRemaining())
                channel.write(buffer);
            //print #j
            while (joinRoom.hasRemaining())
                channel.write(joinRoom);
            joinRoom.rewind();
        }
        //read from client and make decision
        readBuffer.clear();
        channel.read(readBuffer);
        String message = new String(readBuffer.array()).trim();
        if (message.equals("#w")) {
            //lock
            waitForCompanion();
        } else if (message.startsWith("#c")){
            //create room
            createRoom(message);
        } else if (message.startsWith("#j")) {
            Room room = server.getAvailableRoom(message.substring(3));
            client.addRoom(room);
            client.isInRoom = true;
            client.sendMessage( ByteBuffer.wrap( ("You joined room " + room.getRoomName() ).getBytes() ) );
            room.sendRoomMessage(client, ByteBuffer.wrap( ("User " + client.getName() + " joined room.\n").getBytes() ) );
            addToSelector();
        } else if (message.startsWith("#s")) {
            Client freeUser = server.getFreeUser(Integer.parseInt(message.substring(3)));
            client.companion = freeUser;
            client.hasCompanion = true;
            freeUser.companion = client;
            freeUser.hasCompanion = true;
            server.removeFreeUser(freeUser.getId());
            noCompanion.signalAll();
            client.sendMessage( ByteBuffer.wrap( ("Start chatting with" + freeUser.getName()).getBytes() ) );
            freeUser.sendMessage( ByteBuffer.wrap( ("Start chatting with" + client.getName()).getBytes() ) );
            addToSelector();
        }
    }

    private void waitForCompanion() throws IOException, InterruptedException {
        server.addFreeUser(client);
        client.sendMessage(ByteBuffer.wrap("Wait for invite!\n".getBytes()));
        while (this.client.companion == null) {
            noCompanion.await();
        }
        server.removeFreeUser(client.getId());
        writeBuffer.clear();
        writeBuffer.put(("You are invited to chat with " + this.client.companion.getName()).getBytes());
        while (writeBuffer.hasRemaining())
            channel.write(writeBuffer);
        addToSelector();
    }

    public void createRoom(String message) throws IOException {
        StringTokenizer stringTokenizer = new StringTokenizer(message.substring(3), " ");
        Room newRoom = new Room(stringTokenizer.nextToken(), Integer.parseInt(stringTokenizer.nextToken()), server);
        client.sendMessage(ByteBuffer.wrap( ("You create room " + newRoom.getRoomName()).getBytes() ));
        client.isInRoom = true;
        client.addRoom(newRoom);
        server.addAvailableRoom(newRoom);
        server.removeFreeUser(client.getId());
        addToSelector();
    }


    public void addToSelector() throws IOException {
        this.channel.configureBlocking(false);
        this.client.key = this.channel.register(server.selector, SelectionKey.OP_READ, this.client);
    }

}
