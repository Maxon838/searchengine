package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IndexingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public IndexingResponse catchIndexingException(IndexingException e) {

    return new IndexingResponse(false, e.getMessage());
}
    @ExceptionHandler(BadIndexingRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public IndexingResponse catchBadRequestException (BadIndexingRequestException e) {

        return new IndexingResponse(false, e.getMessage());
    }
    @ExceptionHandler(IndexNotReadyException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public SearchingResponse catchIndexNotReadyException (IndexNotReadyException e) {

        return new SearchingResponse(false, e.getMessage());
    }
    @ExceptionHandler(SiteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public SearchingResponse catchSiteNotFoundException (SiteNotFoundException e) {

        return new SearchingResponse(false, e.getMessage());
    }
    @ExceptionHandler(BadSearchingRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public SearchingResponse catchBadSearchingRequestException (BadSearchingRequestException e) {

        return new SearchingResponse(false, e.getMessage());
    }
}
