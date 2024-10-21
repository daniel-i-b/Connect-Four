import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.*;


// Establishes connection with opponent. Blocks until connection is made and prompts terminal input for game moves
public class GameMaster {
    // TCP socket and helper objects to communicate game commands
    final Socket game_socket;
    final PrintWriter game_socket_out;
    final BufferedReader game_socket_in;

    // Matchmaker to find and establish game_socket connection using UDP and TCP
    final MatchMaker match_maker;
    // To manage the game and its logic
    final ConnectFour connect_four;

    // Variable to track of this player (does not change)
    // 1 -> Player 1: Goes first
    // 2 -> Player 2: Takes the L
    final int client_player;

    final String insert_command = "INSERT";
    final String win_command = "YOU WIN";
    final String error_command = "ERROR";

    final int column_base = 0;


    GameMaster(InetAddress broadcast_address, int broadcast_port) throws IOException{
        // Get connection
        this.match_maker = new MatchMaker(broadcast_address, broadcast_port);
        this.game_socket = get_socket_connection();
        this.game_socket_out = new PrintWriter(game_socket.getOutputStream(), true);
        this.game_socket_in = new BufferedReader(new InputStreamReader(game_socket.getInputStream()));

        // Set client player and initialise game
        this.client_player = this.match_maker.player;
        this.connect_four = new ConnectFour(client_player, column_base);
    }


    public void start() throws IOException {
        System.out.println(connect_four.start_message());

        // Go first
        if (client_player == 1) {
            clients_turn();
        }
        else {
            System.out.println(connect_four.toString());
            opponents_turn();
        }

    }


    public void clients_turn() throws IOException {
        int response = connect_four.client_insert();

        // A tie
        if (response == -1) {
            send_message(error_command);
            System.exit(0);
        }
        
        send_message(insert_command + ":" + response);
        // Keep looping until something happens
        opponents_turn();
    }


    public void opponents_turn() throws IOException {
        System.out.println("Waiting for opponent...");

        // Blocks on readLine()
        String input = game_socket_in.readLine();
        System.out.println("Received: " + input);

        switch (input) {
            case win_command:
                System.out.println("Opponent agrees! Exiting...");
                System.exit(0);
            
            case error_command:
                System.out.println("Opponent sent error message. Exiting...");
                System.exit(0);

            default:
                break;
        }

        try {
            int column_index = Integer.parseInt(input.split(":")[1]);

            int response = connect_four.opponent_insert(column_index);
            switch (response) {
                case -2:
                    System.out.println("Opponent send invalid index.");
                    send_message(error_command);
                    System.out.println("Exiting...");
                    System.exit(0);

                case -1:
                    System.out.println(connect_four.toString());
                    System.out.println("You lose!");
                    send_message(win_command);
                    System.out.println("Exiting...");
                    System.exit(0);

                default:
                    break;
            }
        }
        catch (Exception e) {
            System.out.println("Opponent sent invalid input; expected an integer after " +
                insert_command + ". Received: " + input);

            send_message(error_command);
            System.exit(0);
        }

        // Loop until something happens
        clients_turn();
    }


    public void send_message(String message) {
        // Send message
        game_socket_out.println(message);
        System.out.println("Sent to opponent: " + message);
    }


    // Blocks until TCP socket connection is established
    public Socket get_socket_connection() {
        // Create and run matchmaker task thread to get socket.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Socket> match_maker_task = executor.submit(match_maker);

        try {
            // Blocks until matchmatcher returns with socket
            Socket socket = match_maker_task.get();

            System.out.println("\n----------- CONNECTION ESTABLISHED -----------\n" + 
            socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort() +
                " <-----> " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

            // Shutdown matchmaker task thread
            executor.shutdownNow();

            return socket;
        }
        catch (Exception e) {
            System.out.println("GameMaster.get_connection(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
