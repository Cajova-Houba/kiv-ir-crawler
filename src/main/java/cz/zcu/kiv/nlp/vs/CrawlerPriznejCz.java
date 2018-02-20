package cz.zcu.kiv.nlp.vs;

import cz.zcu.kiv.nlp.ir.AbstractHTMLDownloader;
import cz.zcu.kiv.nlp.ir.HTMLDownloaderSelenium;
import cz.zcu.kiv.nlp.ir.Utils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A web crawler which will download posts from page priznej.cz.
 */
public class CrawlerPriznejCz {

    /**
     * Xpath expressions to extract and their descriptions.
     */
    private final static Map<String, String> xpathMap = new HashMap<String, String>();

    public static final String POST_ID_XPATH = "//div[@id=\"stream\"]//div/@id";

    /**
     * XPaths to be applied to a single post.
     */
    static {
        xpathMap.put("postBody", "//div[@class='post-body']//td");
//        xpathMap.put("allText", "//div[@class='oborList']/allText()");
//        xpathMap.put("html", "//div[@class='oborList']/html()");
//        xpathMap.put("tidyText", "//div[@class='oborList']/tidyText()");
    }

    private static final String STORAGE = "./storage/PriznejCz";
    private static String SITE = "http://www.priznej.cz";
    private static String SITE_SUFFIX = "/new/";

    /**
     * Post url is SITE+POST_SUFFIX+postId
     */
    private static String POST_SUFFIX = "/confession/";


    /**
     * Be polite and don't send requests too often.
     * Waiting period between requests. (in milisec)
     */
    private static final int POLITENESS_INTERVAL = 1200;
    private static final Logger log = Logger.getLogger(CrawlerVSCOM.class);

    /**
     * Main method
     */
    public static void main(String[] args) {
        //Initialization
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
//        HTMLDownloader downloader = new HTMLDownloader();
        AbstractHTMLDownloader downloader = new HTMLDownloaderSelenium();
        Map<String, Map<String, List<String>>> results = new HashMap<String, Map<String, List<String>>>();

        for (String key : xpathMap.keySet()) {
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            results.put(key, map);
        }

        // hash set which contains urls to posts
        Collection<String> urlsSet = new HashSet<>();
        Map<String, PrintStream> printStreamMap = new HashMap<String, PrintStream>();

        // load posts form 1 page
        List<String> postDivIds = downloader.getLinks(SITE+SITE_SUFFIX, POST_ID_XPATH);
        urlsSet.addAll(getPostLinks(postDivIds));
        Utils.saveFile(new File(STORAGE + Utils.SDF.format(System.currentTimeMillis()) + "_links_size_" + urlsSet.size() + ".txt"),
                    urlsSet);

//        Map<String, List<String>> products = downloader.processUrl(SITE+SITE_SUFFIX, xpathMap);
//        for(String key : products.keySet()){
//            for(String s : products.get(key)) {
//                log.info(s);
//            }
//        }

        //Try to load links
//        File links = new File(STORAGE + "_urls.txt");
//        if (links.exists()) {
//            try {
//                List<String> lines = Utils.readTXTFile(new FileInputStream(links));
//                for (String line : lines) {
//                    urlsSet.add(line);
//                }
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        } else {
//
//            int max = 200;
//            max=6600;
//            for (int i = 0; i < max; i = i + 100) {
//                String link = SITE + SITE_SUFFIX + "?pgf0=" + i;
//                urlsSet.addAll(downloader.getLinks(link, "//div[@id='skoolList']//h3/a/@href"));
//            }
//            Utils.saveFile(new File(STORAGE + Utils.SDF.format(System.currentTimeMillis()) + "_links_size_" + urlsSet.size() + ".txt"),
//                    urlsSet);
//        }
//
//        for (String key : results.keySet()) {
//            File file = new File(STORAGE + "/" + Utils.SDF.format(System.currentTimeMillis()) + "_" + key + ".txt");
//            PrintStream printStream = null;
//            try {
//                printStream = new PrintStream(new FileOutputStream(file));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            printStreamMap.put(key, printStream);
//        }
//
//        int count = 0;
//        for (String url : urlsSet) {
//            String link = url;
//            if (!link.contains(SITE)) {
//                link = SITE + url;
//            }
//            //Download and extract data according to xpathMap
//            Map<String, List<String>> products = downloader.processUrl(link, xpathMap);
//            count++;
//            if (count % 100 == 0) {
//                log.info(count + " / " + urlsSet.size() + " = " + count / (0.0 + urlsSet.size()) + "% done.");
//            }
//            for (String key : results.keySet()) {
//                Map<String, List<String>> map = results.get(key);
//                List<String> list = products.get(key);
//                if (list != null) {
//                    map.put(url, list);
//                    log.info(Arrays.toString(list.toArray()));
//                    //print
//                    PrintStream printStream = printStreamMap.get(key);
//                    for (String result : list) {
//                        printStream.println(url + "\t" + result);
//                    }
//                }
//            }
//            try {
//                Thread.sleep(POLITENESS_INTERVAL);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        //close print streams
//        for (String key : results.keySet()) {
//            PrintStream printStream = printStreamMap.get(key);
//            printStream.close();
//        }

        // Save links that failed in some way.
        // Be sure to go through these and explain why the process failed on these links.
        // Try to eliminate all failed links - they consume your time while crawling data.
        reportProblems(downloader.getFailedLinks());
        downloader.emptyFailedLinks();
        log.info("-----------------------------");


//        // Print some information.
//        for (String key : results.keySet()) {
//            Map<String, List<String>> map = results.get(key);
//            Utils.saveFile(new File(STORAGE + "/" + Utils.SDF.format(System.currentTimeMillis()) + "_" + key + "_final.txt"),
//                    map, idMap);
//            log.info(key + ": " + map.size());
//        }
        System.exit(0);
    }

    /**
     * Extracts post id from every string in format 'post-xxxxxx' where xxxxxx is a combination of letters and numbers.
     *
     * @param strings List of strings.
     * @return Post links in format POST_SUFFIX+postId.
     */
    private static List<String> getPostLinks(List<String> strings) {
        List<String> postLinks = new ArrayList<>();
        String regex = "post-([a-zA-Z0-9]{6})";
        Pattern pattern = Pattern.compile(regex);

        for(String string : strings) {
            Matcher matcher = pattern.matcher(string);

            if(matcher.find()) {
                String postId = matcher.group(1);
                if(postId != null && !postId.isEmpty()) {
                    postLinks.add(POST_SUFFIX+postId);
                }
            }

        }

        return postLinks;
    }

    /**
     * Save file with failed links for later examination.
     *
     * @param failedLinks links that couldn't be downloaded, extracted etc.
     */
    private static void reportProblems(Set<String> failedLinks) {
        if (!failedLinks.isEmpty()) {

            Utils.saveFile(new File(STORAGE + Utils.SDF.format(System.currentTimeMillis()) + "_undownloaded_links_size_" + failedLinks.size() + ".txt"),
                    failedLinks);
            log.info("Failed links: " + failedLinks.size());
        }
    }

}
