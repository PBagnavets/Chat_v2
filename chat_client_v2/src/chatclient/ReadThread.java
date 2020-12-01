package chatclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

//reads data from server in non-blocking mode
public class ReadThread extends Thread {

    private final ChatClient client;
    private final SocketChannel channel;
    private final String userName;
    private ByteBuffer readBuffer = ByteBuffer.allocate(256);



    public ReadThread(SocketChannel channel, String userName, ChatClient client) {
        this.channel = channel;
        this.userName = userName;
        this.client = client;
    }

    @Override
    public void run() {
        try {

            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ, client);

            while (channel.isConnected()) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (key.isReadable())
                        readAndParse();
                }
                selectedKeys.clear();
            }

//            channel.read(readBuffer);
//            String message = new String(readBuffer.array()).trim();

            readAndParse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readAndParse() throws IOException {
        //read
        StringBuilder sb = new StringBuilder();
        readBuffer.clear();

        while (channel.read(readBuffer) > 0) {
            readBuffer.flip();
            byte[] bytes = new byte[readBuffer.limit()];
            readBuffer.get(bytes);
            sb.append(new String(bytes));
            readBuffer.clear();
        }
        //parse
        String message = sb.toString();
        System.out.println(message);
        if (message.contains("ID '")) { //free users list received
            //add users' IDs to users set
            client.clearUsers();
            StringTokenizer st = new StringTokenizer(message, "'");
            while (st.hasMoreTokens()) {
                st.nextToken();
                if (st.hasMoreTokens())
                    client.addUser(st.nextToken());
            }
            client.setNoUsers(false);
        } else if (message.contains("Room: '")) { //available rooms list received
            client.clearRooms();
            StringTokenizer st = new StringTokenizer(message, "'");
            while (st.hasMoreTokens()) {
                st.nextToken();
                if (st.hasMoreTokens())
                    client.addRoom(st.nextToken());
            }
            client.setNoRooms(false);
        } else if (message.startsWith("#") && client.isInPrivateChat()) { //your companion left
            client.setInPrivateChat(false);
            client.setChatting(false);
        } else if (message.startsWith("You are invited to chat with ")) { //you are invited
            client.setInPrivateChat(true);
            client.setChatting(true);
        } else if (message.equals("There is no free users or available rooms")) {
            client.setNoUsers(true);
            client.setNoRooms(true);
        }



    }



}