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
package edu.uci.ics.crawler4j.tests.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.junit.jupiter.api.Test;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.test.Crawler4jTestUtils;
import edu.uci.ics.crawler4j.url.WebURL;

class PageTest {

	@Test
	void charsetFromContentType() throws IOException {
		String charset = "ISO-8859-1";
		testCharset(ContentType.create("text/html", charset), charset);
	}

	@Test
	void defaultCharsetFallback() throws IOException {
		ContentType contentType = ContentType.parse("text/html");
		// "charset should fallback to UTF-8"
		testCharset(contentType, "UTF-8");
	}

	private static void testCharset(ContentType contentType, String expectedCharset) throws IOException {
		String content = "The content";
		HttpEntity entity = new BasicHttpEntity(
				IOUtils.toInputStream(content, "UTF-8"), content.length(), contentType
		);

		WebURL u = Crawler4jTestUtils.newWebURLFactory().newWebUrl();
		Page page = new Page(u);
		page.load(entity, 1024);

		assertEquals(expectedCharset, page.getContentCharset());
	}
}
