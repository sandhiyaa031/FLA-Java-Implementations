import java.io.IOException;
import java.util.List;

/**
 * TASK 1: DFA SIMULATION
 *
 * Reads a DFA M = (Q, Sigma, delta, q0, F) and then any number of input strings.
 * For each string w it applies delta one symbol at a time, printing every step,
 * and reports whether delta*(q0, w) is a final state (ACCEPTED) or not (REJECTED).
 *
 * Usage:  java DFASimulation                 (type the DFA at the keyboard)
 *         java DFASimulation input-file.txt  (read the DFA and strings from a file)
 */
public class DFASimulation {

    public static void main(String[] args) {
        Util.banner("TASK 1: DFA SIMULATION",
                "Decides whether each input string is accepted or rejected by a DFA.");
        try {
            InputSource in = new InputSource(args);
            DFA dfa = DFA.read(in, "");

            Util.heading("The DFA");
            dfa.printDefinition("M");
            List<String> undefined = dfa.undefinedTransitions();
            if (!undefined.isEmpty()) {
                System.out.println("  Note: " + undefined.size() + " transition(s) are not defined: " + undefined);
                System.out.println("  If the DFA needs one of them, it has no move and the string is REJECTED");
                System.out.println("  (this is the same as going to a non-final dead state).");
            }

            int tested = 0;
            int accepted = 0;
            while (true) {
                String line = in.nextLine(
                        "\nEnter an input string ('eps' = empty string, 'end' to stop):");
                if (line == null || InputSource.isEnd(line)) {
                    break;
                }
                String w = InputSource.parseWord(line);
                if (simulate(dfa, w)) {
                    accepted++;
                }
                tested++;
            }
            Util.heading("Summary");
            System.out.println("  Strings tested: " + tested + ", accepted: " + accepted
                    + ", rejected: " + (tested - accepted));
        } catch (InputException e) {
            System.out.println();
            System.out.println("Input error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Could not read the input: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Runs the DFA on w, printing each step. Returns true if w is accepted. */
    static boolean simulate(DFA dfa, String w) {
        Util.heading("Simulating M on w = " + Util.quote(w) + ", |w| = " + w.length());
        String q = dfa.getStart();
        String failure = null;
        StringBuilder path = new StringBuilder(q);

        if (w.isEmpty()) {
            System.out.println("  w is empty, so no symbol is read: the DFA stays in the start state.");
        } else {
            TablePrinter steps = new TablePrinter("Step", "Current state", "Symbol read", "Transition", "Next state");
            for (int i = 0; i < w.length(); i++) {
                char c = w.charAt(i);
                if (!dfa.getAlphabet().contains(c)) {
                    steps.addRow(i + 1, q, c, "-", "(stop)");
                    failure = "symbol '" + c + "' (position " + (i + 1) + ") is not in the input alphabet";
                    break;
                }
                String r = dfa.next(q, c);
                if (r == null) {
                    steps.addRow(i + 1, q, c, "delta(" + q + ", " + c + ") undefined", "(stop)");
                    failure = "delta(" + q + ", " + c + ") is undefined, so the DFA has no move";
                    break;
                }
                steps.addRow(i + 1, q, c, "delta(" + q + ", " + c + ") = " + r, r);
                path.append(" --").append(c).append("--> ").append(r);
                q = r;
            }
            steps.print();
            System.out.println("  Path: " + path);
        }

        boolean accepted = failure == null && dfa.isFinal(q);
        if (failure != null) {
            System.out.println("  Reason: " + failure + ".");
        } else {
            System.out.println("  delta*(" + dfa.getStart() + ", " + Util.show(w) + ") = " + q
                    + ", which is " + (accepted ? "a final state" : "NOT a final state")
                    + " (F = " + Util.setString(dfa.getFinals()) + ").");
        }
        System.out.println("  Result: " + Util.quote(w) + " is " + (accepted ? "ACCEPTED" : "REJECTED"));
        return accepted;
    }
}
