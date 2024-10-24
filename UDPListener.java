import java.net.*;
import java.util.concurrent.*;


// Thread class that continuously listens for UDP packets on the broadcast port with new_game_message.
// Returns the Datagram packet found, none if halted.
// Calling stop() will halt this thread.
public class UDPListener implements Callable<DatagramPacket> {    
    DatagramSocket udp_socket;

    final InetAddress broadcast_address;
    final int broadcast_port;
    final String new_game_message;


    UDPListener(InetAddress broadcast_address, int broadcast_port, String new_game_message){
        this.broadcast_address = broadcast_address;
        this.broadcast_port = broadcast_port;
        this.new_game_message = new_game_message;
    }

    @Override
    public DatagramPacket call() throws Exception {
        // System.out.println("UDPListener: Listening on address: " + broadcast_address.getHostAddress() + "\tPort: " + broadcast_port);
        System.out.println("UDPListener: Listening on port: " + broadcast_port);

        try {
            // Need to reuse address when testing two promgram instances on same machine
            udp_socket = new DatagramSocket(broadcast_port);
            // udp_socket.setReuseAddress(true);
            // udp_socket.bind(new InetSocketAddress(client_port));
            
            while(true) {
                byte[] receive_data = new byte[1024];
                DatagramPacket receive_packet = new DatagramPacket(receive_data, receive_data.length);

                try {
                    // Receive packet from client (blocks on receive())
                    udp_socket.receive(receive_packet);
                }
                catch (SocketException e) {
                    // When receive is unblocked, this exception is thrown
                    System.out.println("UDPListener has been stopped.");
                    return null;
                }

                if (contains_new_game_message(receive_packet)) {
                    return receive_packet;
                }
            }
        }
        catch (Exception e){
            System.out.println("UDPListener could not bind to the given address and port: " + broadcast_address.getHostAddress() +
                "\t" + broadcast_port);
            e.printStackTrace();
        }
        return null;
    }

    // To unblock this thread when blocked on receive()
    public void stop() {
        // This will unblock receive()
        if (udp_socket != null && !udp_socket.isClosed()) {
            udp_socket.close();
        }
    }


    // Checks if packet contains new game message
    public boolean contains_new_game_message(DatagramPacket receive_packet) {
        // Get message
        String received_message = new String(receive_packet.getData(), 0, receive_packet.getLength());

        try {
            // Check if packet contains new game message
            if (new_game_message.equals(received_message.split(":")[0])) {
                System.out.println("\nUDPListener: Found new game request: " + received_message + "\nAddress: " +
                    receive_packet.getAddress().getHostAddress() + "\tPort: " + receive_packet.getPort());
                return true;
            }
        }
        // If message isn't formatted correctly or doesn't contain new game message
        catch (Exception e) {}

        System.out.println(
            "UDPListener: Received unknown request: " + received_message +
            "\nAddress: " + receive_packet.getAddress().getHostAddress() + "\tPort: " + receive_packet.getPort());
        return false;
    }
}
