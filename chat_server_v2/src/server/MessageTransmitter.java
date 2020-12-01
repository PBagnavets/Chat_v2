package server;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class MessageTransmitter implements Runnable {

    private final SelectionKey selectionKey;
    private final ChatServer server;


    public MessageTransmitter(SelectionKey key, ChatServer server) {
        this.selectionKey = key;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            //read message
            String message;
            Client client = (Client) selectionKey.attachment();
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            StringBuilder sb /*= new StringBuilder()*/;
            ByteBuffer buffer = ByteBuffer.allocate(256);
            buffer.clear();
            int read = channel.read(buffer);

//            while ((read = channel.read(buffer)) > 0 && channel.isConnected()) {
//                buffer.flip();
//                byte[] bytes = new byte[buffer.limit()];
//                buffer.get(bytes);
//                sb.append(new String(bytes));
//                buffer.clear();
////                System.out.println(sb.toString() + ", read" + read);
//            }
//            System.out.println("!!!!!!!!!!!!!!!!!!!!!");
            //parse message from StringBuilder sb and send response
            sb = new StringBuilder(new String(buffer.array()).trim());
            if (read < 0) { //client closed
                channel.close();
                handleExit(client, client.getName() + " left server.");
            }
            //#r - print rooms
            if (sb.toString().equals("#r")) {
                client.sendMessage(ByteBuffer.wrap(server.printRooms().getBytes()));
            //#j - join room
            } else if (sb.toString().startsWith("#j")) {
                if (server.getAvailableRoom(sb.substring(3)) != null)
                    joinRoom(client, sb.substring(3));
                else client.sendMessage(ByteBuffer.wrap("No such room".getBytes()));
            //#e - exit from private chat or room
            } else if (sb.toString().equals("#e")) {

                handleExit(client, client.getName() + " left.");

            } else { //send message
                message = client.getName() + " << " + sb.toString();
                client.sendUserMessage(sb.toString());
            }




        } catch (SocketException e) {
            selectionKey.cancel();
        } catch (ClosedChannelException e) {
            //no such channel. Do nothing
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void joinRoom(Client client, String roomName) throws IOException {
        if (client.hasCompanion || server.getAvailableRoom(roomName) == null) {
            client.sendMessage(ByteBuffer.wrap("You cannot join room!".getBytes()));
        } else {
            //System.out.println("|" + roomName + "|");
            client.addRoom(server.getAvailableRoom(roomName));
            client.sendMessage(ByteBuffer.wrap(("You join room: " + roomName).getBytes()));
        }
    }

    private void handleExit(Client client, String message) throws IOException {

        if (client.hasCompanion) {
            client.sendUserMessage("#" + message);
            client.companion.companion = null;
            client.companion.hasCompanion = false;
            client.companion.key.cancel();
            client.companion.channel.configureBlocking(true);
            server.pool.execute(new ChatSelector(this.server, client.companion.channel, client.companion));

            client.companion = null;
            client.hasCompanion = false;
            client.key.cancel();

        } else if (client.isInRoom){ //isInRoom
            client.sendUserMessage(message);
            client.isInRoom = false;
            for (Room room : client.rooms) {
                room.removeUser(client);
            }
            client.removeRooms();
            client.key.cancel();
        }
        if (client.channel.isConnected()) {
            client.channel.configureBlocking(true);
            server.pool.execute(new ChatSelector(this.server, client.channel, client));
        }
    }

}
