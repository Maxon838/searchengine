package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexPageRequest;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;
    private static final Logger log = LoggerFactory.getLogger(IndexingServiceImpl.class);
    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }
    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing () {
        return indexingService.startIndexing();
    }
    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing () {
        return indexingService.stopIndexing();
    }
    @PostMapping("/indexPage")
    public IndexingResponse indexPage (@RequestBody IndexPageRequest request) {
        return indexingService.lonePageIndexing(request.getUrl());
    }
    @GetMapping("/search")
    public SearchingResponse search (@RequestParam String query, @RequestParam(defaultValue = "") String site) {
        log.debug("Query {} Site {}", query, site);
        return searchingService.search(query, site);
    }
}