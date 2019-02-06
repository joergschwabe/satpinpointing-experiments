package org.proofs;

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
import java.util.Set;

import org.liveontologies.owlapi.proof.OWLProver;
import org.satpinpointing.experiments.ExperimentException;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.InferenceJustifiers;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.statistics.NestedStats;
import org.semanticweb.elk.owlapi.ElkProverFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OwlProofProvider implements
		ProofProvider<OWLAxiom, OWLAxiom, Inference<OWLAxiom>, OWLAxiom> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(OwlProofProvider.class);

	private final OWLProver reasoner_;

	public OwlProofProvider(final File ontologyFile,
			final OWLOntologyManager manager) throws ExperimentException {

		try {

			LOGGER_.info("Loading ontology ...");
			long start = System.currentTimeMillis();
			final OWLOntology ont = manager
					.loadOntologyFromOntologyDocument(ontologyFile);
			LOGGER_.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);
			LOGGER_.info("Loaded ontology: {}", ont.getOntologyID());

			reasoner_ = new ElkProverFactory().createReasoner(ont);

			LOGGER_.info("Classifying ...");
			start = System.currentTimeMillis();
			reasoner_.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			LOGGER_.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);

		} catch (final OWLOntologyCreationException e) {
			throw new ExperimentException(e);
		}

	}

	@NestedStats(name = "elk")
	public OWLProver getReasoner() {
		return reasoner_;
	}

	@Override
	public JustificationCompleteProof<OWLAxiom, Inference<OWLAxiom>, OWLAxiom> getProof(
			final OWLAxiom query) throws ExperimentException {

		final InferenceJustifier<Inference<OWLAxiom>, ? extends Set<? extends OWLAxiom>> justifier = InferenceJustifiers
				.justifyAssertedInferences();

		return new JustificationCompleteProof<OWLAxiom, Inference<OWLAxiom>, OWLAxiom>() {

			@Override
			public OWLAxiom getQuery() {
				return query;
			}

			@Override
			public Proof<? extends Inference<OWLAxiom>> getProof() {
				return reasoner_.getProof(query);
			}

			@Override
			public InferenceJustifier<? super Inference<OWLAxiom>, ? extends Set<? extends OWLAxiom>> getJustifier() {
				return justifier;
			}

		};

	}

	@Override
	public void dispose() {
		reasoner_.dispose();
	}

}
