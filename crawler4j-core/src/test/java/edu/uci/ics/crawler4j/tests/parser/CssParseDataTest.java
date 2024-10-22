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
package edu.uci.ics.crawler4j.tests.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.helger.css.parser.ParseException;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.CssParseData;
import edu.uci.ics.crawler4j.test.Crawler4jTestUtils;
import edu.uci.ics.crawler4j.test.TestUtils;
import edu.uci.ics.crawler4j.url.WebURL;

public class CssParseDataTest {
	
	@Test
	void extractUrlWithParenthesisTest() {
		final String cssText = TestUtils.getInputStringFrom("/css/parenthesis.css");
		assertUrlsFound(cssText//
				// This was previously the result, but is wrong
//				, "http://www.test.com/path/to/'leaves-medium%20(1920x1280"//
				// This is what should be the result
				, "http://www.test.com/path/to/leaves-medium%20(1920x1280).jpg"//
				, "http://www.test.com/images/other-wallpaper.png"//
		);
	}
	
	/**
	 * <p>
	 * Remark: no urls should be found.
	 * </p><p>
	 * A problem was that the pattern inside CssParseData will match parts of data urls instead of the complete url.
	 * More specifically, following will get matched
	 * by the expression for "url(...)" instead of
	 * by the expression for "url("...")":
	 * url("...)...")
	 * <p>
	 */
	@Test
	void extractUrlInCssTextIgnoresDataUrlsFromBootstrapMinCssTest() {
		final String cssText = TestUtils.getInputStringFrom("/css/bootstrap.min.css");
		assertNoURLsFoundInCSS(cssText);
	}
	
	/**
	 * Remark: no urls should be found.
	 */
	@Test
	void extractUrlInCssTextIgnoresDataUrlFromBootstrapSubsetTest() {
		final String cssText = TestUtils.getInputStringFrom("/css/bootstrap.min-subset.css");
		assertNoURLsFoundInCSS(cssText);
	}
	
	/**
	 * Remark: the host of an absolute url should not be altered.
	 */
	@Test
	void extractAbsoluteUrlFromCssTest() {
		// This css is a subset of: https://fonts.googleapis.com/css?family=Lato|Sanchez:400italic,400|Abhaya+Libre
		final String cssText = TestUtils.getInputStringFrom("/css/fonts-absolute.css");
		assertUrlsFound(cssText//
		// This was previously the result, but is wrong
//				, "http://www.test.com/s/sanchez/v13/Ycm0sZJORluHnXbIfmxh_zQA.woff2"//
		// This is what should be the result
				, "https://fonts.gstatic.com/s/sanchez/v13/Ycm0sZJORluHnXbIfmxh_zQA.woff2"//
		);
	}
	
	@Test
	void extractRelativeUrlFromCssTest() {
		final String cssText = TestUtils.getInputStringFrom("/css/fonts-relative.css");
		assertUrlsFound(cssText//
				, "http://www.test.com/path/s/sanchez/v13/Ycm0sZJORluHnXbIfmxh_zQA.woff2"//
		);
	}
	
	private void assertNoURLsFoundInCSS(final String cssText) {
		final WebURL webURL = Crawler4jTestUtils.newWebURL("http://www.test.com/path/to/bootstrap.min.css");
		
		final CssParseData cssParseData = Crawler4jTestUtils.newCssParseData();
		cssParseData.setTextContent(cssText);
        try {
            cssParseData.parseAndSetOutgoingUrls(new Page(webURL));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
		final Set<WebURL> outgoingUrls = cssParseData.getOutgoingUrls();
		assertNotNull(outgoingUrls);
		Assertions.assertThat(outgoingUrls).isEmpty();
	}
	
	private void assertUrlsFound(final String cssText, final String... urls) {
		final WebURL webURL = Crawler4jTestUtils.newWebURL("http://www.test.com/path/to/bootstrap.min.css");
		
		final CssParseData cssParseData = Crawler4jTestUtils.newCssParseData();
		cssParseData.setTextContent(cssText);
		try {
			cssParseData.parseAndSetOutgoingUrls(new Page(webURL));
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		final Set<WebURL> outgoingUrls = cssParseData.getOutgoingUrls();
		
		Assertions.assertThat(outgoingUrls).hasSize(urls.length);
		Assertions.assertThat(outgoingUrls).map(t -> t.getURL()).isSubsetOf(urls);
	}
	
	@Test
	void cSSUrlsParsingQuotes() {
		CssParseData parseData = Crawler4jTestUtils.newCssParseData();
		parseData.setTextContent(TestUtils.getInputStringFrom("/css/quotes.css"));
		WebURL webUrl = Crawler4jTestUtils.newWebURL("http://example.com/css.css");
		
		try {
			parseData.parseAndSetOutgoingUrls(new Page(webUrl));
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		Set<WebURL> urls = parseData.getOutgoingUrls();
		
		Assertions.assertThat(urls).hasSize(3);
	}
	
	@Test
	void cSSAbsoluteUrlsPaths() {
		CssParseData parseData = Crawler4jTestUtils.newCssParseData();
		parseData.setTextContent(TestUtils.getInputStringFrom("/css/absolute.css"));
		// This use case is rather simple. A css file can contain absolute urls to resources that reside on a different host.
		WebURL webUrl = Crawler4jTestUtils.newWebURL("http://example.com/css.css");
		
		try {
			parseData.parseAndSetOutgoingUrls(new Page(webUrl));
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		Set<WebURL> urls = parseData.getOutgoingUrls();
		
		Assertions.assertThat(urls).hasSize(3);
		Assertions.assertThat(urls).map(t -> t.getURL()).isSubsetOf(//
				"http://example.com/css/absolute_no_proto.png"//
				, "http://example.com/css/absolute_path.png"//
				, "http://example.com/css/absolute_with_domain.png"//
		);
	}
	
	@Test
	void cSSRelativeUrlsPaths() {
		CssParseData parseData = Crawler4jTestUtils.newCssParseData();
		parseData.setTextContent(TestUtils.getInputStringFrom("/css/relative.css"));
		WebURL webUrl = Crawler4jTestUtils.newWebURL("http://example.com/asset/css/css.css");
		
		try {
			parseData.parseAndSetOutgoingUrls(new Page(webUrl));
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		Set<WebURL> urls = parseData.getOutgoingUrls();
		
		Assertions.assertThat(urls).hasSize(2);
		Assertions.assertThat(urls).map(t -> t.getURL()).isSubsetOf(//
				"http://example.com/asset/images/backgound_one.jpg"//
				, "http://example.com/backgound_two.jpg"//
		);
	}
	
	@Test
	void nonParsableCssHaltingOnErrorTrueTest() {
		Assertions.assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
			final String cssTextInput = TestUtils.getInputStringFrom("/css/non-parsable.css");
			
			final WebURL webURL = Crawler4jTestUtils.newWebURL("http://www.test.com/path/to/some.css");
			
			final boolean haltOnError = true;
			final CssParseData cssParseData = Crawler4jTestUtils.newCssParseData(haltOnError);
			cssParseData.setTextContent(cssTextInput);
			cssParseData.parseAndSetOutgoingUrls(new Page(webURL));
		}).havingCause().isInstanceOf(ParseException.class);
	}
	
	@Test
	void nonParsableCssHaltingOnErrorFalseTest() throws Exception {
		final String cssTextInput = TestUtils.getInputStringFrom("/css/non-parsable.css");
		
		final WebURL webURL = Crawler4jTestUtils.newWebURL("http://www.test.com/path/to/some.css");
		
		final boolean haltOnError = false;
		final CssParseData cssParseData = Crawler4jTestUtils.newCssParseData(haltOnError);
		cssParseData.setTextContent(cssTextInput);
		cssParseData.parseAndSetOutgoingUrls(new Page(webURL));
		
		final Set<WebURL> outgoingUrls = cssParseData.getOutgoingUrls();
		
		// When parsing fails, no outgoingUrls are provisioned.
		Assertions.assertThat(outgoingUrls).isEmpty();
	}
}
