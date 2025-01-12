package orm.exceptions;

public class DuplicatedElementOnListException extends RuntimeException {
    public DuplicatedElementOnListException() {
    }

    public DuplicatedElementOnListException(String message) {
        super(message);
    }
}
