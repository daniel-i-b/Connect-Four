import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


// Handles the matchmaking by delegating listening tasks to other threads and waiting for the results.
// Returns an established TCP socket connection.
public class MatchMaker implements Callable<Socket>{
    /*
    Player 1 (Sender): Sends UDP packets on broadcast port containing TCP connection port.
    Establishes connection over TCP by receiving a TCP packet sent by opponent using port in previously sent UDP packets.

    Player 2 (Receiver): Receives UDP packet on broadcast port containing TCP connection port.
    Establishes connection over TCP by sending a TCP packet to opponent using port received in UDP packet.
     */
    final InetAddress broadcast_address;
    final int broadcast_port;
    final int tcp_port = new Random().nextInt(100) + 9000;

    final int milliseconds_between_broadcasts = 5000;
    final String new_game_message = "NEW GAME";

    // Create executor to create and manage threads
    final ExecutorService executor = Executors.newCachedThreadPool();
    final UDPListener udp_listener;
    final TCPListener tcp_listener;

    // Variable to track who goes first
    // 1 -> Player 1: Goes first
    // 2 -> Player 2: Takes the L
    int client_player;


    MatchMaker(InetAddress broadcast_address, int broadcast_port) throws UnknownHostException {
        this.broadcast_address = broadcast_address;
        this.broadcast_port = broadcast_port;

        this.udp_listener = new UDPListener(broadcast_address, broadcast_port, new_game_message);
        this.tcp_listener = new TCPListener(tcp_port);
    }


    @Override
    public Socket call() throws Exception {
        // Start threads
        Future<DatagramPacket> udp_listener_task = executor.submit(udp_listener);
        Future<Socket> tcp_listener_task = executor.submit(tcp_listener);

        // To send UDP broadcasts at regular intervals
        long start_time = System.currentTimeMillis();

        // Active check if threads have finished
        while(true) {
            // Player 2 Scenario - found opponent broadcast
            if (udp_listener_task.isDone()) {
                // Must terminate TCP listener as it binds to the client address as does the to be created Socket
                tcp_listener.stop();
                // Get UDP packet containing TCP port
                Socket new_socket = get_socket_from_udp_broadcast(udp_listener_task.get());
                // Close UDP listener (to force it to unbind from socket)
                udp_listener.stop();

                // If UDP packet didn't contain a valid format message or port number
                if (new_socket == null) {
                     // Start up UDP and TCP listeners again 
                    udp_listener_task = executor.submit(udp_listener);
                    tcp_listener_task = executor.submit(tcp_listener);
                    continue;
                }
                // Set player variable
                client_player = 2;
                
                return new_socket;
            }
            // Player 1 Scenario - opponent responded to broadcast
            else if (tcp_listener_task.isDone()) {
                // Get TCP socket 
                Socket socket = tcp_listener_task.get();
                // Terminate other matchmaker threads (to force them to unbind from socket)
                udp_listener.stop();
                tcp_listener.stop();

                // Set player variable
                client_player = 1;
                // Return socket
                return socket;
            }
            // Send broadcast if (milliseconds_between_broadcasts) has passed
            else if (System.currentTimeMillis() - start_time > this.milliseconds_between_broadcasts) {
                // Must close UDP listening thread to send UDP broadcast, otherwise this client will connect with itself
                udp_listener.stop();
                // Send out broadcast
                send_broadcast();
                // Start up UDP listener again 
                udp_listener_task = executor.submit(udp_listener);
                // Update time
                start_time = System.currentTimeMillis();
            }
        }
    }


    // Attempts to build socket from datagram. If it does not contain a format or valid int as port, returns null
    public Socket get_socket_from_udp_broadcast(DatagramPacket opponent_packet) throws IOException {
        String received_message = new String(opponent_packet.getData(), 0, opponent_packet.getLength());
        int opponent_port;
        try {
            // Get port from message
            opponent_port = Integer.parseInt(received_message.split(":")[1]);
        }
        // Not valid format
        catch (Exception e) {
            System.out.println("New game message did not contain a valid format; expected " + new_game_message +
                ": followed by an integer. Received: " + received_message);
            return null;
        }
        System.out.println("\nCreating new socket connection: " + opponent_packet.getAddress().getHostAddress() +
            "\t" + opponent_port);

        // Create new socket connected to the opponent address and port, and binded to this local address and port.
        return new Socket(opponent_packet.getAddress(), opponent_port, InetAddress.getLocalHost(), tcp_port);
    }


    // Send UDP packet to broadcast address with TCP port as payload
    public void send_broadcast() {
        System.out.println("\nBroadcasting new game with TCP port: " + tcp_port +
            "\nAddress: " + broadcast_address.getHostAddress() + "\tPort: " + broadcast_port);
        
        try (DatagramSocket send_socket = new DatagramSocket(broadcast_port)) {
            // Initialise and send payload to broadcast address
            byte[] sendData = (new_game_message + ":" + tcp_port).getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast_address, broadcast_port);
            send_socket.send(sendPacket);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
