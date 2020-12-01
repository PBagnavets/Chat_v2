package chatclient;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

public class ChatClient {

    private int port;

    private final Set<String> availableRooms = new HashSet<>();
    private final Set<String> joinedRooms = new HashSet<>();
    private final Set<String> users = new HashSet<>();
    private boolean chatting = false;
    private boolean inPrivateChat = false;
    private boolean noUsers = true;
    private boolean noRooms = true;
    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public ChatClient(int port) {
        this.port = port;
    }


    public static void main(String[] args) {
        int port = 8080;
//        if (args.length == 1) {
//            port = Integer.parseInt(args[0]);
//        }
        ChatClient client = new ChatClient(port);
        client.startClient();
    }

    private void startClient() {
        try {
            System.out.println("Connecting to chat server at port " + port);
            System.out.println("To see list of available commands type \"#h\"");
            InetSocketAddress address = new InetSocketAddress("localhost", port);
            SocketChannel channel = SocketChannel.open(address);
            ByteBuffer bb = ByteBuffer.allocate(256);
            //read message "Enter your name:"
            channel.read(bb);
            String message = new String(bb.array()).trim();
            System.out.println(message);

            //send your nickname
            String userName = reader.readLine();
            ByteBuffer buffer = ByteBuffer.wrap(userName.getBytes());
            while (buffer.hasRemaining())
                channel.write(buffer);

            channel.configureBlocking(false);

            ReadThread readThread = new ReadThread(channel, userName, this);
            WriteThread writeThread = new WriteThread(channel, userName, reader, this);

            readThread.start();
            writeThread.start();

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public boolean hasRoom(String roomName) {
        return availableRooms.contains(roomName);
    }

    public boolean hasUser(String userID) {
        return users.contains(userID);
    }

    public boolean hasNoRooms() {
        return noRooms;
    }

    public boolean hasNoUsers() {
        return noUsers;
    }

    public void setNoRooms(boolean noRooms) {
        this.noRooms = noRooms;
    }

    public void setNoUsers(boolean noUsers) {
        this.noUsers = noUsers;
    }

    public boolean isChatting() {
        return chatting;
    }

    public boolean isInPrivateChat() {
        return inPrivateChat;
    }

    public void setChatting(boolean chatting) {
        this.chatting = chatting;
    }

    public void setInPrivateChat(boolean inPrivateChat) {
        this.inPrivateChat = inPrivateChat;
    }

    public void addUser(String userID) {
        users.add(userID);
//        System.out.println("USER ADDED: " + userID);
    }

    public void addRoom(String roomName) {
        availableRooms.add(roomName);
//        System.out.println("ROOM ADDED: " + roomName);
    }

    public void addJoinedRoom(String roomName) {
        joinedRooms.add(roomName);
    }

    public void clearJoinedRooms() {
        joinedRooms.clear();
    }

    public void clearUsers() {
        users.clear();
    }

    public void clearRooms() {
        availableRooms.clear();
    }
}
