import java.util.*;
import java.util.function.BiFunction;


// Assuming column indices are base-0
public class ConnectFour {
    // 1 -> Player 1
    // 2 -> Player 2
    final int client_player;
    final int opponent_player;

    // 7-column-wide, 6-row-high
    final int columns = 7;
    final int rows = 6;
    final int positions_to_win = 4;

    final int column_base;

    // Objects to store current game state and display it to terminal
    ArrayList<ArrayList<Node>> game_state;
    final ConnectFourDisplay board_display;

    // For user input
    Scanner scanner = new Scanner(System.in);
    

    ConnectFour(int client_player, int column_base){
        initialise_game();
        this.board_display = new ConnectFourDisplay(game_state, column_base);

        this.client_player = client_player;
        this.opponent_player = client_player == 1 ? 2 : 1;

        this.column_base = column_base;
    }


    // Populates game_state 2d arraylist with nodes with populated indices
    public void initialise_game() {
        game_state = new ArrayList<ArrayList<Node>>();

        for (int i = 0; i < rows; i++) {
            ArrayList<Node> current_row = new ArrayList<Node>();
            for (int j = 0; j < columns; j++) {
                current_row.add(new Node((i * columns) + j));
            }
            game_state.add(current_row);
        }
    }


    // Gets and checks user column input before inserting into column
    // Returns successfully placed column index, or -1 for TIE
    public int client_insert() {
        // Display current board state
        System.out.println(board_display.toString());

        if (is_tie()) {
            System.out.println("TIE! No game spaces remaining.");
            return -1;
        }

        // Loop until valid input is received
        while (true) {
            System.out.println("Enter column index: ");
            String next_line = scanner.nextLine();

            try {
                // Attempt to parse to int
                int column_index = Integer.parseInt(next_line);

                // Check if input is within column bounds
                if (column_index < 0 + column_base || column_index >= columns + column_base) {
                    System.out.println("Invalid input; column index out of bounds. Received: " + column_index);
                    continue;
                }

                // Insert node
                Node inserted_node = insert(column_index, client_player);

                if (inserted_node == null) {
                    System.out.println("Invalid input; column is fully occupied.");
                    continue;
                }
                // Print updated board
                System.out.println(board_display.toString());

                // Check to see if won (still need YOU WIN from opponent to exit)
                if (has_won(inserted_node)) {
                    System.out.println("You win! Waiting for opponent confirmation...");
                }
                else {
                    System.out.println("Successfully inserted node at index: " + column_index);
                }
            
                // Return inserted column index
                return column_index;
            }
            catch (NumberFormatException e) {
                System.out.println("Invalid input; expected an integer. Received: " + next_line);
                continue;
            }
        }
    }


    // Inserts opponent node at column index
    // Returns successfully placed column index, -1 for if opponent won, or -2 if invalid
    public int opponent_insert(int column_index) {
        Node inserted_node = insert(column_index, opponent_player);

        // Opponent sent invalid index
        if (inserted_node == null) {
            return -2;
        }
        // Opponent wins
        else if (has_won(inserted_node)) {
            return -1;
        }
        // Return inserted column index
        return column_index;
    }


    // Check if all cells are occupied and game is a tie
    public boolean is_tie() {
        // For each column
        for (int i = 0; i < columns; i++) {
            // Game continues if any cell in the top row is unoccupied
            if (game_state.get(rows - 1).get(i).state == -1) {
                return false;
            }
        }
        return true;
    }


    // Insert node at column index. Returns inserted node if successful, null if unsuccessful
    // Assuming base 0 column index
    public Node insert(int column_index, int target_player) {
        // If column index is not valid
        if (column_index >= columns + column_base) {
            return null;
        }
        // Target node is the top node in target column
        Node target_node = game_state.get(rows - 1).get(column_index - column_base);

        // If top of column is occupied
        if (target_node.state != -1) {
            return null;
        }
        // Get node below target
        Node below_node = get_node_vertical(target_node, true);
        // Keep going down until node below target is occupided
        while (below_node != null && below_node.state == -1) {
            target_node = below_node;
            below_node = get_node_vertical(target_node, true);
        }
        target_node.state = target_player;
        return target_node;
    }


    // Checks if current_player has won around given target_node
    // Will only need to check based off of each newly placed node - new winning condition must include newly placed node
    public boolean has_won(Node target_node) {
        // Check if won horizontally, vertically, or diagonally (positive and negative gradient)
        if (has_won_in_direction(target_node, (i, j) -> get_node_horizontal(i, j))) {
            return true;
        }
        else if (has_won_in_direction(target_node, (i, j) -> get_node_vertical(i, j))) {
            return true;
        }
        else if (has_won_in_direction(target_node, (i, j) -> get_node_diagonal_negative(i, j))) {
            return true;
        }
        else if (has_won_in_direction(target_node, (i, j) -> get_node_diagonal_positive(i, j))) {
            return true;
        }
        return false;
    }


    // Checks if current_player has at least positions_to_win consecutive nodes in a given direction around given target_node
    public boolean has_won_in_direction(Node target_node, BiFunction<Node, Boolean, Node> node_direction_function) {
        int target_player = target_node.state;
        // To account for target node
        int current_positions = 1;
        boolean current_direction = false;
        Node next_node = node_direction_function.apply(target_node, current_direction);
 
        do {
            // Count the number of consecutive player controlled nodes in the given direction around target node
            while (next_node != null && next_node.state == target_player) {
                next_node = node_direction_function.apply(next_node, current_direction);
                current_positions += 1;
            }
            // Switch direction (if checking horizontal: false=left, true=right)
            current_direction = !current_direction;
            next_node = node_direction_function.apply(target_node, current_direction);
        }
        // Exits loop once two direction changes have been made
        while (current_direction);

        return (current_positions >= this.positions_to_win) ? true : false;
    }


    // Check if index is out of bounds of the board
    public boolean is_valid_index(int index) {
        return (index >= rows*columns || index < 0) ? false : true;
    }

    // Get node with given index
    public Node get_node(int index) {
        return is_valid_index(index) ? game_state.get(index / columns).get(index % columns) : null;
    }

    // below: true = down, false = up
    public Node get_node_vertical(Node node, boolean below) {
        return node == null ? null : 
            below ? get_node(node.index - columns) : get_node(node.index + columns);
    }

    // left: true = left, false = right
    public Node get_node_horizontal(Node node, boolean left) {
        // Must check if going left or right goes over the vertical edge of the board to prevent looping around
        return node == null ? null : 
            left ? ((node.index % columns == 0) ? null : get_node(node.index - 1)) : 
                (((node.index + 1) % columns == 0) ? null : get_node(node.index + 1));
    }

    // Diagonal negative gradient; down: true = bottom right, false = top left
    public Node get_node_diagonal_negative(Node node, boolean down) {
        return node == null ? null :
            down ? get_node_horizontal(get_node_vertical(node, true), false) : get_node_horizontal(get_node_vertical(node, false), true);
    }

    // Diagonal positive gradient; down: true = bottom left, false = top right
    public Node get_node_diagonal_positive(Node node, boolean down) {
        return node == null ? null :
            down ? get_node_horizontal(get_node_vertical(node, true), true) : get_node_horizontal(get_node_vertical(node, false), false);
    }

    // To display board
    public String toString() {
        return board_display.toString();
    }

    // Display nice message at the beginning of the game
    public String start_message() {
        StringBuilder message_builder = new StringBuilder();
        message_builder.append("-- You are Player " + client_player + " --\n");
        message_builder.append(client_player == 1 ? board_display.player_1_cell : board_display.player_2_cell);
        return message_builder.toString();
    }
}
