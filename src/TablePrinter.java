import java.util.ArrayList;
import java.util.List;

/** Prints rows of text as an aligned table using plain ASCII characters. */
public class TablePrinter {
    private final String[] header;
    private final List<String[]> rows = new ArrayList<String[]>();

    public TablePrinter(String... header) {
        this.header = header;
    }

    public void addRow(Object... cells) {
        if (cells.length != header.length) {
            throw new IllegalArgumentException("row has " + cells.length + " cells, expected " + header.length);
        }
        String[] row = new String[cells.length];
        for (int i = 0; i < cells.length; i++) {
            row[i] = String.valueOf(cells[i]);
        }
        rows.add(row);
    }

    public void print() {
        int[] width = new int[header.length];
        for (int i = 0; i < header.length; i++) {
            width[i] = header[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                width[i] = Math.max(width[i], row[i].length());
            }
        }
        printRow(header, width);
        StringBuilder line = new StringBuilder("  ");
        for (int i = 0; i < width.length; i++) {
            if (i > 0) {
                line.append("-+-");
            }
            for (int k = 0; k < width[i]; k++) {
                line.append('-');
            }
        }
        System.out.println(line);
        for (String[] row : rows) {
            printRow(row, width);
        }
    }

    private static void printRow(String[] cells, int[] width) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(cells[i]);
            for (int k = cells[i].length(); k < width[i]; k++) {
                sb.append(' ');
            }
        }
        // remove trailing spaces
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') {
            end--;
        }
        System.out.println(sb.substring(0, end));
    }
}
