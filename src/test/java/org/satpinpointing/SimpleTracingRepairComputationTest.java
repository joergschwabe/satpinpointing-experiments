package org.satpinpointing;

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

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import org.junit.runners.Parameterized.Parameters;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;

public class SimpleTracingRepairComputationTest
		extends TracingJustificationComputationTest {

	@Parameters
	public static Collection<Object[]> parameters() throws URISyntaxException {
		return BaseJustificationComputationTest.getParameters(
				getRepairComputationFactories(), "test_input/simple",
				REPAIRS_DIR_NAME);
	}

	public SimpleTracingRepairComputationTest(
			final MinimalSubsetsFromProofs.Factory<Object, Inference<Object>, ElkAxiom> factory,
			final File ontoFile, final Map<File, File[]> entailFilesPerJustFile)
			throws Exception {
		super(factory, ontoFile, entailFilesPerJustFile);
	}

}
