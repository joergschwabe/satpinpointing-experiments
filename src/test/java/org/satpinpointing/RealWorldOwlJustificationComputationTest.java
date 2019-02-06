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
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.runners.Parameterized.Parameters;
import org.satpinpointing.experiments.ExperimentException;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.google.common.collect.Iterators;

public class RealWorldOwlJustificationComputationTest
		extends OwlJustificationComputationTest {

	@Parameters
	public static Collection<Object[]> parameters() throws URISyntaxException {

		final List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> computations = getJustificationComputationFactories();

		final Collection<Object[]> galenParams = BaseJustificationComputationTest
				.getParameters(computations, "test_input/full-galen_cel",
						JUSTIFICATION_DIR_NAME);
		final Collection<Object[]> goParams = BaseJustificationComputationTest
				.getParameters(computations, "test_input/go_cel",
						JUSTIFICATION_DIR_NAME);

		return new AbstractCollection<Object[]>() {

			@Override
			public Iterator<Object[]> iterator() {
				return Iterators.concat(galenParams.iterator(),
						goParams.iterator());
			}

			@Override
			public int size() {
				return galenParams.size() + goParams.size();
			}

		};
	}

	public RealWorldOwlJustificationComputationTest(
			final MinimalSubsetsFromProofs.Factory<OWLAxiom, Inference<OWLAxiom>, OWLAxiom> factory,
			final File ontoFile, final Map<File, File[]> entailFilesPerJustFile)
			throws ExperimentException {
		super(factory, ontoFile, entailFilesPerJustFile);
	}

}
