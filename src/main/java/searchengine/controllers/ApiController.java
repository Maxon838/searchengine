package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {return statisticsService.getStatistics();}
    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing ()
    {
        return indexingService.startIndexing();
    }
    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing ()
    {
        return indexingService.stopIndexing();
    }
    @PostMapping("/indexPage")
    public IndexingResponse indexPage (@RequestBody Map<String, String> request) {
        String url = request.get("url");
        return indexingService.lonePageIndexing(url);
    }
    @GetMapping("/search")
    public SearchingResponse search (@RequestParam String query, @RequestParam(defaultValue = "") String site)
    {
        System.out.println("Query = " + query + " Site = " + site);
        return searchingService.search(query, site);
    }
}
