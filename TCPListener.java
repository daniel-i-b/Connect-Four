import java.io.IOException;
import java.net.*;
import java.util.concurrent.Callable;


// Continuously listens on the supplied tcp port.
// Returns the socket found, none if halted. 
// Calling stop() will halt this thread.
public class TCPListener implements Callable<Socket>{
    ServerSocket server_socket;
    final int tcp_port;


    TCPListener(int tcp_port) throws UnknownHostException{
        this.tcp_port = tcp_port;
    }


    @Override
    public Socket call() throws Exception {
        System.out.println("TCPListener: Listening on local address and port: " + tcp_port);
        try{
            // Initialise server socket and get new socket
            server_socket = new ServerSocket(tcp_port);
            Socket new_socket = server_socket.accept();   
              
            System.out.println("\nTCPListener: Received new game reply.\nAddress: " +
                new_socket.getInetAddress().getHostAddress() + "\tPort: " + new_socket.getPort());

            // Ensure socket is closed and return new socket
            stop();
            return new_socket;     
        }
        catch(SocketException e) {
            System.out.println("TCPListener has been stopped.");
            return null;
        }
        catch(Exception e) {
            System.out.println("TCPListener could not bind to the given address and port: " + tcp_port);
            e.printStackTrace();
        }
        return null;
    }


    // To unblock this thread when blocked on receive()
    public void stop() throws IOException {
        // This will unblock receive()
        if (server_socket != null && !server_socket.isClosed()) {
            server_socket.close();
        }
    }
}
