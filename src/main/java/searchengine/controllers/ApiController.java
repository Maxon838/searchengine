package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService  = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
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

    @PostMapping("/indexPage/")
    public ResponseEntity<?> indexPage (@RequestParam String url)
    {
        return new ResponseEntity<>(indexingService.lonePageIndexing(url), HttpStatus.OK);
    }
    //http://localhost:8080/api/indexPage/?url=https://splitstone.ru/gdekupit

}
