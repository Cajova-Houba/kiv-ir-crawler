package cz.zcu.kiv.nlp.reddit;

import cz.zcu.kiv.nlp.ir.AbstractHTMLDownloader;
import cz.zcu.kiv.nlp.ir.HTMLDownloader;
import cz.zcu.kiv.nlp.ir.HTMLDownloaderSelenium;
import cz.zcu.kiv.nlp.ir.Utils;
import cz.zcu.kiv.nlp.vs.CrawlerVSCOM;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Crawler for /r/politics.
 */
public class CrawlerRPolitics {

    /**
     * Xpath expressions to extract and their descriptions.
     */
    private final static Map<String, String> xpathMap = new HashMap<String, String>();

    public static final String POST_TEXT_KEY = "postText";
    public static final String SCORE_KEY = "score";
    public static final String USERNAME_KEY = "username";
    public static final String TIMESTAMP_KEY = "timestamp";

    public static final int MAX_URL_COUNT = 150;

    static {
        xpathMap.put(POST_TEXT_KEY, "//div[@class=\"commentarea\"]//div[@class=\"usertext-body may-blank-within md-container \"]/div/allText()");
        xpathMap.put(SCORE_KEY, "//span[@class=\"score unvoted\"]/@title");
        xpathMap.put(TIMESTAMP_KEY, "//div[@class=\"entry unvoted\"]/p/time[1]/@title");
        xpathMap.put(USERNAME_KEY, "//div[@class=\"commentarea\"]//div[@class=\"entry unvoted\"]/p/a[2]/allText()");
    }

    private static final String STORAGE = "./storage/rpolitics";
    private static String SITE = "https://old.reddit.com";
    private static String SITE_SUFFIX = "/r/politics";

    /**
     * Be polite and don't send requests too often.
     * Waiting period between requests. (in milisec)
     */
    private static final int POLITENESS_INTERVAL = 1000;
    private static final Logger log = Logger.getLogger(CrawlerVSCOM.class);

    public static void main(String[] args) {
        // initialization
        initialize();
        Map<String, Map<String, List<String>>> results = prepareResultMap();

        // download urls to crawl
        AbstractHTMLDownloader downloader = new HTMLDownloader();
        Collection<String> urls = getUrls(downloader);

        // crawl
        downloadAndProcessData(results, urls, downloader);
        downloader.quit();

        Collection<Comment> comments = transformResults(results);


        Date timestamp = new Date();
        saveComments(comments, timestamp);
        saveCommentsTextOnly(comments, timestamp);
    }

    private static void saveCommentsTextOnly(Collection<Comment> comments, Date timestamp) {
        List<String> commentstoString = comments.stream().map(c -> c.getText()+"\n;\n").collect(Collectors.toList());;
        Utils.saveFile(new File(STORAGE+"/comments_" + commentstoString.size() + "_textonly_"+timestamp.getTime()+".txt"),
                commentstoString);
    }

    private static void saveComments(Collection<Comment> comments, Date timestamp) {
        List<String> commentstoString = comments.stream().map(Comment::toString).collect(Collectors.toList());;
        Utils.saveFile(new File(STORAGE+"/comments_" + commentstoString.size() + "_"+timestamp.getTime()+".txt"),
                commentstoString);
    }

    /**
     * Transforms result data into Comment objects.
     *
     * To access actual value of the first comment from the first post:
     *  result.get(postText).get(url1)[0]
     *
     * @param results
     * @return
     */
    private static Collection<Comment> transformResults(Map<String, Map<String, List<String>>> results) {

        // post url -> comments
        Map<String, List<Comment>> commentsPerPosts = new HashMap<>();

        // score may be hidden but post test cannot be

        for(String key : results.keySet()) {
            Map<String, List<String>> subres = results.get(key);

            for (String postUrl : subres.keySet()) {
                int size = subres.get(postUrl).size();

                if (size == 0) {
                    continue;
                }

                if (!commentsPerPosts.containsKey(postUrl)) {
                    commentsPerPosts.put(postUrl, initNewCommentList(size));
                }

                for (int i = 0; i < size; i++) {
                    String value = subres.get(postUrl).get(i);

                    if (i >= commentsPerPosts.get(postUrl).size()) {
                        break;
                    }

                    switch (key) {
                        case SCORE_KEY:
                            commentsPerPosts.get(postUrl).get(i).setScore(Integer.parseInt(value));
                            break;
                        case USERNAME_KEY:
                            commentsPerPosts.get(postUrl).get(i).setUsername(value);
                            break;
                        case POST_TEXT_KEY:
                            commentsPerPosts.get(postUrl).get(i).setText(value);
                            break;
                        case TIMESTAMP_KEY:
                            commentsPerPosts.get(postUrl).get(i).setTimestamp(value);
                            break;
                    }
                }
            }
        }

        // merge it together
        List<Comment> comments = new ArrayList<>();
        for(String postUrl : commentsPerPosts.keySet()) {
            comments.addAll(commentsPerPosts.get(postUrl));
        }

        return comments;
    }

    private static List<Comment> initNewCommentList(int size) {
        List<Comment> emptyComments = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            emptyComments.add(new Comment());
        }
        return emptyComments;
    }

    /**
     * Downloads and parses the data.
     *
     * @param results Target map.
     * @param urls
     * @param downloader
     */
    private static void downloadAndProcessData(Map<String, Map<String, List<String>>> results, Collection<String> urls, AbstractHTMLDownloader downloader) {
        log.info("Processing "+urls.size()+" urls.");

        int count = 0;

        // create dir for saving results
        String timestamp = Long.toString(new Date().getTime());
        File outputDir = new File(STORAGE+"/html/"+timestamp);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (String url : urls) {
            String link = url+ "?limit=1000";
            if (!link.contains(SITE)) {
                link = SITE + url + "?limit=1000";
            }

            String postId = link.substring(43, 43+6);;

            //Download and extract data according to xpathMap
            Map<String, List<String>> products = downloader.processUrl(link, xpathMap, STORAGE+"/html/"+timestamp+"/"+postId+".html");
            count++;
            if (count % 10 == 0) {
                log.info(count + " / " + urls.size() + " = " + count / (0.0 + urls.size()) + "% done.");
            }
            for (String key : results.keySet()) {
                Map<String, List<String>> map = results.get(key);
                List<String> list = products.get(key);
                if (list != null) {
                    map.put(url, list);
//                    log.info(Arrays.toString(list.toArray()));
                }
            }
            try {
                Thread.sleep(POLITENESS_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initializes structure for storing results from xpathMap.
     *
     * data structure: xpathKey -> products
     *
     * products: url -> results
     *
     * So to access actual value of the first comment from the first post:
     * result.get(postText).get(url1)[0]
     *
     * @return
     */
    private static Map<String, Map<String, List<String>>> prepareResultMap() {
        Map<String, Map<String, List<String>>> results = new HashMap<String, Map<String, List<String>>>();

        for (String key : xpathMap.keySet()) {
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            results.put(key, map);
        }

        return results;
    }

    /**
     * Either gets links from file (if it exists) or performs initial crawl to get urls be crawled.
     * @return
     * @param downloader
     */
    private static Collection<String> getUrls(AbstractHTMLDownloader downloader) {
        Collection<String> urlsSet = new HashSet<>();
        String fileName = STORAGE+"/urls.txt";
        File links = new File(fileName);
        if (links.exists()) {
            log.info("Getting urls from file "+ fileName);
            try {
                List<String> lines = Utils.readTXTFile(new FileInputStream(links));
                for (String line : lines) {
                    urlsSet.add(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {

            int max = MAX_URL_COUNT;

            getUrlsFromReddit(urlsSet, max, downloader);

            // save urls
            Utils.saveFile(new File(STORAGE + Utils.SDF.format(System.currentTimeMillis()) + "_links_size_" + urlsSet.size() + ".txt"),
                    urlsSet);
        }

        return urlsSet;
    }

    /**
     * Returns the urls of the first [count] posts of /r/politics subreddit.
     *
     * Reddit's result page is 25 items.
     *
     * @param urlsSet Target collection.
     * @param count How many urls to fetch, expected at least 25.
     */
    private static void getUrlsFromReddit(Collection<String> urlsSet, int count, AbstractHTMLDownloader downloader) {
        log.info("Getting urls of first "+count + " posts from "+SITE_SUFFIX);
        final String link = SITE + SITE_SUFFIX;
        final int pageSize = 25;
        int urlsFetched = 0;

        if (count < pageSize) {

            // because fuck you.
            log.warn("Count "+count+" is too low, expected at least "+pageSize);
            return;
        }

        // initial fetch
        List<String> urls = downloader.getLinks(link, "//div[@id=\"siteTable\"]//a[@class=\"bylink comments may-blank\"]/@href");
        String lastPostId = urls.get(urls.size()-1).substring(43, 43+6);
        urlsSet.addAll(urls);

        urlsFetched += pageSize;

        // fetch the rest
        while(urlsFetched < count) {

            try {
                log.info("Politely waiting "+POLITENESS_INTERVAL+"ms");
                Thread.sleep(POLITENESS_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String range = String.format("?count=%d&after=t3_%s", urlsFetched, lastPostId);

            log.info("Fetching "+urlsFetched+" urls after post "+lastPostId);

            urls = downloader.getLinks(link+range, "//div[@id=\"siteTable\"]//a[@class=\"bylink comments may-blank\"]/@href");
            lastPostId = urls.get(urls.size()-1).substring(43, 43+6);
            urlsSet.addAll(urls);

            urlsFetched += pageSize;
        }

        log.info("Urls fetched: "+urlsSet.size());
        urlsSet.forEach(url -> log.info("Url: "+url));
    }

    private static void initialize() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        File outputDir = new File(STORAGE);
        if (!outputDir.exists()) {
            boolean mkdirs = outputDir.mkdirs();
            if (mkdirs) {
                log.info("Output directory created: " + outputDir);
            } else {
                log.error("Output directory can't be created! Please either create it or change the STORAGE parameter.\nOutput directory: " + outputDir);
            }
        }

        File htmlDir = new File(STORAGE+"/html");
        if (!htmlDir.exists()) {
            boolean mkdirs = htmlDir.mkdirs();
            if (mkdirs) {
                log.info("HTML directory created: " + htmlDir);
            } else {
                log.error("HTML directory can't be created! Please either create it or change the STORAGE parameter.\nOutput directory: " + htmlDir);
            }
        }
    }
}
