package chatclient;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.StringTokenizer;

//read data from console, parse it, send messages to server
public class WriteThread extends Thread {

    private final ChatClient client;
    private final SocketChannel channel;
    private final String userName;
    private final BufferedReader console;

    private final String wrong = "Wrong request!";

    public WriteThread(SocketChannel channel, String userName, BufferedReader console, ChatClient client) {
        this.channel = channel;
        this.userName = userName;
        this.console = console;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while (channel.isConnected()) {
                String message = console.readLine();

                parseMessage(message);
               // System.out.println(userName + " >> " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
    private void parseMessage(String message) throws IOException {
        //starts with #
        if (message.startsWith("#")) { //starts with #
            handleCommand(message);
        } else {
            handleMessage(message);
        }
    }

    private void send(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        while (buffer.hasRemaining())
            channel.write(buffer);
    }

    private void handleCommand(String message) throws IOException {
        if (message.equals("#q")) {                                         //quit from server
            channel.close();
            System.exit(0);

        } else if (message.equals("#e")) {                                  //exit from current chats
            if (client.isChatting()) {
                client.setChatting(false);
                client.setInPrivateChat(false);
                client.clearJoinedRooms();
                send("#e");
            } else {
                System.out.println(wrong);
            }

        } else if (message.equals("#w")) {                                  //wait for companion
            if (client.isChatting()) {
                System.out.println(wrong);
            } else {
                send("#w");
                client.setChatting(true);
            }

        } else if (message.startsWith("#c")) {                              //create room
            createRoom(message);

        } else if (message.equals("#r")) {                                  //print available rooms
            if (client.isInPrivateChat()) {
                System.out.println(wrong);
            } else {
                send(message);
            }

        } else if (message.startsWith("#j ")) {                             //join room
            if (client.isInPrivateChat()) {
                System.out.println(wrong);
            } else {
                StringTokenizer st = new StringTokenizer(message, " ");
                st.nextToken();
                if (st.hasMoreTokens() && client.hasRoom(st.nextToken()) ) {
                    send(message);
                    client.setChatting(true);
                    client.addJoinedRoom(message.substring(3));
                } else {
                    System.out.println("No such room. Try again!");
                }
            }

        } else if (message.equals("#h")) {                                  //print help
            printHelp();

        } else if (message.startsWith("#s ")) {                             //select companion
            if (client.isChatting()) {
                System.out.println(wrong);
            } else {
                StringTokenizer st = new StringTokenizer(message, " ");
                st.nextToken();
                if ( st.hasMoreTokens() && !client.hasUser( st.nextToken() ) ) {
                    send(message);
                    client.setChatting(true);
                } else {
                    System.out.println("No such user. Try again!");
                }
            }
        } else {                                                            //wrong command
            System.out.println(wrong);
        }
    }

    private void handleMessage(String message) throws IOException {
        if (!client.isChatting()) {
            System.out.println("You are not in chat yet!");
        } else {
            send(userName + " << " + message);
            System.out.println("\r" + userName + " >> " + message);
        }
    }

    private void printHelp() {
        System.out.println("To quit from server type \"#q\"");
        if (client.isChatting())
            System.out.println("To exit from current chats type \"#e\"");
        if (!client.isChatting()) {
            System.out.println("To wait for invite in private chat type \"#w\"");
            System.out.println("To create new room type \"#c <room_name> <room_capacity>\"");
            System.out.println("To start private chat type \"#s <free user's ID>\"");
        }
        if ( (client.isChatting() && !client.isInPrivateChat()) || !client.isChatting() ) {
            System.out.println("To join available room type \"#j <room_name>\"");
            System.out.println("To see list of available rooms type \"#r\"");
        }
    }

    private void createRoom(String message) throws IOException {
        if (client.isChatting()) {
            System.out.println(wrong);
        } else {
            StringTokenizer st = new StringTokenizer(message, " ");
            st.nextToken();
            String roomName;
            if (st.hasMoreTokens()) {
                roomName = st.nextToken();
            } else {
                System.out.println("No room name and capacity");
                return;
            }

            if (st.hasMoreTokens()) {

                try {
                    int capacity = Integer.parseInt(st.nextToken());
                    if (capacity < 2) {
                        System.out.println("Room capacity must me 2 or more!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("No room capacity");
                    return;
                }

            } else {
                System.out.println(wrong);
                return;
            }
            if (client.hasRoom(roomName)) {
                System.out.println("There is such room on server. Choose another name for room.");
            } else {
                client.setChatting(true);
                send(message);
            }
        }
    }

}
