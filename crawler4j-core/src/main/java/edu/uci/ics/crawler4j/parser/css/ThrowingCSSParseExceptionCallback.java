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
package edu.uci.ics.crawler4j.parser.css;

import org.jspecify.annotations.NonNull;

import com.helger.css.handler.ICSSParseExceptionCallback;
import com.helger.css.parser.ParseException;

public class ThrowingCSSParseExceptionCallback implements ICSSParseExceptionCallback {
	
	@Override
	public void onException (@NonNull final ParseException ex) {
		throw new RuntimeException(ex);
	}
}
