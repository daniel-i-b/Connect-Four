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
    InetAddress broadcast_address;
    int broadcast_port;
    int tcp_port;

    int milliseconds_between_broadcasts = 5000;
    String new_game_message = "NEW GAME";

    // Variable to track who goes first
    // 1 -> Player 1: Goes first
    // 2 -> Player 2: Takes the L
    int player;


    MatchMaker(InetAddress broadcast_address, int broadcast_port) {
        this.broadcast_address = broadcast_address;
        this.broadcast_port = broadcast_port;

        // Get random tcp port between 9000 - 9100
        this.tcp_port = new Random().nextInt(100) + 9000;
    }


    @Override
    public Socket call() throws Exception {
        // Create executor to create and manage threads
        ExecutorService executor = Executors.newCachedThreadPool();

        // Create threads objects
        UDPListener udp_listener = new UDPListener(broadcast_address, broadcast_port, new_game_message);
        TCPListener tcp_listener = new TCPListener(tcp_port);

        // Start threads
        Future<DatagramPacket> udp_listener_task = executor.submit(udp_listener);
        Future<Socket> tcp_listener_task = executor.submit(tcp_listener);

        // To send UDP broadcasts at regular intervals
        long start_time = System.currentTimeMillis();

        // Active check if threads have finished
        while(true) {
            // Player 2 Scenario - found opponent broadcast
            if (udp_listener_task.isDone()) {
                // Get UDP packet containing TCP port
                DatagramPacket opponent_packet = udp_listener_task.get();
                // Terminate other matchmaker threads
                tcp_listener.stop();
                executor.shutdownNow();

                // Get port from message
                String received_message = new String(opponent_packet.getData(), 0, opponent_packet.getLength());
                int opponent_port = Integer.parseInt(received_message.split(":")[1]);

                System.out.println("\nCreating new socket connection: " + opponent_packet.getAddress().getHostAddress() +
                    "\t" + opponent_port);

                // Set player variable
                player = 2;
                // Create new socket connected to the opponent address and port, and binded to this local address and port.
                return new Socket(opponent_packet.getAddress(), opponent_port, InetAddress.getLocalHost(), tcp_port);
            }
            // Player 1 Scenario - opponent responded to broadcast
            else if (tcp_listener_task.isDone()) {
                // Get TCP socket 
                Socket socket = tcp_listener_task.get();
                // Terminate other matchmaker threads
                udp_listener.stop();
                executor.shutdownNow();
                
                // Set player variable
                player = 1;
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
