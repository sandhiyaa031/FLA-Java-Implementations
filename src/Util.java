import java.util.Collection;
import java.util.List;

/** Small helpers shared by all programs. */
public final class Util {

    private Util() {
    }

    /** Prints the title block at the start of a program. */
    public static void banner(String title, String description) {
        String rule = "======================================================================";
        System.out.println(rule);
        System.out.println("  " + title);
        System.out.println("  " + description);
        System.out.println(rule);
    }

    /** Prints a section heading. */
    public static void heading(String text) {
        System.out.println();
        System.out.println("---- " + text + " ----");
    }

    /** Shows a word for output; the empty string is shown as eps. */
    public static String show(String w) {
        return w.isEmpty() ? "eps" : w;
    }

    /** Shows a word in quotes, or eps (the empty string). */
    public static String quote(String w) {
        return w.isEmpty() ? "eps (the empty string)" : "\"" + w + "\"";
    }

    public static String setString(Collection<?> items) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Object o : items) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(o);
            first = false;
        }
        return sb.append('}').toString();
    }

    /**
     * Calls the visitor for every word over the alphabet of length 0..maxLength, in order of
     * length. Stops early (and returns the word) as soon as the visitor returns false;
     * returns null if every word was visited.
     */
    public static String forEachWord(List<Character> alphabet, int maxLength, WordVisitor visitor) {
        int k = alphabet.size();
        for (int len = 0; len <= maxLength; len++) {
            int[] digits = new int[len];
            while (true) {
                StringBuilder sb = new StringBuilder(len);
                for (int d : digits) {
                    sb.append(alphabet.get(d));
                }
                String w = sb.toString();
                if (!visitor.visit(w)) {
                    return w;
                }
                int pos = len - 1;
                while (pos >= 0 && digits[pos] == k - 1) {
                    digits[pos] = 0;
                    pos--;
                }
                if (pos < 0) {
                    break;
                }
                digits[pos]++;
            }
        }
        return null;
    }

    /** Largest length L such that the number of words of length 0..L is at most limit (max cap). */
    public static int testLength(int alphabetSize, long limit, int cap) {
        long total = 0;
        long power = 1;
        int best = 0;
        for (int len = 0; len <= cap; len++) {
            total += power;
            if (total > limit) {
                break;
            }
            best = len;
            power *= alphabetSize;
        }
        return best;
    }

    /** Number of words of length 0..maxLength over an alphabet of the given size. */
    public static long wordCount(int alphabetSize, int maxLength) {
        long total = 0;
        long power = 1;
        for (int len = 0; len <= maxLength; len++) {
            total += power;
            power *= alphabetSize;
        }
        return total;
    }

    /** Callback used by forEachWord. */
    public interface WordVisitor {
        boolean visit(String word);
    }
}
