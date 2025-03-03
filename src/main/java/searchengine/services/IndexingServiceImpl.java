package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.PageDTO;
import searchengine.dto.indexing.PageIndexing;
import searchengine.dto.indexing.SiteParser;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private ForkJoinPool pool = new ForkJoinPool();
    public static final AtomicBoolean stopFlag = new AtomicBoolean(false);

    @Override
    public IndexingResponse startIndexing () throws NoSuchElementException, NullPointerException
    {
        deleteRowsFromTableSiteONStartup();
        addRowsToSiteTable();

        SiteEntity site = siteRepository.getById(1);

        pool.execute(new PageIndexing(site.getMainPageURL(), pageRepository, siteRepository, true));
//        Thread thread = new Thread (() -> {
//        for (Site site : sites.getSites())
//        {
//            SiteParser siteParser = new SiteParser(site.getUrl(), siteRepository.findByName(site.getName()).get().getId());
//            ForkJoinPool pool = new ForkJoinPool(12);
//            pool.invoke(siteParser);
//            SiteEntity siteEntity = siteRepository.findByName(site.getName()).get();
//
//            for (PageDTO pageDTO : SiteParser.getPageDTOSet())
//            {
//                PageEntity pageEntity = new PageEntity();
//                pageEntity.setSiteId(siteEntity);
//                pageEntity.setPagePath(pageDTO.getPagePath());
//                pageEntity.setPageContentHttpCode(pageDTO.getPageContentHttpCode());
//                pageEntity.setPageResponseHttpCode(pageDTO.getPageResponseHttpCode());
//
//                pageRepository.save(pageEntity);
//            }
//
//            pool.shutdown();
//            SiteParser.resetLinksList();
//            SiteParser.getPageDTOSet();
//            siteEntity.setStatus(Status.INDEXED);
//            siteRepository.save(siteEntity);
//
//        }
//        });
//        thread.start();
       
        return new IndexingResponse(true, null);

    }

    public void stopIndexing() {
        stopFlag.set(true);
    }

    @Override
    public void deleteRowsFromTableSiteONStartup()
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

    @Override
    public void addRowsToSiteTable() {

        for (Site site : sites.getSites())
        {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setMainPageURL(site.getUrl());
            siteEntity.setStatus(Status.INDEXING);
            siteRepository.save(siteEntity);
        }

    }

}
