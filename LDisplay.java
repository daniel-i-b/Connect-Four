public class LDisplay {

    final int l_girth = 5;
    final int l_height = 6;
    final int l_base_length = 6;

    
    public String toString() {
        StringBuilder l_builder = new StringBuilder();
        l_builder.append("+ " + "-".repeat(l_girth - 2) + " +\n");

        for (int i = 0; i < l_height; i++) {
            l_builder.append("|" + " ".repeat(l_girth) + "|\n");
        }
        l_builder.append("|" + " ".repeat(l_girth) + "+ " + "-".repeat(l_base_length - 2) +
            " +\n");
        l_builder.append("|" + " ".repeat(l_girth + 1 + l_base_length) + "|\n");
        l_builder.append("+ " + "-".repeat(l_girth - 2) + " + " + "-".repeat(l_base_length - 2) +
            " +");
        return l_builder.toString();
    }
}
