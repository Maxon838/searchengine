package searchengine.dto.indexing;

import org.hibernate.PersistentObjectException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import searchengine.lemma.LemmaFinder;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

import static searchengine.services.IndexingServiceImpl.isRunning;
import static searchengine.services.IndexingServiceImpl.stopFlag;

public class PageIndexing extends RecursiveAction {

    private final int id;
    private final String url;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final boolean isRootTask;
    private int responseCode = 0;
    private static Set<String> globalLinksStorage = ConcurrentHashMap.newKeySet();
    private static ConcurrentHashMap<Integer, String> mainPages = new ConcurrentHashMap<>();
    private static final Set<Integer> responseErrorsCodesSet = new HashSet<>(Arrays.asList(0,400,401,403,404,405,406,407,408,409,410,411,412,413,500,501,502,503,504,505,507,508,509));
    public PageIndexing (int id, String url, PageRepository pageRepository, SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, boolean isRootTask)
    {
        this.id = id;
        this.url = url;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.isRootTask = isRootTask;
    }

    @Override
    protected void compute() {
        if (stopFlag.get()) {
            for (SiteEntity site : siteRepository.findAll()) {
                if (site.getStatus().equals(Status.INDEXING)) {
                    site.setStatus(Status.FAILED);
                    site.setLastError("Индексация прервана пользователем");
                    siteRepository.save(site);
                }
            }
            Thread.currentThread().interrupt();
            mainPages.clear();
            globalLinksStorage.clear();
            isRunning.set(false);
        }

        if (isRootTask) {
            mainPages.put(siteRepository.findByMainPageURL(this.url).get().getId(), this.url);
        }
        Document htmlContent = getHTMLContent(url);
        PageEntity page = new PageEntity();
        if (!isRootTask) {
            page.setPagePath(url.replace(mainPages.get(this.id), ""));
            page.setPageContentHttpCode(htmlContent.html());
            page.setPageResponseHttpCode(responseCode);
            page.setSiteId(siteRepository.findById(this.id).get());
            pageRepository.save(page);

            if (!responseErrorsCodesSet.contains(responseCode)) {
                try {
                    saveLemmasToTable(htmlContent, url.replace(mainPages.get(this.id), ""));
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        List <PageIndexing> subTasksList = createSubtasks(getLinksFromPage(htmlContent));
        for (PageIndexing task : subTasksList) {
            task.fork();
        }
        for (PageIndexing task : subTasksList) {
            task.join();
        }
        if (isRootTask) {
            SiteEntity site = siteRepository.findByMainPageURL(url).get();
            if (site.getStatus().equals(Status.FAILED)) return;
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);

            List <SiteEntity> sitesList = siteRepository.findAll();
            int j = 0;
            for (SiteEntity siteEntity : sitesList) {
                if (!siteEntity.getStatus().equals(Status.INDEXING)) j++;
            }
            if (j == sitesList.size()) {
                isRunning.set(false);
                mainPages.clear();
                globalLinksStorage.clear();
            }
        }
    }

    private Document getHTMLContent (String url) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Document doc = new Document(url);

        try {
            doc = Jsoup.connect(url).get();
            responseCode = doc.connection().response().statusCode();
        }
        catch (HttpStatusException e) {
            System.out.println(e.getMessage());
            responseCode = e.getStatusCode();
        }
        catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
            if (isRootTask) {
                SiteEntity site = siteRepository.findByMainPageURL(url).get();
                site.setStatus(Status.FAILED);
                site.setLastError("Не удалось проиндексировать сайт: " + ex);
                siteRepository.save(site);
            }
        }

        return doc;
    }

    private HashSet<String> getLinksFromPage(Document doc) {
        HashSet<String> links = new HashSet<>();
        String regexURL = mainPages.get(this.id);
        String regexURLEndsWithSlash = regexURL + "[\\/]*";
        String regexURLEndsWithAnySymbol = regexURL + "/\\S*";
        Elements elements = doc.select("a[href]");

        for (Element e : elements) {
            String buff = regexURL.concat(e.attr("href"));
            if (LinkFilter(e.attr("href"))) {
                if (e.attr("href").contains(regexURL) && !(e.attr("href").matches(regexURLEndsWithSlash))
                        && !e.attr("href").contains("#")) {
                    if (globalLinksStorage.add(e.attr("href"))) links.add(e.attr("href"));
                } else if (buff.matches(regexURLEndsWithAnySymbol) && !(buff.matches(regexURLEndsWithSlash))
                        && !(e.attr("href").contains("#"))) {
                    if (globalLinksStorage.add(buff)) links.add(buff);
                }
            }
        }

        return links;
    }

    private List<PageIndexing> createSubtasks (HashSet<String> links) {
        List<PageIndexing> subTasks = new ArrayList<>();
            if (!links.isEmpty()) {
                links
                .forEach(link ->
                    subTasks
                    .add(new PageIndexing(this.id, link, pageRepository, siteRepository, lemmaRepository, indexRepository, false)));
            }
        return subTasks;
    }

    private void saveLemmasToTable (Document htmlCode, String pagePath) throws SQLException {
        HashMap<String, Integer> lemmasMap = new HashMap<>();
        LemmaFinder lemmaFinder = new LemmaFinder();
        PageEntity pageEntity = pageRepository.findByPagePath(pagePath).get();
        SiteEntity siteEntity = siteRepository.findById(this.id).get();

        try {
            lemmasMap = lemmaFinder.getLemmas(htmlCode.body().text());
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        for (String lemma : lemmasMap.keySet()) {
            LemmaEntity lemmaEntity = new LemmaEntity();
            List <LemmaEntity> lemmasFromTableList;

            synchronized (lemmaRepository) {
                lemmasFromTableList = lemmaRepository.findAllByLemma(lemma);
                if (!lemmasFromTableList.isEmpty()) {
                    List<LemmaEntity> lemmaFromTableWithThisSiteId= lemmasFromTableList.stream()
                    .filter(o -> o.getSiteId().getId() == this.id).toList();
                    if (!lemmaFromTableWithThisSiteId.isEmpty()) {
                        LemmaEntity lemmaEntityTransitional = lemmaFromTableWithThisSiteId.get(0);
                        int frequency = lemmaEntityTransitional.getFrequency() + 1;
                        lemmaEntityTransitional.setFrequency(frequency);
                        lemmaRepository.saveAndFlush(lemmaEntityTransitional);
                        saveIndexEntityToTable(lemmaEntityTransitional, pageEntity, lemmasMap.get(lemma));
                    }
                    else {
                        lemmaEntity.setLemma(lemma);
                        lemmaEntity.setFrequency(1);
                        try {
                            lemmaEntity.setSiteId(siteEntity);
                        } catch (PersistentObjectException | InvalidDataAccessApiUsageException ex) {
                            System.out.println(ex.getMessage());
                        }
                        lemmaRepository.saveAndFlush(lemmaEntity);
                        saveIndexEntityToTable(lemmaEntity, pageEntity, lemmasMap.get(lemma));
                    }
                }
                else {
                    lemmaEntity.setLemma(lemma);
                    lemmaEntity.setFrequency(1);
                    try {
                        lemmaEntity.setSiteId(siteEntity);
                    } catch (PersistentObjectException | InvalidDataAccessApiUsageException ex) {
                        System.out.println(ex.getMessage());
                    }
                    lemmaRepository.saveAndFlush(lemmaEntity);
                    saveIndexEntityToTable(lemmaEntity, pageEntity, lemmasMap.get(lemma));
                }
            }
        }
    }

    private void saveIndexEntityToTable(LemmaEntity lemmaEntity, PageEntity pageEntity, int rank) {
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

    private boolean LinkFilter(String link) {
        Set<String> unwantedLinksSet = new HashSet<>(Arrays.asList(".*[.]pdf.*", ".*[.]jpeg.*", ".*[.]jpg.*", ".*[.]tiff.*", ".*[.]png.*"));
        for (String s : unwantedLinksSet) {
            if (link.matches(s)) return false;
        }

        return true;
    }
}
