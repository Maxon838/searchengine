package searchengine.exceptions;

public class BadSearchingRequestException extends RuntimeException{
    public BadSearchingRequestException(String message) {
        super(message);
    }
}
