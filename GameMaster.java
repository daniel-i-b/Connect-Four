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

    // Variable to track this player
    // 1 -> Player 1: Goes first
    // 2 -> Player 2: Takes the L
    final int client_player;
    
    // Command strings
    final String insert_command = "INSERT";
    final String win_command = "YOU WIN";
    final String error_command = "ERROR";
    final String l_message = new LDisplay().toString();

    // Variable to change the INSERT column base since spec doesn't specify 
    final int column_base = 1;
    


    GameMaster(InetAddress broadcast_address, int broadcast_port) throws IOException, InterruptedException, ExecutionException{
        // Get connection
        this.match_maker = new MatchMaker(broadcast_address, broadcast_port);
        this.game_socket = get_socket_connection();
        this.game_socket_out = new PrintWriter(game_socket.getOutputStream(), true);
        this.game_socket_in = new BufferedReader(new InputStreamReader(game_socket.getInputStream()));

        // Set client player and initialise game
        this.client_player = this.match_maker.client_player;
        this.connect_four = new ConnectFour(client_player, column_base);
    }


    // Blocks until TCP socket connection is established
    public Socket get_socket_connection() throws InterruptedException, ExecutionException {
        // Create and run matchmaker task thread to get socket.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Socket> match_maker_task = executor.submit(match_maker);

        // Blocks until matchmatcher returns with socket
        Socket socket = match_maker_task.get();

        System.out.println("\n----------- CONNECTION ESTABLISHED -----------\n" + 
            socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort() +
            " <-----> " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

        // Shutdown matchmaker task thread
        executor.shutdownNow();
        return socket;

    }


    public void start() throws IOException {
        int current_player;

        System.out.println(connect_four.start_message());

        // Go first
        if (client_player == 1) {
            current_player = 1;

            clients_turn();
        }
        else {
            current_player = 2;

            System.out.println(connect_four.toString());
            opponents_turn();
        }

        while(true) {
            // If current_player is 1, then 3 - 1 = 2
            // If current_player is 2, then 3 - 2 = 1
            current_player = 3 - current_player;

            if (current_player == 1){
                 clients_turn();
            }
            else {
                opponents_turn();
            }
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
    }


    public void opponents_turn() throws IOException {
        System.out.println("Waiting for opponent...");

        // Get message
        String input = game_socket_in.readLine();
        // Check message
        if (input == null) {
            System.out.println("Opponent sent invalid input; expected game command arguments. Received: "+ input);
            send_message(error_command);
            System.out.println("Exiting...");
            System.exit(0);
        }

        System.out.println("Received: " + input);

        // Check for error or win messages
        switch (input) {
            case win_command:
                System.out.println("Opponent agrees! Exiting...");
                System.exit(0);
            
            case error_command:
                System.out.println("Opponent sent error message. Exiting...");
                System.exit(0);
        }

        int column_index = -1;

        try {
            // Attempt to split string and parse int
            column_index = Integer.parseInt(input.split(":")[1]);
        }
        catch (Exception e) {
            // Catch and return error to opponent
            System.out.println("Opponent sent invalid input; expected an integer after " +
                insert_command + ":. Received: " + input);
            send_message(error_command);
            System.out.println("Exiting...");
            System.exit(0);
        }

        // Insert node into game
        int response = connect_four.opponent_insert(column_index);
        switch (response) {
            // Column index received is not valid
            case -2:
                System.out.println("Opponent send invalid index.");
                send_message(error_command);
                System.out.println("Exiting...");
                System.exit(0);
            // Opponent wins
            case -1:
                System.out.println(connect_four.toString());
                System.out.println("You lose!");
                send_message(win_command);
                System.out.println(l_message);
                System.out.println("Exiting...");
                System.exit(0);
        }
    }


    public void send_message(String message) {
        // Send message
        game_socket_out.println(message);
        System.out.println("Sent to opponent: " + message);
    }
}
