import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * TASK 5: PDA SIMULATION
 *
 * A pushdown automaton M = (Q, Sigma, Gamma, delta, q0, z, F).
 * A move  delta(q, a, X) contains (p, alpha)  means: in state q, reading a (or eps),
 * with X on top of the stack: pop X, push the string alpha, go to state p.
 * alpha is written with its LEFTMOST symbol on TOP (so (q, aZ) puts a above Z), and
 * alpha = eps pushes nothing (a pure pop).
 *
 * A PDA may be nondeterministic, so the program explores every possible computation
 * (breadth-first search over configurations, also called instantaneous descriptions
 * (state, unread input, stack)). It accepts by FINAL STATE or by EMPTY STACK, as chosen,
 * and prints the sequence of moves and stack operations of an accepting computation.
 *
 * Usage:  java PDASimulation                 (keyboard)
 *         java PDASimulation input-file.txt  (file)
 */
public class PDASimulation {

    /** Search limits that protect against PDAs with endless epsilon-moves. */
    static final int MAX_CONFIGURATIONS = 1000000;
    static final int MAX_STACK = 1000;

    /** One transition: delta(from, input, pop) contains (to, push). */
    static class Move {
        final int number;
        final String from;
        final Character input;   // null = epsilon (reads nothing)
        final Character pop;     // null = does not look at / pop the stack
        final String to;
        final String push;       // "" = pushes nothing; leftmost character becomes the top

        Move(int number, String from, Character input, Character pop, String to, String push) {
            this.number = number;
            this.from = from;
            this.input = input;
            this.pop = pop;
            this.to = to;
            this.push = push;
        }

        public String toString() {
            return "delta(" + from + ", " + (input == null ? "eps" : input) + ", " + (pop == null ? "eps" : pop)
                    + ") = (" + to + ", " + Util.show(push) + ")";
        }

        String operation() {
            StringBuilder sb = new StringBuilder();
            sb.append(input == null ? "read nothing" : "read " + input);
            if (pop != null) {
                sb.append(", pop ").append(pop);
            }
            sb.append(push.isEmpty() ? (pop != null ? ", push nothing" : "") : ", push " + push);
            if (pop != null && push.equals(String.valueOf(pop))) {
                sb.append(" (stack unchanged)");
            }
            return sb.toString();
        }
    }

    /** A configuration (instantaneous description) of the PDA. */
    static class Config {
        final String state;
        final int position;      // number of input symbols already read
        final String stack;      // index 0 is the top of the stack
        final Config parent;
        final Move via;

        Config(String state, int position, String stack, Config parent, Move via) {
            this.state = state;
            this.position = position;
            this.stack = stack;
            this.parent = parent;
            this.via = via;
        }

        String key() {
            return state + '\u0001' + position + '\u0001' + stack;
        }
    }

    private final List<String> states;
    private final List<Character> inputAlphabet;
    private final List<Character> stackAlphabet;
    private final String start;
    private final char initialStack;
    private final Set<String> finals;
    private final boolean byFinalState;
    private final List<Move> moves;
    private final Map<String, List<Move>> movesFrom = new HashMap<String, List<Move>>();

    PDASimulation(List<String> states, List<Character> inputAlphabet, List<Character> stackAlphabet,
                  String start, char initialStack, Set<String> finals, boolean byFinalState, List<Move> moves) {
        this.states = states;
        this.inputAlphabet = inputAlphabet;
        this.stackAlphabet = stackAlphabet;
        this.start = start;
        this.initialStack = initialStack;
        this.finals = finals;
        this.byFinalState = byFinalState;
        this.moves = moves;
        for (String q : states) {
            movesFrom.put(q, new ArrayList<Move>());
        }
        for (Move m : moves) {
            movesFrom.get(m.from).add(m);
        }
    }

    public static void main(String[] args) {
        Util.banner("TASK 5: PDA SIMULATION",
                "Simulates a (possibly nondeterministic) pushdown automaton with its stack.");
        try {
            InputSource in = new InputSource(args);
            PDASimulation pda = read(in);
            pda.printDefinition();

            int tested = 0;
            int accepted = 0;
            while (true) {
                String line = in.nextLine("\nEnter an input string ('eps' = empty string, 'end' to stop):");
                if (line == null || InputSource.isEnd(line)) {
                    break;
                }
                if (pda.simulate(InputSource.parseWord(line))) {
                    accepted++;
                }
                tested++;
            }
            Util.heading("Summary");
            System.out.println("  Strings tested: " + tested + ", accepted: " + accepted
                    + ", not accepted: " + (tested - accepted));
        } catch (InputException e) {
            System.out.println();
            System.out.println("Input error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Could not read the input: " + e.getMessage());
            System.exit(1);
        }
    }

    static PDASimulation read(InputSource in) throws IOException {
        List<String> states = in.parseStates(in.require("Enter the states (separated by spaces):"));
        List<Character> sigma = in.parseAlphabet(in.require("Enter the input alphabet (single characters):"), "input");
        List<Character> gamma = in.parseAlphabet(in.require("Enter the stack alphabet (single characters):"), "stack");
        String start = in.parseSingleState(in.require("Enter the start state:"), states);
        String z = in.require("Enter the initial stack symbol:");
        if (z.length() != 1 || !gamma.contains(z.charAt(0))) {
            throw in.error("the initial stack symbol must be one symbol of the stack alphabet " + chars(gamma));
        }
        Set<String> finals = in.parseStateSet(in.require("Enter the final states ('none' if there are none):"), states);
        String mode = in.require("Accept by final state or by empty stack? (F / E):").toLowerCase(Locale.ROOT);
        boolean byFinal;
        if (mode.equals("f") || mode.startsWith("final")) {
            byFinal = true;
        } else if (mode.equals("e") || mode.startsWith("empty")) {
            byFinal = false;
        } else {
            throw in.error("please answer F (final state) or E (empty stack)");
        }
        if (byFinal && finals.isEmpty()) {
            System.out.println("  Note: acceptance is by final state but F is empty, so no string can be accepted.");
        }

        in.hint("Enter the transitions, one per line, as:  state input pop next-state push");
        in.hint("  input: an input symbol or 'eps'      pop: the stack symbol on top that is popped,");
        in.hint("  or 'eps' to leave the stack alone     push: the string pushed, leftmost symbol on top,");
        in.hint("  or 'eps' to push nothing.");
        in.hint("Example:  q0 a Z q0 aZ   means delta(q0, a, Z) contains (q0, aZ).   Type 'end' when finished.");
        List<Move> moves = new ArrayList<Move>();
        Set<String> seen = new HashSet<String>();
        while (true) {
            String line = in.require("  transition:");
            if (InputSource.isEnd(line)) {
                break;
            }
            List<String> t = InputSource.transitionTokens(line);
            if (t.size() != 5) {
                throw in.error("a PDA transition needs 5 parts (state input pop next-state push), got: " + line);
            }
            String from = in.parseState(t.get(0), states);
            Character input = symbol(in, t.get(1), sigma, "input");
            Character pop = symbol(in, t.get(2), gamma, "stack");
            String to = in.parseState(t.get(3), states);
            String push = InputSource.isEpsilon(t.get(4)) ? "" : t.get(4);
            for (int i = 0; i < push.length(); i++) {
                if (!gamma.contains(push.charAt(i))) {
                    throw in.error("'" + push.charAt(i) + "' in the push string is not in the stack alphabet "
                            + chars(gamma));
                }
            }
            Move m = new Move(moves.size() + 1, from, input, pop, to, push);
            if (seen.add(m.toString())) {
                moves.add(m);
            }
        }
        return new PDASimulation(states, sigma, gamma, start, z.charAt(0), finals, byFinal, moves);
    }

    private static Character symbol(InputSource in, String token, List<Character> alphabet, String what) {
        if (InputSource.isEpsilon(token)) {
            return null;
        }
        if (token.length() != 1 || !alphabet.contains(token.charAt(0))) {
            throw in.error("'" + token + "' is not in the " + what + " alphabet " + chars(alphabet)
                    + " (use 'eps' for none)");
        }
        return token.charAt(0);
    }

    void printDefinition() {
        Util.heading("The PDA");
        System.out.println("M = (Q, Sigma, Gamma, delta, q0, z, F) where");
        System.out.println("  Q     = " + Util.setString(states));
        System.out.println("  Sigma = " + chars(inputAlphabet) + "   (input alphabet)");
        System.out.println("  Gamma = " + chars(stackAlphabet) + "   (stack alphabet)");
        System.out.println("  q0    = " + start);
        System.out.println("  z     = " + initialStack + "   (initial stack symbol)");
        System.out.println("  F     = " + Util.setString(finals));
        System.out.println("  Acceptance: " + (byFinalState
                ? "by FINAL STATE (input fully read and the PDA is in a state of F)"
                : "by EMPTY STACK (input fully read and the stack is empty)"));
        System.out.println("  Transitions (push strings are written with the top of the stack on the left):");
        TablePrinter t = new TablePrinter("#", "Transition", "Meaning");
        for (Move m : moves) {
            t.addRow(m.number, m.toString(), m.operation() + ", go to " + m.to);
        }
        t.print();
        System.out.println("  " + determinism());
    }

    /** Checks the conditions for a deterministic PDA (DPDA). */
    String determinism() {
        for (Move m : moves) {
            if (m.pop == null) {
                return "(Determinism check skipped: some moves do not pop the stack.)";
            }
        }
        Map<String, List<Move>> byKey = new LinkedHashMap<String, List<Move>>();
        for (Move m : moves) {
            String key = m.from + '\u0001' + m.pop;
            List<Move> list = byKey.get(key);
            if (list == null) {
                list = new ArrayList<Move>();
                byKey.put(key, list);
            }
            list.add(m);
        }
        for (List<Move> list : byKey.values()) {
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    Move x = list.get(i);
                    Move y = list.get(j);
                    if (x.input == null || y.input == null || x.input.equals(y.input)) {
                        return "This PDA is NONDETERMINISTIC: moves #" + x.number + " and #" + y.number
                                + " can both apply in the same situation. (The simulation tries every choice.)";
                    }
                }
            }
        }
        return "This PDA is DETERMINISTIC (a DPDA): in every situation at most one move applies.";
    }

    private boolean isAccepting(Config c, int length) {
        if (c.position != length) {
            return false;
        }
        return byFinalState ? finals.contains(c.state) : c.stack.isEmpty();
    }

    /** Simulates the PDA on w. Returns true if some computation accepts w. */
    boolean simulate(String w) {
        Util.heading("Simulating the PDA on w = " + Util.quote(w));
        for (int i = 0; i < w.length(); i++) {
            if (!inputAlphabet.contains(w.charAt(i))) {
                System.out.println("  Symbol '" + w.charAt(i) + "' is not in the input alphabet " + chars(inputAlphabet)
                        + ".");
                System.out.println("  Result: " + Util.quote(w) + " is REJECTED");
                return false;
            }
        }

        Config first = new Config(start, 0, String.valueOf(initialStack), null, null);
        Deque<Config> queue = new ArrayDeque<Config>();
        Set<String> visited = new HashSet<String>();
        queue.add(first);
        visited.add(first.key());
        Config accepting = null;
        boolean limited = false;
        int farthest = 0;

        search:
        while (!queue.isEmpty()) {
            Config c = queue.poll();
            farthest = Math.max(farthest, c.position);
            if (isAccepting(c, w.length())) {
                accepting = c;
                break;
            }
            for (Move m : movesFrom.get(c.state)) {
                int position = c.position;
                if (m.input != null) {
                    if (position >= w.length() || w.charAt(position) != m.input) {
                        continue;
                    }
                    position++;
                }
                String stack = c.stack;
                if (m.pop != null) {
                    if (stack.isEmpty() || stack.charAt(0) != m.pop) {
                        continue;
                    }
                    stack = stack.substring(1);
                }
                stack = m.push + stack;
                if (stack.length() > MAX_STACK) {
                    limited = true;
                    continue;
                }
                Config next = new Config(m.to, position, stack, c, m);
                if (visited.add(next.key())) {
                    if (visited.size() > MAX_CONFIGURATIONS) {
                        limited = true;
                        break search;
                    }
                    queue.add(next);
                }
            }
        }

        if (accepting != null) {
            printComputation(accepting, w);
            System.out.println("  The input is completely read and "
                    + (byFinalState ? "the PDA is in final state " + accepting.state + "."
                    : "the stack is empty."));
            System.out.println("  (" + visited.size() + " configuration(s) explored.)");
            System.out.println("  Result: " + Util.quote(w) + " is ACCEPTED");
            return true;
        }
        System.out.println("  Every possible computation was explored (" + visited.size() + " configuration(s)).");
        System.out.println("  The farthest any computation got was " + farthest + " of " + w.length()
                + " input symbol(s)" + (farthest > 0 ? " (\"" + w.substring(0, farthest) + "\" read)" : "") + ".");
        if (limited) {
            System.out.println("  WARNING: the search reached its limit (" + MAX_CONFIGURATIONS + " configurations or "
                    + MAX_STACK + " stack symbols), probably because of endless epsilon-moves.");
            System.out.println("  No accepting computation was found within this limit.");
            System.out.println("  Result: " + Util.quote(w) + " is UNDECIDED (search limit reached)");
        } else {
            System.out.println("  None of them ends with the input fully read and "
                    + (byFinalState ? "the PDA in a final state." : "the stack empty."));
            System.out.println("  Result: " + Util.quote(w) + " is REJECTED");
        }
        return false;
    }

    /** Prints the accepting computation as a table and as a chain of instantaneous descriptions. */
    private void printComputation(Config last, String w) {
        List<Config> path = new ArrayList<Config>();
        for (Config c = last; c != null; c = c.parent) {
            path.add(0, c);
        }
        System.out.println("  Accepting computation found (" + (path.size() - 1) + " move(s)).");
        System.out.println("  Stack is shown with its TOP on the LEFT.");
        TablePrinter t = new TablePrinter("Step", "Move used", "Stack operation", "State", "Unread input", "Stack");
        for (int i = 0; i < path.size(); i++) {
            Config c = path.get(i);
            if (i == 0) {
                t.addRow(0, "(start)", "-", c.state, Util.show(w.substring(c.position)), Util.show(c.stack));
            } else {
                t.addRow(i, "#" + c.via.number + " " + c.via, c.via.operation(), c.state,
                        Util.show(w.substring(c.position)), Util.show(c.stack));
            }
        }
        t.print();
        System.out.println("  Instantaneous descriptions:");
        StringBuilder sb = new StringBuilder("    ");
        int lineLength = 4;
        for (int i = 0; i < path.size(); i++) {
            Config c = path.get(i);
            String id = "(" + c.state + ", " + Util.show(w.substring(c.position)) + ", " + Util.show(c.stack) + ")";
            String piece = (i == 0 ? "" : " |- ") + id;
            if (lineLength + piece.length() > 100 && i > 0) {
                System.out.println(sb);
                sb = new StringBuilder("    ");
                lineLength = 4;
                piece = "|- " + id;
            }
            sb.append(piece);
            lineLength += piece.length();
        }
        System.out.println(sb);
    }

    private static String chars(List<Character> symbols) {
        List<String> s = new ArrayList<String>();
        for (char c : symbols) {
            s.add(String.valueOf(c));
        }
        return Util.setString(s);
    }
}
