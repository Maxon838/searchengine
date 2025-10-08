package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.SitesList;
import searchengine.dto.searching.Response;
import searchengine.dto.searching.SearchingResponse;
import searchengine.exceptions.IndexNotReadyException;
import searchengine.exceptions.SiteNotFoundException;
import searchengine.lemma.LemmaFinder;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Setter
@Getter
public class SearchingServiceImpl implements SearchingService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;

    @Override
    public SearchingResponse search(String query, String siteURL)
    {
        SearchingResponse searchingResponse = new SearchingResponse();
        boolean isSiteRelevance = true;

        if (IndexingServiceImpl.isRunning.get()==true) throw new IndexNotReadyException();
        if (!(siteURL == null || siteURL.isBlank())) isSiteRelevance = relevanceSite(siteURL);
        if (!isSiteRelevance) throw new SiteNotFoundException();

        LemmaFinder lemmaFinder = new LemmaFinder();
        HashMap<String, Integer> lemmasFromQueryMap = new HashMap<>();
        TreeMap<String, Integer> LemmasWithFrequencyFromQueryMap = new TreeMap<>();
        List<Integer> commonPages = new ArrayList<>();
        int count = 0;

        try {
            lemmasFromQueryMap = lemmaFinder.getLemmas(query.toLowerCase());
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        if (lemmasFromQueryMap.isEmpty()) return searchingResponse.setNoResults();
        for (String lemma: lemmasFromQueryMap.keySet()) {
            int result = 0;
            if (!lemmaRepository.findAllByLemma(lemma).isEmpty()) {
                try {
                    result = (siteURL == null || siteURL.isBlank())
                        ? getLemmasFrequencyFromTable(lemma)
                        : getLemmasFrequencyFromTable(lemma, siteRepository.findByMainPageURL(siteURL).get().getId());
                }
                catch (SQLException ex) {
                    ex.getSQLState();
                }
            } else {
                return searchingResponse.setNoResults();
            }

            count = count + result;
            LemmasWithFrequencyFromQueryMap.put(lemma, result);
        }

        LemmasWithFrequencyFromQueryMap.keySet().
                stream().forEach(o-> System.out.println(o + " - " + LemmasWithFrequencyFromQueryMap.get(o)));

        LemmasWithFrequencyFromQueryMap.values().removeIf(value -> value == 0 || value > 20);
        if (LemmasWithFrequencyFromQueryMap.isEmpty()) return searchingResponse.setNoResults();
        List<Map.Entry<String, Integer>> sortedEntriesList = LemmasWithFrequencyFromQueryMap.entrySet()
                .stream().sorted(Map.Entry.comparingByValue()).toList();
        List<String> sortedLemmasList = sortedEntriesList.stream().map(entry -> entry.getKey()).toList();

        if (!sortedLemmasList.isEmpty())
        {
            commonPages = (siteURL == null || siteURL.isBlank())
                    ? findPagesByLemmas(sortedLemmasList.get(0), sortedLemmasList)
                    : findPagesByLemmas(sortedLemmasList.get(0), sortedLemmasList, siteRepository.findByMainPageURL(siteURL).get().getId());
        }


        return calculateAndWriteInSearchingResponse (true, commonPages, sortedLemmasList);
    }

    private int getLemmasFrequencyFromTable(String lemma) throws SQLException {

        String sql = "SELECT SUM(`frequency`) AS `sum` FROM lemma WHERE `lemma` = ?";

        try (
                Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/search_engine?user=engine_user&password=access");
                PreparedStatement ps = connection.prepareStatement(sql)
        ) {
            ps.setString(1, lemma);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("sum");
                } else {
                    return 0;
                }
            }
        }
    }

    private int getLemmasFrequencyFromTable(String lemma, int site_id) throws SQLException {

        String sql = "SELECT * FROM lemma WHERE `lemma` = ? AND `site_id` = ?";

        try (
                Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/search_engine?user=engine_user&password=access");
                PreparedStatement ps = connection.prepareStatement(sql)
        ) {
            ps.setNString(1, lemma);
            ps.setInt(2, site_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("frequency");
                } else {
                    return 0;
                }
            }
        }
    }

    private List<Integer> findPagesByLemmas(String lemma, List<String> lemmas) {

        List<LemmaEntity> firstLemmaList = lemmaRepository.findAllByLemma(lemma);
        List<IndexEntity> indexEntitiesJointList = new ArrayList<>();
        List<Integer> pagesIdList = new ArrayList<>();

        for (LemmaEntity lemmaEntity : firstLemmaList)
        {
            List <IndexEntity> indexEntityListOfSingleLemmaEntity = indexRepository.findAllByLemmaEntity(lemmaEntity);
            indexEntitiesJointList.addAll(indexEntityListOfSingleLemmaEntity);
        }

        pagesIdList = indexEntitiesJointList 
                .stream()
                .map(o -> o.getPageEntity().getId())
                .toList();

        if (lemmas.size() > 1)
        {
            return findPagesByLemmas(lemmas, 1, pagesIdList);
        }

        return pagesIdList;
    }

    private List<Integer> findPagesByLemmas(String lemma, List<String> lemmas, int siteId) {

        List<LemmaEntity> firstLemmaList = lemmaRepository.findAllByLemma(lemma);
        List<Integer> pagesIdList = new ArrayList<>();

        List<LemmaEntity> lemmaEntityOfSite = firstLemmaList
                .stream()
                .filter(o -> o.getSiteId().getId() == siteId)
                .toList();

        List <IndexEntity> indexEntityListOfSingleLemmaEntity = indexRepository
                .findAllByLemmaEntity(lemmaEntityOfSite.get(0));

        pagesIdList = indexEntityListOfSingleLemmaEntity
                .stream()
                .map(o -> o.getPageEntity().getId())
                .toList();

        if (lemmas.size() > 1)
        {
            return findPagesByLemmas(lemmas, 1, pagesIdList);
        }

        return pagesIdList;
    }

    private List<Integer> findPagesByLemmas(List<String> lemmas, int level, List<Integer> pagesIdList) {

        List<Integer> bufferPagesList = indexRepository.findPagesWithPagesAndLemmas(lemmas.get(level), pagesIdList);
        level = level + 1;

        if ((lemmas.size() > level)&&(!bufferPagesList.isEmpty()))
        {
            return findPagesByLemmas(lemmas, level, bufferPagesList);
        }

        return bufferPagesList;
    }

    private boolean relevanceSite (String siteURL) {

        boolean result = sites.getSites().stream()
                .anyMatch(site-> site.getUrl().replaceAll("https://", "")
                        .contains(siteURL.replaceAll("https://", "").replaceAll("www.", "")));

        return result;
    }
    private SearchingResponse calculateAndWriteInSearchingResponse (boolean result, List<Integer> commonPages, List<String> lemmasList) {

        List<PageEntity> resultPageEntitiesList = pageRepository.findAllById(commonPages);
        List<Response> responsesList = new ArrayList<>();
        SearchingResponse searchingResponse = new SearchingResponse();
        LemmaFinder lemmaFinder = new LemmaFinder();
        double maxAbsoluteRelevance = 0;

        for (PageEntity pageEntity : resultPageEntitiesList)
        {
            String snippet = "";
            Response response = new Response();
            SiteEntity siteEntity = pageEntity.getSiteId();
            response.setSiteName(siteEntity.getName());
            response.setSite(siteEntity.getMainPageURL());
            response.setUri(pageEntity.getPagePath());
            response.setTitle(getTitle(pageEntity.getPageContentHttpCode()));
            try {
                snippet = lemmaFinder.getSnippet(lemmasList, pageEntity.getPageContentHttpCode());
            }
            catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            response.setSnippet(snippet);
            double absoluteRelevance = calculateAbsoluteRelevance(pageEntity, lemmasList);
            response.setRelevance(absoluteRelevance);
            maxAbsoluteRelevance = Math.max(absoluteRelevance, maxAbsoluteRelevance);
            responsesList.add(response);
        }
        calculateRelativeRelevance(maxAbsoluteRelevance, responsesList);
        searchingResponse.setResult(true);
        searchingResponse.setCount(resultPageEntitiesList.size());
        responsesList.sort(Comparator.comparingDouble(Response::getRelevance).reversed());
        searchingResponse.setData(responsesList);

        return searchingResponse;
    }

    private String getTitle (String htmlText) {

        try {
            Document doc = Jsoup.parse(htmlText);
            String title = doc.title();

            return title;
        }
        catch (Exception ex) {
            System.out.println("Проблема чтения html-документа: " + ex.getMessage());

            return null;
        }
    }

    private double calculateAbsoluteRelevance(PageEntity pageEntity, List<String> lemmasList) {

        float absoluteRelevance = 0.0f;
        for (String lemma : lemmasList)
        {
            List<Integer> lemmasIds = lemmaRepository.findAllByLemma(lemma)
                    .stream().
                    map(lemmaEntity -> lemmaEntity.getId())
                    .toList();
            String inSQL = String.join(",", Collections.nCopies(lemmasIds.size(), "?"));
            String sql = "SELECT `rank_field` FROM index_table WHERE `page_id` = ? AND lemma_id IN (" + inSQL + ")";

            try (
                    Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/search_engine?user=engine_user&password=access");
                    PreparedStatement ps = connection.prepareStatement(sql)
            ) {
                ps.setInt(1, pageEntity.getId());
                for (int i = 0; i < lemmasIds.size(); i++) {
                    ps.setInt(i + 2, lemmasIds.get(i));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) absoluteRelevance += rs.getFloat("rank_field");
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return absoluteRelevance;
    }

    private void calculateRelativeRelevance(double maxAbsoluteRelevance, List<Response> responseList) {

        for (Response response : responseList) {
            response.setRelevance(response.getRelevance()/maxAbsoluteRelevance);
        }
    }
}
