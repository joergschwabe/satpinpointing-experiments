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
package com.github.joergschwabe;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;

@RunWith(Parameterized.class)
public class SatJustification_LogicNgTest<C, I extends Inference<? extends C>, A> extends BaseEnumeratorTest<C, I, A> {

	public static final String TEST_INPUT_SUBPKG = "input.justifications";

	public static List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> getJustificationEnumeratorFactories() {
		final List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> factories = new ArrayList<MinimalSubsetsFromProofs.Factory<?, ?, ?>>();
		factories.add(SatJustificationComp_LogicNg.getFactory());
		return factories;
	}

	@Parameters(name = "{index}: {0}")
	public static Iterable<Object[]> data() throws Exception {
		return getParameters(getJustificationEnumeratorFactories(), TEST_INPUT_SUBPKG);
	}

}
