/*-
 * #%L
 * de.hs-heilbronn.mi:crawler4j-core
 * %%
 * Copyright (C) 2010 - 2022 crawler4j-fork (pre-fork: Yasser Ganjisaffar)
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
package edu.uci.ics.crawler4j.crawler;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import crawlercommons.filters.basic.BasicURLNormalizer;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.test.TestUtils;
import edu.uci.ics.crawler4j.url.SleepycatWebURLFactory;
import edu.uci.ics.crawler4j.url.WebURLFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Disabled(value = "Flaky test on CI - too slow.")
class RedirectHandlerTest {

    @TempDir
  public File crawlStorageFolder;

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(new WireMockConfiguration().dynamicPort())
        .build();


    @ValueSource(ints = {300, 301})
    @ParameterizedTest
    void followRedirects(int redirectStatus) throws Exception {
        //given: "an index page with a ${redirectStatus}"
    	wm.stubFor(get(urlEqualTo("/some/index.html"))
                .willReturn(aResponse()
                .withStatus(redirectStatus)
                .withHeader("Location", "/another/index.html")));

    	wm.stubFor(get(urlPathMatching("/another/index.html"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(TestUtils.getInputStringFrom("/html/redirect/index.html")))
              );

        // when:
    	CrawlConfig config = new CrawlConfig();
      config.setCrawlStorageFolder(crawlStorageFolder.getAbsolutePath());
      config.setPolitenessDelay(100);
      config.setMaxConnectionsPerHost(1);
      config.setThreadShutdownDelaySeconds(1);
      config.setThreadMonitoringDelaySeconds(1);
      config.setCleanupDelaySeconds(1);
        
        // and: "and allow everything robots.txt"
        		wm.stubFor(get(urlPathMatching("/robots.txt"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody(TestUtils.getInputStringFrom("/robotstxt/robots.txt")))
              );

        BasicURLNormalizer normalizer = TestUtils.newNormalizer();
        WebURLFactory webURLFactory = new SleepycatWebURLFactory();
        PageFetcher pageFetcher = new PageFetcher(config, normalizer);
        RobotstxtServer robotstxtServer = new RobotstxtServer(new RobotstxtConfig(), pageFetcher, webURLFactory);
        CrawlController controller = new CrawlController(config, normalizer, pageFetcher, robotstxtServer, TestUtils.createFrontierConfiguration(config));
        controller.addSeed("http://localhost:" + wm.getPort() + "/some/index.html");

        controller.start(HandleRedirectWebCrawler.class, 1);

        // then: "envent in WebCrawler will trigger"
        List<Object> crawlerData = (List<Object>) controller.getCrawlersLocalData().get(0);
    		Assertions.assertThat(crawlerData.get(0)).isEqualTo(1);
				Assertions.assertThat(crawlerData.get(1)).isEqualTo("http://localhost:" + wm.getPort() + "/another/index.html");

        wm.verify(exactly(1), getRequestedFor(urlEqualTo("/some/index.html")));
        wm.verify(exactly(1), getRequestedFor(urlEqualTo("/another/index.html")));
    }
}


class HandleRedirectWebCrawler extends WebCrawler {

    int onRedirectedCounter = 0;
    List<Object> data = new ArrayList<>();

    @Override
		protected void onRedirectedStatusCode(Page page) {
        data.add(0, ++onRedirectedCounter);
        data.add(1, page.getRedirectedToUrl());
    }

    @Override
    public Object getMyLocalData() {
        return data;
    }
}

