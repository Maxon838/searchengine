package searchengine.exceptions;

public class IndexNotReadyException extends RuntimeException{
    public IndexNotReadyException() {
        super("Индексация ещё не завершена");
    }

    public IndexNotReadyException(String message) {
        super(message);
    }

    public IndexNotReadyException(String message, Throwable cause) {
        super(message, cause);
    }
}
