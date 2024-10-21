# crawler4j

![Maven Central](https://img.shields.io/maven-central/v/de.hs-heilbronn.mi/crawler4j-parent.svg?style=flat-square)

This repository contains a fork of [yasserg/crawler4j](https://github.com/yasserg/crawler4j).

**⚠️ Disclaimer: This repository is not actively maintained. Feel free to provide PR/bug fixes if it suits your needs. We can also do relases if there is a community need for it.**.

---

crawler4j is an open source web crawler for Java which provides a simple interface for
crawling the Web. Using it, you can setup a multi-threaded web crawler in few minutes.

## Table of content

- [Why you should use this fork?](#why-you-should-use-this-fork)
- [Installation](#installation)
- [Quickstart](#quickstart)   
- [More Examples](#more-examples)
- [Configuration Details](#configuration-details)
- [Reconstructing extra urls to crawl](#reconstructing-extra-urls-to-crawl)
- [Authentication](#authentication)
- [High-level design diagrams](#high-level-design-diagrams)
- [License](#license)

## Why you should use this fork?

This fork starts where the development of the previous main repository stalled.

Some highlights include:

- choice between multiple frontier implementations => avoid using a database with a license that doesn't comply with your use-case
- easy substitution of various parser implementations (not only for html, but also css, binary, and plain text)
- dynamic authentication
- improved exception handling, more versatile to customize
- fixes various parsing issues
- more documentation
- more tests and all tests are JUnit5 based (so no knowledge of Groovy and/or Spock needed anymore to maintain the codebase)
- uses Apache Maven as build tool
- provides a clean upgrade path by keeping backward compatibility in mind and deprecating methods before removing them
- more eyes have gone through the code, so readability and correctness have improved
- maintained, i.e. dependencies are often updated to their latest versions

## Installation

### Using Maven

Add the following dependency to your pom.xml:

```xml
        <dependency>
            <groupId>de.hs-heilbronn.mi</groupId>
            <artifactId>crawler4j-with-sleepycat</artifactId>
            <version>5.0.2</version>
        </dependency>
```

**Please check**, if the Oracle license for Sleepycat database complies with your use-case.

Otherwise, you can use `HSQLDB` instead

```xml
        <dependency>
            <groupId>de.hs-heilbronn.mi</groupId>
            <artifactId>crawler4j-with-hsqldb</artifactId>
            <version>5.0.2</version>
        </dependency>
```

or you use an external [crawler-commons/url-frontier](https://github.com/crawler-commons/url-frontier)

```xml
        <dependency>
            <groupId>de.hs-heilbronn.mi</groupId>
            <artifactId>crawler4j-with-urlfrontier</artifactId>
            <version>5.0.2</version>
        </dependency>
```

## Quickstart

### Archetype

Since `5.0.1`, we provide a Maven archetype to bootstrap crawler4j development. Just urn

```bash
mvn archetype:generate -DarchetypeGroupId=de.hs-heilbronn.mi -DarchetypeArtifactId=crawler4j-archetype -DarchetypeVersion=5.0.1                
```

### Manual

You need to create a crawler class that extends WebCrawler. This class decides which URLs
should be crawled and handles the downloaded page. The following is a sample
implementation:

```java
public class MyCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
                                                           + "|png|mp3|mp4|zip|gz))$");

    /**
     * This method receives two parameters. The first parameter is the page
     * in which we have discovered this new url and the second parameter is
     * the new url. You should implement this function to specify whether
     * the given url should be crawled or not (based on your crawling logic).
     * In this example, we are instructing the crawler to ignore urls that
     * have css, js, git, ... extensions and to only accept urls that start
     * with "https://www.ics.uci.edu/". In this case, we didn't need the
     * referringPage parameter to make the decision.
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        return !FILTERS.matcher(href).matches()
               && href.startsWith("https://www.ics.uci.edu/");
    }

    /**
     * This function is called when a page is fetched and ready
      * to be processed by your program.
     */
    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        System.out.println("URL: " + url);

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            Set<WebURL> links = htmlParseData.getOutgoingUrls();

            System.out.println("Text length: " + text.length());
            System.out.println("Html length: " + html.length());
            System.out.println("Number of outgoing links: " + links.size());
        }
    }
    
    /**
     * Determine whether links found at the given URL should be added to the queue for crawling.
     */
    @Override
    protected boolean shouldFollowLinksIn(WebURL url) {
        return super.shouldFollowLinksIn(url);
    }
    
}
```

As can be seen in the above code, there are two main functions that should be overridden:

- shouldVisit: This function decides whether the given URL should be crawled or not. In
the above example, this example is not allowing .css, .js and media files and only allows
 pages within 'www.ics.uci.edu' domain.
- visit: This function is called after the content of a URL is downloaded successfully.
 You can easily get the url, text, links, html, and unique id of the downloaded page.
- (extra) shouldFollowLinksIn: can also be overridden as needed. false means none of the outgoing links are scheduled to be crawled.

The flow is as follows:  
fetch url -> parse => outgoing links are detected -> "shouldFollowLinksIn": decide for all outgoing urls -> "shouldVisit": a more fine grained (per url) decision


You should also implement a controller class which specifies the seeds of the crawl,
the folder in which intermediate crawl data should be stored and the number of concurrent
 threads:

```java
public class Controller {
    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "/data/crawl/root";
        int numberOfCrawlers = 7;

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        
        // Instantiate the controller for this crawl 
        BasicURLNormalizer normalizer = BasicURLNormalizer.newBuilder().idnNormalization(BasicURLNormalizer.IdnNormalization.NONE).build();
        PageFetcher pageFetcher = new PageFetcher(config, normalizer);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        FrontierConfiguration frontierConfiguration = new SleepycatFrontierConfiguration(config);
        // OR use
        // FrontierConfiguration frontierConfiguration = new HSQLDBFrontierConfiguration(config, 10);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher, frontierConfiguration.getWebURLFactory());
        CrawlController controller = new CrawlController(config, normalizer, pageFetcher, robotstxtServer, frontierConfiguration);

        // For each crawl, you need to add some seed urls. These are the first
        // URLs that are fetched and then the crawler starts following links
        // which are found in these pages
        controller.addSeed("https://www.ics.uci.edu/~lopes/");
        controller.addSeed("https://www.ics.uci.edu/~welling/");
    	controller.addSeed("https://www.ics.uci.edu/");
    	
    	// The factory which creates instances of crawlers.
        CrawlController.WebCrawlerFactory<BasicCrawler> factory = MyCrawler::new;
        
        // Start the crawl. This is a blocking operation, meaning that your code
        // will reach the line after this only when crawling is finished.
        controller.start(factory, numberOfCrawlers);
    }
}
```
## More Examples
- [Basic crawler](crawler4j-examples/crawler4j-examples-base/src/test/java/edu/uci/ics/crawler4j/examples/basic/): the full source code of the above example with more details.
- [Image crawler](crawler4j-examples/crawler4j-examples-base/src/test/java/edu/uci/ics/crawler4j/examples/imagecrawler/): a simple image crawler that downloads image content from the crawling domain and stores them in a folder. This example demonstrates how binary content can be fetched using crawler4j.
- [Collecting data from threads](crawler4j-examples/crawler4j-examples-base/src/test/java/edu/uci/ics/crawler4j/examples/localdata/): this example demonstrates how the controller can collect data/statistics from crawling threads.
- [Multiple crawlers](crawler4j-examples/crawler4j-examples-base/src/test/java/edu/uci/ics/crawler4j/examples/multiple/): this is a sample that shows how two distinct crawlers can run concurrently. For example, you might want to split your crawling into different domains and then take different crawling policies for each group. Each crawling controller can have its own configurations.
- [Shutdown crawling](crawler4j-examples/crawler4j-examples-base/src/test/java/edu/uci/ics/crawler4j/examples/shutdown/): this example shows how crawling can be terminated gracefully by sending the 'shutdown' command to the controller.
- [Postgres/JDBC integration](crawler4j-examples/crawler4j-examples-postgres/): this shows how to save the crawled content into a Postgres database (or any other JDBC repository), thanks [rzo1](https://github.com/rzo1/).

## Configuration Details
The controller class has a mandatory parameter of type [CrawlConfig](crawler4j/src/main/java/edu/uci/ics/crawler4j/crawler/CrawlConfig.java).
 Instances of this class can be used for configuring crawler4j. The following sections
describe some details of configurations.

### Crawl depth
By default there is no limit on the depth of crawling. But you can limit the depth of crawling. For example, assume that you have a seed page "A", which links to "B", which links to "C", which links to "D". So, we have the following link structure:

A -> B -> C -> D

Since, "A" is a seed page, it will have a depth of 0. "B" will have depth of 1 and so on. You can set a limit on the depth of pages that crawler4j crawls. For example, if you set this limit to 2, it won't crawl page "D". To set the maximum depth you can use:
```java
crawlConfig.setMaxDepthOfCrawling(maxDepthOfCrawling);
```

### Maximum number of pages to crawl
Although by default there is no limit on the number of pages to crawl, you can set a limit
on this:

```java
crawlConfig.setMaxPagesToFetch(maxPagesToFetch);
```

### Enable Binary Content Crawling
By default crawling binary content (i.e. images, audio etc.) is turned off. To enable crawling these files:

```java
crawlConfig.setIncludeBinaryContentInCrawling(true);
```

See an example [here](crawler4j-examples/crawler4j-examples-base/src/test/java/edu/uci/ics/crawler4j/examples/imagecrawler/) for more details.

### Politeness
crawler4j is designed very efficiently and has the ability to crawl domains very fast
(e.g., it has been able to crawl 200 Wikipedia pages per second). However, since this
is against crawling policies and puts huge load on servers (and they might block you!),
since version 1.3, by default crawler4j waits at least 200 milliseconds between requests.
However, this parameter can be tuned:

```java
crawlConfig.setPolitenessDelay(politenessDelay);
```

### Proxy
Should your crawl run behind a proxy? If so, you can use:

```java
crawlConfig.setProxyHost("proxyserver.example.com");
crawlConfig.setProxyPort(8080);
```
If your proxy also needs authentication:

```java
crawlConfig.setProxyUsername(username);
crawlConfig.setProxyPassword(password);
```

### Resumable Crawling
Sometimes you need to run a crawler for a long time. It is possible that the crawler
terminates unexpectedly. In such cases, it might be desirable to resume the crawling.
You would be able to resume a previously stopped/crashed crawl using the following
settings:

```java
crawlConfig.setResumableCrawling(true);
```
However, you should note that it might make the crawling slightly slower.

### User agent string
User-agent string is used for representing your crawler to web servers. See [here](http://en.wikipedia.org/wiki/User_agent)
for more details. By default crawler4j uses the following user agent string:

```
"crawler4j (https://github.com/rzo1/crawler4j/)"
```
However, you can overwrite it:

```java
crawlConfig.setUserAgentString(userAgentString);
```

## Reconstructing extra urls to crawl
In these heydays of JavaScript frameworks not all links can always be easily detected.  
Following is a naive implementation to add more links to crawl after prying them out of fetched content:  

```java
public class MyWebCrawler extends WebCrawler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MyWebCrawler.class);
	
	@Override
	public void visit(final Page page) {
		
		final List<WebURL> pageUrls = new ArrayList<>();
		// ... -> gathering/reconstructing urls from page.getContentData() or page.getParseData() (more likely)
		pageUrls.addAll(page.getParseData().getOutgoingUrls()); // Useless, these are already scheduled automatically!!
		
		// Technically possible, but it's better to define extra urls to fetch inside a custom HtmlParser.
		// That way the parent url will automatically be linked with it as well as the crawl depth.
		getMyController().getFrontier().scheduleAll(pageUrls);
		
		// Also possible, but again has no notion of crawl depth, parent url, no "shouldFollowLinksIn(...)"-functionality...
		final List<String> stringUrls = new ArrayList<>();
		try {
			getMyController().addSeeds(stringUrls);
		} catch (InterruptedException e) {
			LOGGER.error("Unable to add seeds -> exception: ", e);
		}
	}
}
```

A better approach is detecting the extra links while parsing. The links will get added to the list of ``page.getParseData().outgoingUrls()``:

```java
// Use this constructor:
new CrawlController(config, normalizer, pageFetcher, parser, robotstxtServer, tldList, frontierConfiguration);

// Of course first create a custom parser:
new Parser(config, normalizer, htmlParser, tldList, frontierConfiguration.getWebURLFactory());
// -> the magic should happen inside a custom htmlParser implementation.
```

## Authentication

Different forms of authentication are supported:
- BASIC_AUTHENTICATION
- FORM_AUTHENTICATION
- NT_AUTHENTICATION.

An example on how to configure form authentication in its most simple form:

```java
		final CrawlConfig config = new CrawlConfig();
		config.addAuthInfo(new FormAuthInfo(
				"myUser", "myReallyGoodPwd"
				, "https://www.test.com/login"
				, "username", "password"
		));
```

Overriding `FormAuthInfo.doFormLogin(...)` allows implementing more dynamic form authentication (entering more fields, fetching dynamic login forms, ...).

## High-level design diagrams

Below activity diagram highlights the most important steps and vocabulary to know as user of the library.
![crawler4j activity diagram](documentation/crawler4j%20activity%20diagram.svg "crawler4j activity diagram")

## License

Published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0), see LICENSE
