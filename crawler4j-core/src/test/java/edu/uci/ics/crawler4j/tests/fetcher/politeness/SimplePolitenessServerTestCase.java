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
package edu.uci.ics.crawler4j.tests.fetcher.politeness;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.fetcher.politeness.CachedPolitenessServer;
import edu.uci.ics.crawler4j.fetcher.politeness.SimplePolitenessServer;
import edu.uci.ics.crawler4j.test.SimpleWebURL;
import edu.uci.ics.crawler4j.url.WebURL;

import java.util.concurrent.TimeUnit;

@Disabled(value = "Flaky test on CI")
public class SimplePolitenessServerTestCase {

    private edu.uci.ics.crawler4j.PolitenessServer simplePolitenessServer;
    private CrawlConfig config;

    @BeforeEach
    public void init() {
        this.config = new CrawlConfig();
        this.config.setPolitenessDelay(100);
        this.simplePolitenessServer = new SimplePolitenessServer(config);
    }

    @Test
    public void testApplyPoliteness1() {

        WebURL webUrl = new SimpleWebURL();
        webUrl.setURL("https://github.com/yasserg/crawler4j");

        long politenessDelay = simplePolitenessServer.applyPoliteness(webUrl);

        Assertions.assertThat(politenessDelay).isEqualTo(CachedPolitenessServer.NO_POLITENESS_APPLIED);

        politenessDelay = simplePolitenessServer.applyPoliteness(webUrl);

        Assertions.assertThat(politenessDelay).isBetween(//
                (long) (config.getPolitenessDelay() - 10)//
        		, (long) config.getPolitenessDelay());

    }

    @Test
    public void testApplyPoliteness2() {

        WebURL webUrl = new SimpleWebURL();
        webUrl.setURL("https://github.com/yasserg/crawler4j");

        long politenessDelay = simplePolitenessServer.applyPoliteness(webUrl);

        Assertions.assertThat(politenessDelay).isEqualTo(CachedPolitenessServer.NO_POLITENESS_APPLIED);

        webUrl.setURL("https://github.com/yasserg/crawler4j/blob/master/pom.xml");

        politenessDelay = simplePolitenessServer.applyPoliteness(webUrl);

        Assertions.assertThat(politenessDelay).isBetween(//
                (long) (config.getPolitenessDelay() - 10)//
        		, (long) config.getPolitenessDelay());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            final long delay = simplePolitenessServer.applyPoliteness(webUrl);
            Assertions.assertThat(delay).isEqualTo(CachedPolitenessServer.NO_POLITENESS_APPLIED);
        });

    }

    @Test
    public void testApplyPoliteness3() {

        WebURL webUrl = new SimpleWebURL();
        webUrl.setURL("https://github.com/yasserg/crawler4j");

        long politenessDelay = simplePolitenessServer.applyPoliteness(webUrl);

        Assertions.assertThat(politenessDelay).isEqualTo(CachedPolitenessServer.NO_POLITENESS_APPLIED);

        webUrl.setURL("http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentLinkedQueue.html");

        politenessDelay = simplePolitenessServer.applyPoliteness(webUrl);

        Assertions.assertThat(politenessDelay).isBetween(//
                (long) (config.getPolitenessDelay() - 10)//
        		, (long) config.getPolitenessDelay());

        webUrl.setURL("https://github.com/yasserg/crawler4j/blob/master/pom.xml");

        politenessDelay = simplePolitenessServer.applyPoliteness(webUrl);

        Assertions.assertThat(politenessDelay).isEqualTo(config.getPolitenessDelay());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            final long delay = simplePolitenessServer.applyPoliteness(webUrl);
            Assertions.assertThat(delay).isEqualTo(CachedPolitenessServer.NO_POLITENESS_APPLIED);
        });

    }

}
