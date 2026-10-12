import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reads a program's input either from a text file (when a file name is given on the
 * command line) or from the keyboard (when no file name is given).
 *
 * Both modes use exactly the same format, so a sample file is simply a record of what
 * you would type at the keyboard. Blank lines and lines starting with '#' are ignored.
 * In file mode every value read is echoed next to its prompt, so the output is
 * self-explanatory.
 *
 * This class also holds the parsing helpers shared by all five programs.
 */
public class InputSource {

    /** Words that cannot be used as state names because they have a special meaning. */
    private static final Set<String> RESERVED = new HashSet<String>(Arrays.asList(
            "end", "done", "exit", "quit", "none", "eps", "epsilon", "lambda", "ε", "λ"));

    private final BufferedReader reader;
    private final boolean interactive;
    private int lineNumber = 0;

    public InputSource(String[] args) throws IOException {
        if (args.length > 0) {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(args[0]), StandardCharsets.UTF_8));
            interactive = false;
            System.out.println("Reading input from file: " + args[0]);
        } else {
            reader = new BufferedReader(new InputStreamReader(System.in));
            interactive = true;
            System.out.println("Reading input from the keyboard.");
            System.out.println("(Tip: you can also run this program with an input file name as an argument.)");
        }
    }

    public boolean isInteractive() {
        return interactive;
    }

    /** Prints a message only when the user is typing at the keyboard (instructions). */
    public void hint(String message) {
        if (interactive) {
            System.out.println(message);
        }
    }

    /**
     * Shows the prompt and returns the next meaningful line (trimmed), or null at the end
     * of the input.
     */
    public String nextLine(String prompt) throws IOException {
        if (interactive) {
            System.out.print(prompt + " ");
            System.out.flush();
        }
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.startsWith("﻿")) {          // byte-order mark written by some editors
                line = line.substring(1);
            }
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!interactive) {
                System.out.println(prompt + " " + line);
            }
            return line;
        }
        return null;
    }

    /** Like nextLine, but the value is required: the end of input is an error. */
    public String require(String prompt) throws IOException {
        String line = nextLine(prompt);
        if (line == null) {
            throw new InputException("The input ended while waiting for: " + prompt.trim());
        }
        return line;
    }

    /** Builds an error that mentions the current line number when reading from a file. */
    public InputException error(String message) {
        if (interactive) {
            return new InputException(message);
        }
        return new InputException("line " + lineNumber + ": " + message);
    }

    // ------------------------------------------------------------------ parsing helpers

    /**
     * Splits a line into tokens. Spaces and commas both separate tokens, and one pair of
     * surrounding braces is removed, so "q0 q1", "q0,q1" and "{q0, q1}" are all accepted.
     */
    public static List<String> tokens(String line) {
        String s = line.trim();
        if (s.startsWith("{") && s.endsWith("}") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        List<String> result = new ArrayList<String>();
        for (String part : s.split("[\\s,]+")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

    /** Tokens with the optional decoration "->", "=" and "|-" removed. */
    public static List<String> transitionTokens(String line) {
        List<String> result = new ArrayList<String>();
        for (String t : tokens(line)) {
            if (!t.equals("->") && !t.equals("=") && !t.equals("|-") && !t.equals("=>")) {
                result.add(t);
            }
        }
        return result;
    }

    public static boolean isEpsilon(String token) {
        String t = token.toLowerCase(Locale.ROOT);
        return t.equals("eps") || t.equals("epsilon") || t.equals("lambda")
                || t.equals("ε") || t.equals("λ");
    }

    public static boolean isEnd(String line) {
        String t = line.trim().toLowerCase(Locale.ROOT);
        return t.equals("end") || t.equals("done") || t.equals("exit") || t.equals("quit");
    }

    public List<String> parseStates(String line) {
        List<String> states = tokens(line);
        if (states.isEmpty()) {
            throw error("at least one state is required");
        }
        Set<String> seen = new HashSet<String>();
        for (String q : states) {
            if (RESERVED.contains(q.toLowerCase(Locale.ROOT))) {
                throw error("'" + q + "' is a reserved word and cannot be used as a state name");
            }
            if (!seen.add(q)) {
                throw error("state '" + q + "' is listed twice");
            }
        }
        return states;
    }

    /** Parses an alphabet: every symbol must be a single character. */
    public List<Character> parseAlphabet(String line, String what) {
        List<Character> symbols = new ArrayList<Character>();
        for (String t : tokens(line)) {
            if (t.length() != 1) {
                throw error("each " + what + " symbol must be a single character, but got '" + t + "'");
            }
            char c = t.charAt(0);
            if (c == 'ε' || c == 'λ') {
                throw error("the empty string cannot be a symbol of the " + what + " alphabet");
            }
            if (symbols.contains(c)) {
                throw error("symbol '" + c + "' is listed twice");
            }
            symbols.add(c);
        }
        if (symbols.isEmpty()) {
            throw error("the " + what + " alphabet must contain at least one symbol");
        }
        return symbols;
    }

    public String parseState(String token, Collection<String> states) {
        if (!states.contains(token)) {
            throw error("'" + token + "' is not one of the states " + Util.setString(states));
        }
        return token;
    }

    public String parseSingleState(String line, Collection<String> states) {
        List<String> t = tokens(line);
        if (t.size() != 1) {
            throw error("expected exactly one state, got: " + line);
        }
        return parseState(t.get(0), states);
    }

    /** Parses a set of states; "none", "-" or "{}" mean the empty set. */
    public Set<String> parseStateSet(String line, Collection<String> states) {
        Set<String> result = new LinkedHashSet<String>();
        String s = line.trim();
        if (s.equalsIgnoreCase("none") || s.equals("-")) {
            return result;
        }
        for (String t : tokens(s)) {
            result.add(parseState(t, states));
        }
        return result;
    }

    /** Converts a line typed as an input word: "eps" means the empty string, spaces are ignored. */
    public static String parseWord(String line) {
        String s = line.trim();
        if (isEpsilon(s)) {
            return "";
        }
        return s.replaceAll("\\s+", "");
    }
}
