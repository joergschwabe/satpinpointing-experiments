/*-
 * #%L
 * Proof Utility Library
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Live Ontologies Project
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
package com.github.joergschwabe.input.justifications;

import java.util.Collection;
import java.util.Set;

import com.github.joergschwabe.input.Cycles_2;
import com.google.common.collect.ImmutableSet;

public class Cycles_2Justifications extends Cycles_2 {

	@Override
	public Collection<? extends Set<? extends Integer>> getExpectedResult() {
		// @formatter:off
		return ImmutableSet.of(
				ImmutableSet.of(1, 17, 10, 4), 
				ImmutableSet.of(16, 2),
				ImmutableSet.of(14, 18, 3), 
				ImmutableSet.of(2, 18, 12, 6), 
				ImmutableSet.of(16, 1, 10, 11, 4, 13),
				ImmutableSet.of(1, 6, 18), 
				ImmutableSet.of(16, 1, 9, 11, 4), 
				ImmutableSet.of(17, 2, 10, 12, 4));
		// @formatter:on
	}

}
