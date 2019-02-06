package org.satpinpointing;

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


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Producer;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.Proofs;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * @author Joerg Schwabe
 *
 * @param <C>
 *            the type of conclusions used in inferences
 * @param <I>
 *            the type of inferences used in the proof
 * @param <A>
 *            the type of axioms used by the inferences
 */
public class IntegerProof<C, I extends Inference<? extends C>, A>
		implements Proof<Inference<? extends Integer>>, Producer<I> {

	private final BiMap<Object, Integer> conclusionIds_ = HashBiMap.create();
	private final BiMap<A, Integer> axiomIds_ = HashBiMap.create();
	private int nextId_ = 0;

	private final Map<Integer, Inference<? extends Integer>> inferences_ = new HashMap<>();
	private final int goal_;
	private InferenceJustifier<? super I, ? extends Set<? extends A>> justifier_;

	public IntegerProof(Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			C query) {
		this.goal_ = getConclusionId(query);
		Proofs.unfoldRecursively(proof, query, this);
		this.justifier_ = justifier;
	}

	private int getConclusionId(Object conclusion) {
		Integer result = conclusionIds_.get(conclusion);
		if (result == null) {
			result = nextId_++;
			conclusionIds_.put(conclusion, result);
		}
		return result;
	}

	private int getAxiomId(A axiom) {
		Integer result = axiomIds_.get(axiom);
		if (result == null) {
			result = nextId_++;
			axiomIds_.put(axiom, result);
		}
		return result;
	}

	@Override
	public Collection<? extends Inference<Integer>> getInferences(
			Object conclusion) {
		return null;
	}

	@Override
	public void produce(I inference) {
		// translation to sat
		C conclusion = inference.getConclusion();
		int conclusionId = getConclusionId(conclusion);
		for (C premise : inference.getPremises()) {
			int premiseId = getConclusionId(premise);

		}

	}

}
