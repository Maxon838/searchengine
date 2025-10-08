package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.SiteNotFoundException;
import searchengine.exceptions.IndexNotReadyException;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchingService searchingService) {
        this.statisticsService = statisticsService;
        this.indexingService  = indexingService;
        this.searchingService = searchingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics()
    {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing ()
    {
        return new ResponseEntity<>(indexingService.startIndexing(), HttpStatus.OK);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing ()
    {
        return new ResponseEntity<>(indexingService.stopIndexing(), HttpStatus.OK);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage (@RequestBody Map<String, String> request)
    {
        String url = request.get("url");
        System.out.println(url);
        return new ResponseEntity<>(indexingService.lonePageIndexing(url), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchingResponse> search (@RequestParam String query, @RequestParam(defaultValue = "") String site)
    {
        System.out.println("Query = " + query + " Site = " + site);

        if (query == null || query.trim().isEmpty()) {
            SearchingResponse searchingResponse = new SearchingResponse();
            searchingResponse.setResult(false);
            searchingResponse.setError("Поисковый запрос не может быть пустым");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(searchingResponse);
        }

        try {
            SearchingResponse result = searchingService.search(query, site);

            return ResponseEntity.ok(result);

        } catch (SiteNotFoundException e) {
            SearchingResponse response = new SearchingResponse();
            response.setResult(false);
            response.setError("Запрашиваемый сайт не найден");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IndexNotReadyException e) {
            SearchingResponse response = new SearchingResponse();
            response.setResult(false);
            response.setError("Индексация ещё не завершена");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);

        } catch (Exception e) {
            SearchingResponse response = new SearchingResponse();
            response.setResult(false);
            response.setError("Произошла внутренняя ошибка сервера");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }
}
