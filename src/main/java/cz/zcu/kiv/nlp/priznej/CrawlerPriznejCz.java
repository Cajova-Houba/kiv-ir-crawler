package cz.zcu.kiv.nlp.priznej;

import cz.zcu.kiv.nlp.ir.AbstractHTMLDownloader;
import cz.zcu.kiv.nlp.ir.Utils;
import cz.zcu.kiv.nlp.vs.CrawlerVSCOM;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
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
        xpathMap.put("postBody", "//div[@class='post-body']//td/tidyText()");
        xpathMap.put("upvotes", "//table[@class=\"post-stats\"]//tr[1]/td/tidyText()");
        xpathMap.put("downvotes", "//table[@class=\"post-stats\"]//tr[2]/td/tidyText()");
        xpathMap.put("commentCount", "//div[@class=\"post-body\"]//span/tidyText()");
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
    private static final int POLITENESS_INTERVAL = 800;
    private static final Logger log = Logger.getLogger(CrawlerVSCOM.class);

    /**
     * How many pages will be scrolled before post links are obtained.
     */
    public static final int scrolling = 40;

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
        AbstractHTMLDownloader downloader = new HTMLStreamDownloaderSelenium(scrolling);
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

        // load posts from links
        log.info(urlsSet.size()+" links to posts found.");
        List<Confession> confessions = new ArrayList<>();
        for(String postLink : urlsSet) {
            String url = SITE + postLink;
            log.info("Processing url: "+url);

            Map<String, List<String>> res = downloader.processUrl(url, xpathMap);
            Confession c = new Confession();
            c.setId(postLink.substring(postLink.length()-6,postLink.length()));
            for(String key : res.keySet()) {
                List<String> data = res.get(key);
                if(!data.isEmpty()) {
                    String item = data.get(0);
                    switch (key) {
                        case "postBody" :
                            c.setText(item);
                            break;
                        case "upvotes":
                            c.setUpvotes(Integer.parseInt(item));
                            break;
                        case "downvotes":
                            c.setDownvotes(Integer.parseInt(item));
                            break;
                        case "commentCount":
                            c.setCommentCount(Integer.parseInt(item));
                            break;
                    }
                }
            }
            confessions.add(c);

            try {
                Thread.sleep(POLITENESS_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // print downloaded confessions
        log.info(confessions.size()+" confessions found.");
//        for(Confession c : confessions) {
//            log.info(c);
//        }
        FileOutputStream fout = null;
        String fname= STORAGE+"\\"+ Utils.SDF.format(System.currentTimeMillis()) + "_" + confessions.size() + ".txt";
        try {
            fout = new FileOutputStream(fname);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(confessions);
            oos.close();
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // load serialized confessions and test print them
        try {
            FileInputStream fin = new FileInputStream(fname);
            ObjectInputStream oin = new ObjectInputStream(fin);
            List<Confession> cs = (List<Confession>) oin.readObject();
            oin.close();
            fin.close();

            log.info(cs.size()+" confessions deserialized.");
            for(Confession c : cs) {
                log.info(c);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        // Save links that failed in some way.
        // Be sure to go through these and explain why the process failed on these links.
        // Try to eliminate all failed links - they consume your time while crawling data.
        reportProblems(downloader.getFailedLinks());
        downloader.emptyFailedLinks();
        log.info("-----------------------------");


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
