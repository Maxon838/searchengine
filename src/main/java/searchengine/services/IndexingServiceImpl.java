package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.PersistentObjectException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.PageIndexing;
import searchengine.lemma.LemmaFinder;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Setter
@Getter
public class IndexingServiceImpl implements IndexingService{

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private ForkJoinPool pool = new ForkJoinPool();
    public static final AtomicBoolean stopFlag = new AtomicBoolean(false);
    public static final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public IndexingResponse startIndexing () throws NoSuchElementException, NullPointerException
    {
        if (isRunning.get()) return new IndexingResponse(false, "Индексация уже запущена");
        stopFlag.set(false);
        deleteRowsFromTablesONStartup();
        addRowsToSiteTable();

        for (Site site : sites.getSites())
        {
            pool.execute(
            new PageIndexing(
            siteRepository.findByMainPageURL(site.getUrl()).get().getId(), site.getUrl(), pageRepository, siteRepository, lemmaRepository, indexRepository, true));
        }
        isRunning.set(true);

        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse stopIndexing()
    {
        if (!isRunning.get()) return new IndexingResponse(false, "Индексация не запущена");
        stopFlag.set(true);
        isRunning.set(false);

        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse lonePageIndexing(String pageURL)
    {
        if (isRunning.get()) return new IndexingResponse(false, "Запущена индексация сайтов");

        for (Site site : sites.getSites())
        {
            if (pageURL.contains(site.getUrl().replaceAll("https://", "")))
            {
                new Thread (()->
                {
                    String pagePath = pageURL.replaceAll("www.", "").replaceAll(site.getUrl(), "");

                    if (siteRepository.findByMainPageURL(site.getUrl()).isPresent())
                    {
                        int siteId = siteRepository.findByMainPageURL(site.getUrl()).get().getId();

                        if (pageRepository.findByPagePath(pagePath).isPresent())
                        {
                            List<IndexEntity> deletedIndexTableRows = indexRepository.findAllByPageEntity(pageRepository.findByPagePath(pagePath).get());

                            try {
                                pageRepository.delete(pageRepository.findByPagePath(pagePath).get());
                            } catch (NoSuchElementException ex) {
                                System.out.println(ex.getMessage());
                            }

                            deleteLemmas(deletedIndexTableRows);
                        }

                        HashMap<Integer, String> htmlAndResponseCode = parseSinglePage(pageURL);
                        addPageEntity(pagePath, htmlAndResponseCode, siteId);
                        Integer responseCode = htmlAndResponseCode.keySet().stream().findFirst().get();

                        saveLemmasAndIndexesToTables(siteId, htmlAndResponseCode.get(responseCode), pagePath);
                    }
                    else
                    {
                        addSiteEntity(site);

                        HashMap<Integer, String> htmlAndResponseCode = parseSinglePage(pageURL);
                        addPageEntity(pagePath, htmlAndResponseCode, siteRepository.findByMainPageURL(site.getUrl()).get().getId());
                        Integer responseCode = htmlAndResponseCode.keySet().stream().findFirst().get();

                        saveLemmasAndIndexesToTables(siteRepository.findByMainPageURL(site.getUrl()).get().getId(), htmlAndResponseCode.get(responseCode), pagePath);
                    }
                }).start();

                return new IndexingResponse(true, "");
            }
        }

        return new IndexingResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }

    private void deleteRowsFromTablesONStartup()
    {
        for (Site site : sites.getSites())
        {
            try {
                siteRepository.deleteById(siteRepository.findByName(site.getName()).get().getId());
            }
            catch (Throwable e)
            {
                System.err.println("No such Row");
            }
        }
    }

    private void addRowsToSiteTable()
    {
        for (Site site : sites.getSites())
        {
            addSiteEntity(site);
        }
    }

    private void addSiteEntity (Site site)
    {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setMainPageURL(site.getUrl());
        siteEntity.setStatus(Status.INDEXING);
        siteRepository.save(siteEntity);
    }

    private void addPageEntity (String pagePath, HashMap<Integer, String> htmlAndResponseCode, int siteId)
    {
        PageEntity page = new PageEntity();
        page.setPagePath(pagePath);
        Integer responseCode = htmlAndResponseCode.keySet().stream().findFirst().get();
        page.setPageResponseHttpCode(responseCode);
        String htmlCode = htmlAndResponseCode.get(responseCode);
        page.setPageContentHttpCode(htmlCode);
        page.setSiteId(siteRepository.findById(siteId).get());
        pageRepository.save(page);
    }

    private HashMap<Integer, String> parseSinglePage (String url)
    {
        HashMap<Integer, String> htmlAndResponseCode = new HashMap<>();
        Document doc = new Document(url);
        int responseCode = 0;

        try {
            doc = Jsoup.connect(url).get();
            responseCode = doc.connection().response().statusCode();
            htmlAndResponseCode.put(responseCode, doc.html());
        }
        catch (HttpStatusException e)
        {
            System.out.println(e.getMessage());
            responseCode = e.getStatusCode();
            htmlAndResponseCode.put(responseCode, doc.html());
        }
        catch (IOException | NullPointerException ex) {

            ex.printStackTrace();
            htmlAndResponseCode.put(responseCode, doc.html());
        }

        return htmlAndResponseCode;
    }

    private void deleteLemmas (List<IndexEntity> deletedIndexTableRows)
    {
        for (IndexEntity index : deletedIndexTableRows)
        {
            LemmaEntity lemmaEntity = index.getLemmaEntity();
            LemmaEntity editingLemmaEntity = lemmaRepository.findById(lemmaEntity.getId()).get();

            int lemmaFrequency = editingLemmaEntity.getFrequency();
            if (lemmaFrequency > 1)
            {
                editingLemmaEntity.setFrequency(lemmaFrequency - 1);
                lemmaRepository.save(editingLemmaEntity);
            }
            else lemmaRepository.delete(editingLemmaEntity);
        }
    }
    private void saveLemmasAndIndexesToTables (int siteId, String htmlCode, String pagePath)
    {
        HashMap<String, Integer> lemmasMap = new HashMap<>();
        LemmaFinder lemmaFinder = new LemmaFinder();
        PageEntity pageEntity = pageRepository.findByPagePath(pagePath).get();

        try
        {
            lemmasMap = lemmaFinder.getLemmas(htmlCode);
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage());
        }

        for (String lemma : lemmasMap.keySet())
        {
            LemmaEntity lemmaEntity = new LemmaEntity();
            List <LemmaEntity> lemmasFromTableList = new ArrayList<>();
            lemmasFromTableList = lemmaRepository.findAllByLemma(lemma);

            if (!lemmasFromTableList.isEmpty())
            {
                for (LemmaEntity lemmaEntityTransitional : lemmasFromTableList)
                {
                    if (lemmaEntityTransitional.getSiteId().getId() == siteId)
                    {
                        lemmaEntityTransitional.setFrequency(lemmaEntityTransitional.getFrequency() + 1);
                        lemmaRepository.saveAndFlush(lemmaEntityTransitional);
                        saveRowsToIndexTable(lemmaEntityTransitional, pageEntity, lemmasMap.get(lemma));
                    }
                }
            }
            else
            {
                lemmaEntity.setLemma(lemma);
                lemmaEntity.setFrequency(1);
                try {
                    lemmaEntity.setSiteId(siteRepository.findById(siteId).get());
                } catch (PersistentObjectException | InvalidDataAccessApiUsageException ex) {
                    System.out.println(ex.getMessage());
                }

                lemmaRepository.saveAndFlush(lemmaEntity);
                saveRowsToIndexTable(lemmaEntity, pageEntity, lemmasMap.get(lemma));
            }
        }
    }

    private void saveRowsToIndexTable (LemmaEntity lemmaEntity, PageEntity pageEntity, int rank)
    {
        IndexEntity indexEntity = new IndexEntity();

        try {
            indexEntity.setLemmaEntity(lemmaEntity);
            indexEntity.setPageEntity(pageEntity);
            indexEntity.setRank(rank);

            indexRepository.saveAndFlush(indexEntity);
        } catch (PersistentObjectException | InvalidDataAccessApiUsageException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
