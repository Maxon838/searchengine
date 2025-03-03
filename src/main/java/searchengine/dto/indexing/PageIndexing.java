package searchengine.dto.indexing;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;

import static searchengine.services.IndexingServiceImpl.stopFlag;

public class PageIndexing extends RecursiveAction {

    private final String url;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final boolean isRootTask;
    private int responseCode = 0;

    private static HashSet<String> allLinks = new HashSet<>();

    public PageIndexing (String url, PageRepository pageRepository, SiteRepository siteRepository, boolean isRootTask)
    {
        this.url = url;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.isRootTask = isRootTask;
    }

    @Override
    protected void compute() {

        if (stopFlag.get()) return;

        Document htmlContent = getHTMLContent(url);

        PageEntity page = new PageEntity();
        if (!isRootTask) {
            page.setPagePath(url);
            page.setPageContentHttpCode(htmlContent.html());
            page.setPageResponseHttpCode(responseCode);
            page.setSiteId(siteRepository.getById(1));
            pageRepository.save(page);
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
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);

        }
    }

    private Document getHTMLContent (String url) {

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Document doc = new Document(url);

        try {
            doc = Jsoup.connect(url).get();
            responseCode = doc.connection().response().statusCode();
        }
        catch (HttpStatusException e)
        {
            System.out.println(e.getMessage());
            responseCode = e.getStatusCode();
        }
        catch (IOException | NullPointerException ex) {

            ex.printStackTrace();
        }

        return doc;
    }

    private HashSet<String> getLinksFromPage(Document doc) {

        HashSet<String> links = new HashSet<>();

        String regexURL = siteRepository.findById(1).get().getMainPageURL();
        String regexURLEndsWithSlash = regexURL + "[\\/]*";
        String regexURLEndsWithAnySymbol = regexURL + "/\\S*";

        Elements elements = doc.select("a[href]");

        for (Element e : elements) {

            String buff = regexURL.concat(e.attr("href"));

            if (e.attr("href").contains(regexURL) && !(e.attr("href").matches(regexURLEndsWithSlash))
                    && !e.attr("href").contains("#")) {

                /*if (allLinks.add(e.attr("href")))*/ links.add(e.attr("href"));
            }

            else if (buff.matches(regexURLEndsWithAnySymbol) && !(buff.matches(regexURLEndsWithSlash)) && !(e.attr("href").contains("#"))) {
                /*if (allLinks.add(buff))*/ links.add(buff);
            }
        }

        return links;
    }

    private List<PageIndexing> createSubtasks (HashSet<String> links) {

        List<PageIndexing> subTasks = new ArrayList<>();

            if (!links.isEmpty()) {
                links
                        .forEach(link ->
                        {
                            if (!pageRepository.findByPagePath(link).isPresent())
                                subTasks.add(new PageIndexing(link, pageRepository, siteRepository, false));
                        });
            }

        return subTasks;
    }

}
