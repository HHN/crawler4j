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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.crawler4j.parser;

import java.util.HashSet;
import java.util.Set;

import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;

import crawlercommons.filters.basic.BasicURLNormalizer;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.css.ThrowingCSSParseExceptionCallback;
import edu.uci.ics.crawler4j.url.WebURL;
import edu.uci.ics.crawler4j.url.WebURLFactory;

public class CssParseData extends TextParseData {
	
	private final WebURLFactory factory;
	private final BasicURLNormalizer normalizer;
	private final CSSReaderSettings cssReaderSettings;

	public CssParseData(final WebURLFactory webURLFactory, final BasicURLNormalizer normalizer, boolean haltOnError) {
		this.factory = webURLFactory;
		this.normalizer = normalizer;
		this.cssReaderSettings = new CSSReaderSettings();
		if (haltOnError) {
			// When parsing fails null is returned by the framework and NullPointerExceptions can arise later on.
			this.cssReaderSettings.setCustomExceptionHandler(new ThrowingCSSParseExceptionCallback());
		}
	}
	
	@Override
	public void parseAndSetOutgoingUrls(final Page page) throws Exception {
		final Set<WebURL> outgoingUrls = parseOutgoingUrls(page.getWebURL());
		this.setOutgoingUrls(outgoingUrls);
	}
	
	private Set<WebURL> parseOutgoingUrls(final WebURL referringPage) {
		
		final Set<String> seedUrls = extractSeedUrlsFromCss(this.getTextContent(), referringPage);
		
		final Set<WebURL> outgoingUrls = new HashSet<>();
		for (final String seedUrl : seedUrls) {
			final WebURL webURL = factory.newWebUrl();
			webURL.setURL(seedUrl);
			outgoingUrls.add(webURL);
		}
		return outgoingUrls;
	}
	
	private Set<String> extractSeedUrlsFromCss(final String input, final WebURL referringPage) {
		if (input == null || input.isEmpty()) {
			return new HashSet<>();
		}
		
		final CascadingStyleSheet css = CSSReader.readFromStringReader(input, getCssReaderSettings());
		if (css == null) { // Parsing failed and "haltOnError" is false.
			return new HashSet<>();
		}
		final CssUrlExtractVisitor cssVisitor = new CssUrlExtractVisitor(referringPage.getURL(), normalizer);
		
		CSSVisitor.visitCSSUrl(css, cssVisitor);
		return cssVisitor.getSeedUrls();
	}

    protected CSSReaderSettings getCssReaderSettings() {
        return cssReaderSettings;
    }
}
