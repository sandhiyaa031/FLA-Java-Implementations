/** Thrown when the user's input does not describe a valid automaton or word. */
public class InputException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputException(String message) {
        super(message);
    }
}
