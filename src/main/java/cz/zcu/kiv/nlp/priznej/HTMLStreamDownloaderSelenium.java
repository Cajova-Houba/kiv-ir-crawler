package cz.zcu.kiv.nlp.priznej;

import cz.zcu.kiv.nlp.ir.HTMLDownloaderSelenium;
import org.openqa.selenium.JavascriptExecutor;

import java.util.List;

/**
 * Class based on selenium downlader which allows scrolling down through stream of posts so that all of them are loaded.
 */
public class HTMLStreamDownloaderSelenium extends HTMLDownloaderSelenium {

    /**
     * Number of pages to scroll through.
     */
    private int scrollPages;

    public HTMLStreamDownloaderSelenium(int scrollPages) {
        super();
        this.scrollPages = scrollPages;
    }

    /**
     * Same as HTMLDownloaderSelenium.getLink() but scrolls before applying the xPath expression.
     * @param url   page url
     * @param xPath xpath expression
     * @return
     */
    @Override
    public List<String> getLinks(String url, String xPath) {
        // open url
        openUrl(url);

        // scroll
        for (int i = 0; i < scrollPages; i++) {
            JavascriptExecutor jse = (JavascriptExecutor)driver;
            jse.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // get links
        return getLinksInternal(url, xPath);
    }
}
