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
import org.satpinpointing.experiments.ExperimentException;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.semanticweb.owlapi.model.OWLAxiom;

public class SimpleOwlJustificationComputationTest
		extends OwlJustificationComputationTest {

	@Parameters
	public static Collection<Object[]> parameters() throws URISyntaxException {
		return BaseJustificationComputationTest.getParameters(
				getJustificationComputationFactories(), "test_input/simple",
				JUSTIFICATION_DIR_NAME);
	}

	public SimpleOwlJustificationComputationTest(
			final MinimalSubsetsFromProofs.Factory<OWLAxiom, Inference<OWLAxiom>, OWLAxiom> factory,
			final File ontoFile, final Map<File, File[]> entailFilesPerJustFile)
			throws ExperimentException {
		super(factory, ontoFile, entailFilesPerJustFile);
	}

}
