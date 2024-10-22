/*-
 * #%L
 * de.hs-heilbronn.mi:crawler4j-core
 * %%
 * Copyright (C) 2010 - 2021 crawler4j-fork (pre-fork: Yasser Ganjisaffar)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package edu.uci.ics.crawler4j.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import crawlercommons.filters.basic.BasicURLNormalizer;
import edu.uci.ics.crawler4j.url.UrlResolver;
import edu.uci.ics.crawler4j.url.WebURLFactory;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.JSoupParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.exceptions.ParseException;
import edu.uci.ics.crawler4j.url.TLDList;
import edu.uci.ics.crawler4j.url.WebURL;

public class TikaHtmlParser implements edu.uci.ics.crawler4j.parser.HtmlParser {
    protected static final Logger logger = LoggerFactory.getLogger(TikaHtmlParser.class);

    private final CrawlConfig config;
    private final TLDList tldList;

    private final Parser htmlParser;
    private final ParseContext parseContext;
    private final WebURLFactory factory;
    private final BasicURLNormalizer normalizer;

    public TikaHtmlParser(CrawlConfig config, BasicURLNormalizer normalizer, TLDList tldList, WebURLFactory webURLFactory) {
        this.config = config;
        this.tldList = tldList;
        this.normalizer = normalizer;

        htmlParser = new JSoupParser();
        parseContext = new ParseContext();
        parseContext.set(HtmlMapper.class, new AllTagMapper());
        this.factory = webURLFactory;
    }

    public HtmlParseData parse(Page page, String contextURL) throws ParseException {
        HtmlParseData parsedData = new HtmlParseData();

        HtmlContentHandler contentHandler = new HtmlContentHandler();
        Metadata metadata = new Metadata();

        if (page.getContentType() != null) {
            metadata.add(Metadata.CONTENT_TYPE, page.getContentType());
        }

        try (InputStream inputStream = new ByteArrayInputStream(page.getContentData())) {
            htmlParser.parse(inputStream, contentHandler, metadata, parseContext);
        } catch (Exception e) {
            logger.error("{}, while parsing: {}", e.getMessage(), page.getWebURL().getURL());
            throw new ParseException("could not parse [" + page.getWebURL().getURL() + "]", e);
        }

        String contentCharset = chooseEncoding(page, metadata);
        parsedData.setContentCharset(contentCharset);

        parsedData.setText(contentHandler.getBodyText().trim());
        parsedData.setTitle(metadata.get(DublinCore.TITLE));
        parsedData.setMetaTags(contentHandler.getMetaTags());

        try {
            Set<WebURL> outgoingUrls = getOutgoingUrls(contextURL, contentHandler);
            parsedData.setOutgoingUrls(outgoingUrls);

            if (page.getContentCharset() == null) {
                parsedData.setHtml(new String(page.getContentData(), StandardCharsets.UTF_8));
            } else {
                parsedData.setHtml(new String(page.getContentData(), page.getContentCharset()));
            }

            return parsedData;
        } catch (UnsupportedEncodingException e) {
            logger.error("error parsing the html: " + page.getWebURL().getURL(), e);
            throw new ParseException("could not parse [" + page.getWebURL().getURL() + "]", e);
        }

    }

    private Set<WebURL> getOutgoingUrls(String contextURL, HtmlContentHandler contentHandler) {
        Set<WebURL> outgoingUrls = new HashSet<>();

        String baseURL = contentHandler.getBaseUrl();
        if (baseURL != null) {
            contextURL = baseURL;
        }

        int urlCount = 0;
        for (ExtractedUrlAnchorPair urlAnchorPair : contentHandler.getOutgoingUrls()) {

            String href = urlAnchorPair.getHref();
            if ((href == null) || href.trim().isEmpty()) {
                continue;
            }

            String hrefLoweredCase = href.trim().toLowerCase(Locale.ROOT);
            if (!containsForbiddenRefTag(hrefLoweredCase)) {
                String url = normalizer.filter(UrlResolver.resolveUrl((contextURL == null) ? "" : contextURL, href));
                if (url != null) {
                    WebURL webURL = factory.newWebUrl();
                    webURL.setTldList(tldList);
                    webURL.setURL(url);
                    webURL.setTag(urlAnchorPair.getTag());
                    webURL.setAnchor(urlAnchorPair.getAnchor());
                    webURL.setAttributes(urlAnchorPair.getAttributes());
                    outgoingUrls.add(webURL);
                    urlCount++;
                    if (urlCount > config.getMaxOutgoingLinksToFollow()) {
                        break;
                    }
                }
            }
        }
        return outgoingUrls;
    }

    protected boolean containsForbiddenRefTag(String href) {
        return Stream.of("about:", "tel:", "data:", "whatsapp:", "javascript:", "viber:", "sms:", "android-app:", "fb-messenger:", "mailto:", "@").anyMatch(href::contains);
    }

    private String chooseEncoding(Page page, Metadata metadata) {
        String pageCharset = page.getContentCharset();
        if (pageCharset == null || pageCharset.isEmpty()) {
            return metadata.get("Content-Encoding");
        }
        return pageCharset;
    }
}
