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

import java.util.HashSet;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;

import com.google.common.base.Preconditions;

public class ProofAndJustifierBuilder<C, A> extends ProofBuilder<C> {

	private final InferenceJustifier<JustifiedInference<C, A>, Set<? extends A>> justifier_ = new InferenceJustifier<JustifiedInference<C, A>, Set<? extends A>>() {

		@Override
		public Set<? extends A> getJustification(
				final JustifiedInference<C, A> inference) {
			return inference.justification_;
		}

	};

	public ProofAndJustifierBuilder() {
		// Empty.
	}

	@SuppressWarnings("unchecked")
	@Override
	public Proof<? extends JustifiedInference<C, A>> build() {
		return (Proof<? extends JustifiedInference<C, A>>) super.build();
	}

	public InferenceJustifier<JustifiedInference<C, A>, ? extends Set<? extends A>> buildJustifier() {
		return justifier_;
	}

	@Override
	public ThisInferenceBuilder conclusion(C conclusion) {
		ThisInferenceBuilder result = new ThisInferenceBuilder();
		result.conclusion(conclusion);
		return result;
	}

	public class ThisInferenceBuilder
			extends ProofBuilder<C>.ThisInferenceBuilder {

		private final Set<A> axioms_ = new HashSet<A>();

		protected ThisInferenceBuilder() {
			super();
		}

		@Override
		ThisInferenceBuilder conclusion(C conclusion) {
			super.conclusion(conclusion);
			return this;
		}

		@Override
		public ThisInferenceBuilder premise(C premise) {
			super.premise(premise);
			return this;
		}

		public ThisInferenceBuilder axiom(final A axiom) {
			Preconditions.checkNotNull(axiom);
			axioms_.add(axiom);
			return this;
		}

		@Override
		Inference<C> build() {
			return new JustifiedInference<C, A>(getName(), getConclusion(),
					getPremises(), axioms_);
		}
		
	}

}
