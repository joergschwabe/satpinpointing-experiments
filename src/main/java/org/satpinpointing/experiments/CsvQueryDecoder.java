package org.satpinpointing.experiments;

/*-
 * #%L
 * Axiom Pinpointing Experiments
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2018 Live Ontologies Project
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

import com.google.common.base.Preconditions;

public class CsvQueryDecoder {

	public static interface Factory<Q> {
		Q createQuery(String subIri, String supIri);
	}

	public static <Q> Q decode(final String query, final Factory<Q> factory) {
		Preconditions.checkNotNull(query);

		final String[] columns = query.split(" ");
		if (columns.length < 2) {
			throw new IllegalArgumentException(
					"Invalid query format: " + query);
		}

		return factory.createQuery(columns[0], columns[1]);
	}

}
