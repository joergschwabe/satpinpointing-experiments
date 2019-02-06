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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.proofs.ElkProofProvider;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkDeclarationAxiom;
import org.semanticweb.elk.owl.visitors.DummyElkAxiomVisitor;
import org.semanticweb.elk.reasoner.TestReasonerUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public abstract class TracingJustificationComputationTest extends
		BaseJustificationComputationTest<ElkAxiom, Object, Inference<Object>, ElkAxiom> {

	private static final OWLOntologyManager OWL_MANAGER_ = OWLManager
			.createOWLOntologyManager();

	public TracingJustificationComputationTest(
			final MinimalSubsetsFromProofs.Factory<Object, Inference<Object>, ElkAxiom> factory,
			final File ontoFile, final Map<File, File[]> entailFilesPerJustFile)
			throws Exception {
		super(new ElkProofProvider(ontoFile, OWL_MANAGER_), factory, ontoFile,
				entailFilesPerJustFile);
	}

	@Override
	protected ElkAxiom getQuery(final File entailFile) throws Exception {
		return filterLogical(TestReasonerUtils.loadAxioms(entailFile))
				.iterator().next();
	}

	@Override
	public Set<? extends Set<? extends ElkAxiom>> getExpectedJustifications(
			final File[] justFiles) throws Exception {
		final Set<Set<? extends ElkAxiom>> expectedJusts = new HashSet<>();
		for (final File justFile : justFiles) {
			final Set<? extends ElkAxiom> just = filterLogical(
					TestReasonerUtils.loadAxioms(justFile));
			expectedJusts.add(just);
		}
		return expectedJusts;
	}

	private static Set<? extends ElkAxiom> filterLogical(
			final Set<? extends ElkAxiom> axioms) {
		final Set<? extends ElkAxiom> result = new HashSet<>(axioms);
		final Iterator<? extends ElkAxiom> iter = result.iterator();
		while (iter.hasNext()) {
			iter.next().accept(new DummyElkAxiomVisitor<Void>() {

				@Override
				protected Void defaultNonLogicalVisit(final ElkAxiom axiom) {
					iter.remove();
					return null;
				}

				@Override
				public Void visit(final ElkDeclarationAxiom axiom) {
					iter.remove();
					return null;
				}

			});
		}
		return result;
	}

	@Override
	public void dispose() {
		super.dispose();
		final Collection<OWLOntology> ontologies = new ArrayList<>(
				OWL_MANAGER_.getOntologies());
		for (final OWLOntology ontology : ontologies) {
			OWL_MANAGER_.removeOntology(ontology);
		}
	}

}
