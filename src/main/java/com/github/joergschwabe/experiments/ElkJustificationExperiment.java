package com.github.joergschwabe.experiments;

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

import org.liveontologies.puli.Inference;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkObject;
import org.semanticweb.elk.owl.iris.ElkFullIri;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.joergschwabe.proofs.CsvQueryProofProvider;
import com.github.joergschwabe.proofs.ElkProofProvider;
import com.github.joergschwabe.proofs.ProofProvider;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public abstract class ElkJustificationExperiment<O extends ElkJustificationExperiment.Options>
		extends
		BaseJustificationExperiment<O, Object, Inference<Object>, ElkAxiom> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ElkJustificationExperiment.class);

	public static final String ONTOLOGY_OPT = "ontology";

	public static class Options extends BaseJustificationExperiment.Options {
		@Arg(dest = ONTOLOGY_OPT)
		public File ontologyFile;
	}

	private File ontologyFile_;

	private OWLOntologyManager manager_ = null;
	private OWLOntologyManager getManager() {
		if (manager_ == null) {
			manager_ = OWLManager.createOWLOntologyManager();
		}
		return manager_;
	}

	@Override
	protected void addArguments(final ArgumentParser parser) {
		parser.addArgument(ONTOLOGY_OPT)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("ontology file");
	}

	@Override
	protected void init(final O options) throws ExperimentException {
		LOGGER_.info("ontologyFile: {}", options.ontologyFile);
		this.ontologyFile_ = options.ontologyFile;
	}

	@Override
	protected ProofProvider<String, Object, Inference<Object>, ElkAxiom> newProofProvider()
			throws ExperimentException {

		final ElkProofProvider elkProofProvider = new ElkProofProvider(
				ontologyFile_, getManager());
		final ElkObject.Factory factory = elkProofProvider.getReasoner()
				.getElkFactory();

		final CsvQueryDecoder.Factory<ElkAxiom> decoder = new CsvQueryDecoder.Factory<ElkAxiom>() {

			@Override
			public ElkAxiom createQuery(final String subIri,
					final String supIri) {
				return factory.getSubClassOfAxiom(
						factory.getClass(new ElkFullIri(subIri)),
						factory.getClass(new ElkFullIri(supIri)));
			}

		};
		final ProofProvider<String, Object, Inference<Object>, ElkAxiom> proofProvider = new CsvQueryProofProvider<>(
				decoder, elkProofProvider);

		return proofProvider;
	}

}
