package com.github.joergschwabe;

/*-
 * #%L
 * Axiom Pinpointing Experiments
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2019 Live Ontologies Project
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Inferences;
import org.liveontologies.puli.Producer;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.Proofs;

/**
 * @author Yevgeny Kazakov
 *
 * @param <C> the type of conclusions used in inferences
 * @param <I> the type of inferences used in the proof
 * @param <A> the type of axioms used by the inferences
 */
public class IntegerProofTranslator<C, I extends Inference<? extends C>, A> {

	private Proof<? extends I> proof_;
	private InferenceJustifier<? super I, ? extends Set<? extends A>> justifier_;
	protected Map<Integer, Collection<Inference<Integer>>> inferences_ = new HashMap<>();

	public IntegerProofTranslator(Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier) {
		this.justifier_ = justifier;
		this.proof_ = proof;
	}

	Proof<Inference<? extends Integer>> getTranslatedProof(IdProvider<A, I> idProvider, Object query) {
		return new TranslatedProof(idProvider, query);
	}

	class TranslatedProof implements Proof<Inference<? extends Integer>>, Producer<I> {

		protected IdProvider<A, I> idProvider_;

		TranslatedProof(IdProvider<A, I> idProvider, Object query) {
			this.idProvider_ = idProvider;
			Proofs.unfoldRecursively(proof_, query, this);
		}

		@Override
		public void produce(I inference) {
			// translation to integer inferences
			C conclusion = inference.getConclusion();
			List<?> premises = inference.getPremises();
			Set<? extends A> justifications = justifier_.getJustification(inference);
			List<Integer> translatedPremises = new ArrayList<Integer>(premises.size() + justifications.size());
			for (C premise : inference.getPremises()) {
				translatedPremises.add(idProvider_.getConclusionId(premise));
			}
			for (A axiom : justifier_.getJustification(inference)) {
				translatedPremises.add(idProvider_.getAxiomId(axiom));
			}
			int translatedConclusion = idProvider_.getConclusionId(conclusion);
			Collection<Inference<Integer>> currentInferences = inferences_.get(translatedConclusion);
			if (currentInferences == null) {
				currentInferences = new ArrayList<Inference<Integer>>();
				inferences_.put(translatedConclusion, currentInferences);
			}
			Inference<Integer> translatedInference = Inferences.create("Integer Translation", translatedConclusion, translatedPremises);
			currentInferences.add(translatedInference);
		}

		@Override
		public Collection<Inference<Integer>> getInferences(Object conclusion) {
			return Optional.ofNullable(inferences_.get(conclusion)).orElse(Collections.<Inference<Integer>>emptyList());
		}

	}

	public Proof<Inference<? extends Integer>> getTranslatedProofGetInferences(IdProvider<A, I> idProvider) {
		return new TranslatedProof2(idProvider);
	}
	
	class TranslatedProof2 implements Proof<Inference<? extends Integer>> {

		protected IdProvider<A, I> idProvider_;

		TranslatedProof2(IdProvider<A, I> idProvider) {
			this.idProvider_ = idProvider;
		}

		@Override
		public Collection<? extends Inference<? extends Integer>> getInferences(Object conclusion) {
			if(idProvider_.getAxiomIds().contains(conclusion)) {
				Inference<Integer> inference = Inferences.create("axiom Translation", (Integer) conclusion, new ArrayList<Integer>());
				return Arrays.asList(inference);
			}
			return Optional.ofNullable(inferences_.get(conclusion)).orElse(Collections.<Inference<Integer>>emptyList());
		}
	}
}
