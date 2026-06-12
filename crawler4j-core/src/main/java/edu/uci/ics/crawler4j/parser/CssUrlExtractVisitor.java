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
package edu.uci.ics.crawler4j.parser;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSExpressionMemberTermURI;
import com.helger.css.decl.CSSImportRule;
import com.helger.css.decl.ICSSTopLevelRule;
import com.helger.css.decl.visit.DefaultCSSUrlVisitor;

import crawlercommons.filters.basic.BasicURLNormalizer;
import edu.uci.ics.crawler4j.url.UrlResolver;

public class CssUrlExtractVisitor extends DefaultCSSUrlVisitor {
	
	private final String referenceAbsoluteUrl;
	private final BasicURLNormalizer normalizer;
	private final Set<String> seedUrls = new LinkedHashSet<>();
	
	
	public CssUrlExtractVisitor(final String referenceAbsoluteUrl, final BasicURLNormalizer normalizer) {
		this.referenceAbsoluteUrl = referenceAbsoluteUrl;
		this.normalizer = normalizer;
	}
	
	
	
	@Override
	public void onImport(@NonNull final CSSImportRule aImportRule) {
		addSeedUrl(aImportRule.getLocationString());
	}
	
	
	@Override
	public void onUrlDeclaration(@Nullable final ICSSTopLevelRule aTopLevelRule,
			@NonNull final CSSDeclaration aDeclaration, @NonNull final CSSExpressionMemberTermURI aURITerm)
	{
		addSeedUrl(aURITerm.getURIString());
	}
	
	
	
	/**
	 * @return the added seed url or empty
	 */
	protected Optional<String> addSeedUrl(final String url) {
		if (StringUtils.isBlank(url) || url.startsWith("data:")) {
			return Optional.empty();
		}
		
		final String seedUrl = toSeedUrl(url);
		seedUrls.add(seedUrl);
		return Optional.of(seedUrl);
	}
	
	private String toSeedUrl(final String url) {
		// Normalization is needed, because the String will be input for URI.create(...).
		return normalizer.filter(UrlResolver.resolveUrl((referenceAbsoluteUrl == null) ? "" : referenceAbsoluteUrl, url));
	}
	
	
	
	public String getReferenceAbsoluteUrl() {
		return referenceAbsoluteUrl;
	}
	
	public BasicURLNormalizer getNormalizer() {
		return normalizer;
	}
	
	public Set<String> getSeedUrls() {
		return Collections.unmodifiableSet(seedUrls);
	}
}
