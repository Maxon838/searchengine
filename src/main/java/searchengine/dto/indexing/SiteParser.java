package searchengine.dto.indexing;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.RecursiveAction;

public class SiteParser extends RecursiveAction {

    private volatile static HashSet<String> links;
    private static HashSet<PageDTO> pageDTOSet;
    private static String url;
    private static int siteId;
    private String workUrl;
    private PageDTO pageDTO;

    public SiteParser (String url, int siteId) {

        this.url = url;
        this.workUrl = url;
        SiteParser.siteId = siteId;
        SiteParser.links = new HashSet<>();
        SiteParser.pageDTOSet = new HashSet<>();

        System.out.println("Hello "  + url);
    }

    public SiteParser (String url) {
        this.workUrl = url;
        this.pageDTO = new PageDTO();
    }

    public static HashSet<String> getLinks() {
        return links;
    }
    public static HashSet<PageDTO> getPageDTOSet() {return pageDTOSet;}
    public static void resetLinksList() { links.clear(); }
    public static void resetPageDTOSet() { pageDTOSet.clear(); }

    @Override
    public void compute() {

        HashSet<String> levelUrlSet = new HashSet<>();
        ArrayList<SiteParser> objectsList = new ArrayList<>();
        int responseCode = 0;

        String regexURL = url;
        String regexURLRoot = url + "[\\/]*";
        String regexCut = url + "/\\S*";


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Document doc = new Document(workUrl);

        try {

            doc = Jsoup.connect(this.workUrl).get();
            if (!(this.workUrl == url))
                {
                    responseCode = doc.connection().response().statusCode();
                    pageDTO.setSiteId(siteId);
                    pageDTO.setPagePath(this.workUrl.replace(url, ""));
                    pageDTO.setPageContentHttpCode(doc.html());
                    pageDTO.setPageResponseHttpCode(responseCode);
                    pageDTOSet.add(this.pageDTO);
                }
        }
        catch (HttpStatusException e)
        {
            pageDTO.setSiteId(siteId);
            pageDTO.setPagePath(this.workUrl.replace(url, ""));
            pageDTO.setPageContentHttpCode(e.getMessage());
            pageDTO.setPageResponseHttpCode(e.getStatusCode());
            pageDTOSet.add(this.pageDTO);
            System.out.println(e.getMessage());
        }
        catch (IOException | NullPointerException ex) {
            pageDTO.setSiteId(siteId);
            pageDTO.setPagePath(this.workUrl.replace(url, ""));
            pageDTO.setPageContentHttpCode(doc.html());
            pageDTO.setPageResponseHttpCode(responseCode);
            pageDTOSet.add(this.pageDTO);

            ex.printStackTrace();
        }

        Elements elements = doc.select("a[href]");
        for (Element e : elements) {

            String buff = regexURL.concat(e.attr("href"));
            if (e.attr("href").contains(regexURL) && !(e.attr("href").matches(regexURLRoot))
                    && !e.attr("href").contains("#")) {

                if (links.add(e.attr("href"))) {
                    levelUrlSet.add(e.attr("href"));
                }
            }
            else if (buff.matches(regexCut) && !(buff.matches(regexURLRoot)) && !e.attr("href").contains("#")) {

                if (links.add(buff)) {
                    String childURL = buff;
                    levelUrlSet.add(childURL);
                }
            }

        }

        if (!levelUrlSet.isEmpty()) {

            levelUrlSet.stream().forEach(element -> {
                objectsList.add(new SiteParser(element));
            });
        }

        invokeAll(objectsList);

    }

}
