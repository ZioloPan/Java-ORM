package orm.exceptions;

public class InvalidStringContainerValueException extends RuntimeException {
    public InvalidStringContainerValueException() {
    }

    public InvalidStringContainerValueException(String message) {
        super("Bad value:" + message);
    }
}
