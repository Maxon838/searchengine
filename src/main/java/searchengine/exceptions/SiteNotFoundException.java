package searchengine.exceptions;

public class SiteNotFoundException extends RuntimeException{
    public SiteNotFoundException() {
        super("Запрашиваемый сайт не найден");
    }

    public SiteNotFoundException(String message) {
        super(message);
    }

    public SiteNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
