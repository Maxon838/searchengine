package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        total.setLemmas((int) lemmaRepository.count());
        total.setPages((int) pageRepository.count());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();

        for(int i = 0; i < sitesList.size(); i++) {

            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            try {
                int siteId = siteRepository.findByMainPageURL(site.getUrl()).get().getId();
                item.setUrl(site.getUrl());
                item.setName(site.getName());
                item.setStatus(siteRepository.findByMainPageURL(site.getUrl()).get().getStatus().toString());
                item.setError(siteRepository.findByMainPageURL(site.getUrl()).get().getLastError());
                item.setStatusTime(siteRepository.findByMainPageURL(site.getUrl()).get().getStatusTime().toEpochMilli());
                item.setPages(countRowsBySiteId(siteId, "page"));
                item.setLemmas(countRowsBySiteId(siteId, "lemma"));
            }
            catch (NoSuchElementException | SQLException ex) {
                System.out.println(ex.getLocalizedMessage());
            }

            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }

    private int countRowsBySiteId(int siteId, String table) throws SQLException
    {

        Connection connection = null;
        ResultSet rs = null;
        int result = 0;

        if (connection == null) {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/search_engine?user=engine_user&password=access");
            rs = connection.createStatement().executeQuery("SELECT COUNT(*) c FROM " + table + " WHERE site_id = " + siteId);
        }
        if (rs.next()) result = rs.getInt("c");
        connection.close();

        return result;
    }
}
