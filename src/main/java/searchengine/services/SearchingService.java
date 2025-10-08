package searchengine.services;

import searchengine.dto.searching.SearchingResponse;

public interface SearchingService {
    SearchingResponse search(String query, String site);
}
