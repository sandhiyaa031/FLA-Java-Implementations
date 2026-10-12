import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A nondeterministic finite automaton M = (Q, Sigma, delta, q0, F), optionally with
 * epsilon-transitions. delta(q, a) is a set of states.
 *
 * Sets of states are stored as BitSets: bit i stands for the i-th state in the order
 * the states were entered. This makes "the same subset" easy to recognise.
 */
public class NFA {

    private final List<String> states;
    private final List<Character> alphabet;
    private final String start;
    private final Set<String> finals;
    private final Map<String, Integer> index = new HashMap<String, Integer>();
    private final BitSet[][] delta;      // delta[state][symbol]
    private final BitSet[] epsilon;      // epsilon[state]
    private boolean hasEpsilon = false;

    public NFA(List<String> states, List<Character> alphabet, String start, Set<String> finals) {
        this.states = Collections.unmodifiableList(new ArrayList<String>(states));
        this.alphabet = Collections.unmodifiableList(new ArrayList<Character>(alphabet));
        this.start = start;
        this.finals = Collections.unmodifiableSet(new LinkedHashSet<String>(finals));
        for (int i = 0; i < states.size(); i++) {
            index.put(states.get(i), i);
        }
        delta = new BitSet[states.size()][alphabet.size()];
        epsilon = new BitSet[states.size()];
        for (int i = 0; i < states.size(); i++) {
            epsilon[i] = new BitSet();
            for (int j = 0; j < alphabet.size(); j++) {
                delta[i][j] = new BitSet();
            }
        }
    }

    public List<String> getStates() {
        return states;
    }

    public List<Character> getAlphabet() {
        return alphabet;
    }

    public String getStart() {
        return start;
    }

    public Set<String> getFinals() {
        return finals;
    }

    public boolean hasEpsilon() {
        return hasEpsilon;
    }

    /** Adds "to" to delta(from, symbol); symbol null means an epsilon-transition. */
    public void addTransition(String from, Character symbol, String to) {
        int f = index.get(from);
        int t = index.get(to);
        if (symbol == null) {
            epsilon[f].set(t);
            hasEpsilon = true;
        } else {
            delta[f][alphabet.indexOf(symbol)].set(t);
        }
    }

    public BitSet singleton(String q) {
        BitSet s = new BitSet();
        s.set(index.get(q));
        return s;
    }

    /** epsilon-closure(S): every state reachable from S using only epsilon-transitions. */
    public BitSet closure(BitSet set) {
        BitSet result = (BitSet) set.clone();
        Deque<Integer> stack = new ArrayDeque<Integer>();
        for (int q = set.nextSetBit(0); q >= 0; q = set.nextSetBit(q + 1)) {
            stack.push(q);
        }
        while (!stack.isEmpty()) {
            int q = stack.pop();
            BitSet next = epsilon[q];
            for (int r = next.nextSetBit(0); r >= 0; r = next.nextSetBit(r + 1)) {
                if (!result.get(r)) {
                    result.set(r);
                    stack.push(r);
                }
            }
        }
        return result;
    }

    /** move(S, a): the union of delta(q, a) for every q in S (no epsilon-closure). */
    public BitSet move(BitSet set, char symbol) {
        BitSet result = new BitSet();
        int k = alphabet.indexOf(symbol);
        if (k < 0) {
            return result;
        }
        for (int q = set.nextSetBit(0); q >= 0; q = set.nextSetBit(q + 1)) {
            result.or(delta[q][k]);
        }
        return result;
    }

    public BitSet startSet() {
        return closure(singleton(start));
    }

    public boolean containsFinal(BitSet set) {
        for (int q = set.nextSetBit(0); q >= 0; q = set.nextSetBit(q + 1)) {
            if (finals.contains(states.get(q))) {
                return true;
            }
        }
        return false;
    }

    /** Simulates the NFA by tracking the set of all states it could be in. */
    public boolean accepts(String w) {
        BitSet current = startSet();
        for (int i = 0; i < w.length(); i++) {
            if (!alphabet.contains(w.charAt(i))) {
                return false;
            }
            current = closure(move(current, w.charAt(i)));
            if (current.isEmpty()) {
                return false;
            }
        }
        return containsFinal(current);
    }

    /** Writes a set of states as {q0,q1}; the empty set is written {}. */
    public String name(BitSet set) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int q = set.nextSetBit(0); q >= 0; q = set.nextSetBit(q + 1)) {
            if (!first) {
                sb.append(',');
            }
            sb.append(states.get(q));
            first = false;
        }
        return sb.append('}').toString();
    }

    /** The final states that belong to the set. */
    public List<String> finalsIn(BitSet set) {
        List<String> result = new ArrayList<String>();
        for (int q = set.nextSetBit(0); q >= 0; q = set.nextSetBit(q + 1)) {
            if (finals.contains(states.get(q))) {
                result.add(states.get(q));
            }
        }
        return result;
    }

    public void printDefinition(String name) {
        List<String> symbols = new ArrayList<String>();
        for (char c : alphabet) {
            symbols.add(String.valueOf(c));
        }
        System.out.println(name + " = (Q, Sigma, delta, q0, F) where");
        System.out.println("  Q     = " + Util.setString(states) + "   (" + states.size() + " states)");
        System.out.println("  Sigma = " + Util.setString(symbols));
        System.out.println("  q0    = " + start);
        System.out.println("  F     = " + Util.setString(finals));
        System.out.println("  delta (transition table; each entry is a SET of states):");
        int columns = alphabet.size() + 2 + (hasEpsilon ? 1 : 0);
        String[] header = new String[columns];
        header[0] = "";
        header[1] = "State";
        for (int i = 0; i < alphabet.size(); i++) {
            header[i + 2] = String.valueOf(alphabet.get(i));
        }
        if (hasEpsilon) {
            header[columns - 1] = "eps";
        }
        TablePrinter table = new TablePrinter(header);
        for (int q = 0; q < states.size(); q++) {
            Object[] row = new Object[columns];
            String s = states.get(q);
            row[0] = (s.equals(start) ? "->" : "") + (finals.contains(s) ? "*" : "");
            row[1] = s;
            for (int i = 0; i < alphabet.size(); i++) {
                row[i + 2] = name(delta[q][i]);
            }
            if (hasEpsilon) {
                row[columns - 1] = name(epsilon[q]);
            }
            table.addRow(row);
        }
        table.print();
        System.out.println("  (-> marks the start state, * marks a final state, {} is the empty set)");
    }

    /**
     * Reads an NFA: states, alphabet, start state, final states, then transitions
     * "state symbol target1 target2 ..." (symbol 'eps' for an epsilon-transition),
     * ending with the word "end".
     */
    public static NFA read(InputSource in) throws IOException {
        List<String> states = in.parseStates(in.require("Enter the states (separated by spaces):"));
        List<Character> alphabet = in.parseAlphabet(
                in.require("Enter the input alphabet (single characters, do not include eps):"), "input");
        String start = in.parseSingleState(in.require("Enter the start state:"), states);
        Set<String> finals = in.parseStateSet(in.require("Enter the final states ('none' if there are none):"), states);
        NFA nfa = new NFA(states, alphabet, start, finals);

        in.hint("Enter the transitions, one per line, as:  state symbol target1 target2 ...");
        in.hint("Examples:  q0 a q0 q1   means delta(q0, a) = {q0, q1}");
        in.hint("           q0 eps q2    means an epsilon-transition from q0 to q2");
        in.hint("Type 'end' when finished.");
        while (true) {
            String line = in.require("  transition:");
            if (InputSource.isEnd(line)) {
                break;
            }
            List<String> t = InputSource.transitionTokens(line);
            if (t.size() < 3) {
                throw in.error("an NFA transition needs: state symbol target(s), got: " + line);
            }
            String from = in.parseState(t.get(0), states);
            Character symbol;
            if (InputSource.isEpsilon(t.get(1))) {
                symbol = null;
            } else if (t.get(1).length() == 1 && alphabet.contains(t.get(1).charAt(0))) {
                symbol = t.get(1).charAt(0);
            } else {
                throw in.error("'" + t.get(1) + "' is not an input symbol (use 'eps' for the empty string)");
            }
            for (int i = 2; i < t.size(); i++) {
                nfa.addTransition(from, symbol, in.parseState(t.get(i), states));
            }
        }
        return nfa;
    }
}
