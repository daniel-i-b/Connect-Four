import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Iterator;



// If auto_broadcast_address is set to false, broadcast address and port must be input through startup commandline arguments.
// ie: java Client 192.168.0.255 8000
public class Client {
    // Im lazy
    static final boolean auto_broadcast_address = true; 

    static InetAddress broadcast_address;
    static int broadcast_port;


    public static void main(String[] args) throws IOException {
        if (auto_broadcast_address) {
            broadcast_address = find_first_broadcast_address();
            broadcast_port = 8000;
        }
        else {
            try {
                broadcast_address = InetAddress.getByName(args[0]);
                broadcast_port = Integer.parseInt(args[1]);
            }
            catch (Exception e) {
                System.out.println("Broadcast address and port must be input through startup commandline arguments.\n" +
                    "ie: java Client 192.168.0.255 8000");
            }
        }

        GameMaster gameMaster = new GameMaster(broadcast_address, broadcast_port);
        gameMaster.start();
    }


    // Utility function for finding the first broadcast address on the network
    // Posted by mpontillo: https://stackoverflow.com/questions/4887675/detecting-all-available-networks-broadcast-addresses-in-java
    public static InetAddress find_first_broadcast_address() {
        // HashSet<InetAddress> listOfBroadcasts = new HashSet<InetAddress>();
        Enumeration<NetworkInterface> list;
        try {
            list = NetworkInterface.getNetworkInterfaces();

            while(list.hasMoreElements()) {
                NetworkInterface iface = list.nextElement();

                if(iface == null) continue;

                if(!iface.isLoopback() && iface.isUp()) {
                    Iterator<InterfaceAddress> it = iface.getInterfaceAddresses().iterator();

                    while (it.hasNext()) {
                        InterfaceAddress address = it.next();

                        if(address == null) continue;

                        InetAddress broadcast = address.getBroadcast();
                        if(broadcast != null) {
                            return broadcast;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            System.err.println("Error while getting network interfaces");
            ex.printStackTrace();
        }
        return null;
    }
}
