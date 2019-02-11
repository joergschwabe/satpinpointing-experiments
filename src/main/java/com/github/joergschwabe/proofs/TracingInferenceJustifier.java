package com.github.joergschwabe.proofs;

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.liveontologies.puli.InferenceJustifier;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.visitors.DummyElkAxiomVisitor;
import org.semanticweb.elk.reasoner.tracing.Conclusion;
import org.semanticweb.elk.reasoner.tracing.ConclusionBaseFactory;
import org.semanticweb.elk.reasoner.tracing.DummyConclusionVisitor;
import org.semanticweb.elk.reasoner.tracing.TracingInference;
import org.semanticweb.elk.reasoner.tracing.TracingInferencePremiseVisitor;

public class TracingInferenceJustifier implements
		InferenceJustifier<TracingInference, Set<? extends ElkAxiom>> {

	public static final TracingInferenceJustifier INSTANCE = new TracingInferenceJustifier();

	private static final Conclusion.Factory CONCLUSION_FACTORY_ = new ConclusionBaseFactory();

	private static final Conclusion.Visitor<Void> DUMMY_CONCLUSION_VISITOR_ = new DummyConclusionVisitor<Void>();

	private TracingInferenceJustifier() {
		// Forbid instantiation.
	}

	@Override
	public Set<? extends ElkAxiom> getJustification(
			final TracingInference inference) {
		// else
		final Set<ElkAxiom> result = new HashSet<ElkAxiom>();
		inference.accept(new TracingInferencePremiseVisitor<>(
				CONCLUSION_FACTORY_, DUMMY_CONCLUSION_VISITOR_,
				new ElkAxiomCollector(result)));
		return result;
	}

	private static class ElkAxiomCollector extends DummyElkAxiomVisitor<Void> {

		private final Collection<ElkAxiom> axioms_;

		public ElkAxiomCollector(final Collection<ElkAxiom> axioms) {
			this.axioms_ = axioms;
		}

		@Override
		protected Void defaultLogicalVisit(ElkAxiom axiom) {
			axioms_.add(axiom);
			return null;
		}

	}

}
