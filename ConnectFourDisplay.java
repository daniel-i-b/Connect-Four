import java.util.*;


// To display connect 4 board to terminal
public class ConnectFourDisplay {
    final ArrayList<ArrayList<Node>> board;

    final int rows;
    final int columns;
    final int column_base;

    final String cell_spacing = " ".repeat(9);

    final String player_1_cell = 
        "  \\   /  \n" +
        "    X    \n" +
        "  /   \\  ";
    
    final String player_2_cell = 
        "   ---   \n" +
        "  |   |  \n" +
        "   ---   ";

        
    ConnectFourDisplay(ArrayList<ArrayList<Node>> board, int column_base) {
        this.board = board;
        this.rows = board.size();
        this.columns = board.get(0).size();
        this.column_base = column_base;
    }
    
    
    // Override string method
    public String toString() {
        // Build board by each row top down
        StringBuilder board_builder = new StringBuilder();
        // Rows decrementing so visually board starts in bottom left and ends in top right
        for (int i = rows-1; i >= 0; i--) {
            board_builder.append("+" + (cell_spacing + "+").repeat(columns) + "\n");
            // Each visual cell row consists of 3 actual rows
            for (int cell_row = 0; cell_row < 3; cell_row++) {
                // For each column
                for (int j = 0; j < columns; j++) {
                    board_builder.append("|");
                    // 1 -> x, 2 -> o
                    switch (board.get(i).get(j).state) {
                        case 1:
                            board_builder.append(player_1_cell.split("\n")[cell_row]);
                            break;
                        case 2:
                            board_builder.append(player_2_cell.split("\n")[cell_row]);
                            break;
                        default:
                            // empty cell
                            board_builder.append(cell_spacing);
                            break;
                    }
                }
                // Add most right wall and newline
                board_builder.append("|\n");
            }
        }
        // Add floor
        board_builder.append("+" + " ------- +".repeat(columns));

        // Add column indices below floor
        board_builder.append("\n" + " ".repeat(5));
        for (int i = 0 + column_base; i < columns + column_base; i++) {
            board_builder.append(i + cell_spacing);
        }
        board_builder.append(" ".repeat(5));
        return board_builder.toString();
    }
}
