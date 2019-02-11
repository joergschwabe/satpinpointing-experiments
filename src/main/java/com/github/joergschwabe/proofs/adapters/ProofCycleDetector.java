package com.github.joergschwabe.proofs.adapters;

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

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A convenience class for checking whether there are cyclic proofs for given
 * conclusions in the given proof. A proof is cyclic if some conclusion in the
 * proof can be derived from itself. Cycle detection is performed by descending
 * over inferences in depth-first manner.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <I>
 *            the type of inferences in the proof
 */
public class ProofCycleDetector<I extends Inference<?>> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ProofCycleDetector.class);

	/**
	 * inferences that are filtered
	 */
	private final Proof<I> originalProof_;

	/**
	 * verified conclusions with cyclic proofs will be collected here
	 */
	private final Set<Object> visitedCyclic_ = new HashSet<>();

	/**
	 * verified conclusions without cyclic proofs will be collected here
	 */
	private final Set<Object> visitedNonCyclic_ = new HashSet<>();

	/**
	 * conclusions on the current proof path, for cycle detection
	 */
	private final Set<Object> conclusionsOnPath_ = new HashSet<>();

	/**
	 * the current stack of conclusions together with the iterators over
	 * inferences
	 */
	private final Deque<ConclusionRecord<I>> conclusionStack_ = new LinkedList<>();

	/**
	 * the current stack of inferences together with the iterators over premises
	 */
	private final Deque<InferenceRecord<I>> inferenceStack_ = new LinkedList<>();

	ProofCycleDetector(final Proof<I> originalProof) {
		this.originalProof_ = originalProof;
	}

	/**
	 * @param conclusion
	 * @return {@code true} if there exists a cyclic proof for the conclusion in
	 *         this proof and {@code false} otherwise; a proof is cyclic if some
	 *         of the premises in the proof can be derived from itself using the
	 *         inferences in this proof
	 */
	public boolean hasCyclicProofFor(Object conclusion) {
		if (visitedNonCyclic_.contains(conclusion)) {
			return false;
		}
		push(conclusion);
		checkCycles();
		return (visitedCyclic_.contains(conclusion));
	}

	private boolean checkCycles() {
		for (;;) {
			ConclusionRecord<I> conclRec = conclusionStack_.peek();
			if (conclRec == null) {
				return false;
			}
			// else
			if (conclRec.inferenceIterator_.hasNext()) {
				push(conclRec.inferenceIterator_.next());
			} else {
				// no more inferences
				popNonCyclic();
			}
			InferenceRecord<I> infRec = inferenceStack_.peek();
			if (infRec == null) {
				return false;
			}
			for (;;) {
				if (!infRec.premiseIterator_.hasNext()) {
					// no more premises
					popInference();
					break;
				}
				// else
				Object premise = infRec.premiseIterator_.next();
				if (visitedNonCyclic_.contains(premise)) {
					// already checked
					LOGGER_.trace("{}: already visited", premise);
					continue;
				}
				// else
				if (!push(premise)) {
					LOGGER_.trace("{}: CYCLE!", premise);
					inferenceStack_.clear();
					while (!conclusionStack_.isEmpty()) {
						popCyclic();
						// mark all conclusions on the path as cyclic
					}
					return true;
				}
				// else
				break;
			}

		}

	}

	private boolean push(Object conclusion) {
		if (visitedCyclic_.contains(conclusion)
				|| !conclusionsOnPath_.add(conclusion)) {
			return false;
		}
		// else
		conclusionStack_
				.push(new ConclusionRecord<>(originalProof_, conclusion));
		LOGGER_.trace("{}: conclusion pushed", conclusion);
		return true;
	}

	private void push(final I inf) {
		inferenceStack_.push(new InferenceRecord<>(inf));
		LOGGER_.trace("{}: inference pushed", inf);
	}

	private ConclusionRecord<I> popNonCyclic() {
		ConclusionRecord<I> result = conclusionStack_.pop();
		LOGGER_.trace("{}: conclusion popped, non-cyclic", result.conclusion_);
		conclusionsOnPath_.remove(result.conclusion_);
		visitedNonCyclic_.add(result.conclusion_);
		return result;
	}

	private ConclusionRecord<I> popCyclic() {
		ConclusionRecord<I> result = conclusionStack_.pop();
		LOGGER_.trace("{}: conclusion popped, cyclic", result.conclusion_);
		conclusionsOnPath_.remove(result.conclusion_);
		visitedCyclic_.add(result.conclusion_);
		return result;
	}

	private InferenceRecord<I> popInference() {
		InferenceRecord<I> result = inferenceStack_.pop();
		LOGGER_.trace("{}: inference popped", result.inference_);
		return result;
	}

	private static class ConclusionRecord<I extends Inference<?>> {

		private final Object conclusion_;

		private final Iterator<? extends I> inferenceIterator_;

		ConclusionRecord(final Proof<I> proof, final Object conclusion) {
			this.conclusion_ = conclusion;
			this.inferenceIterator_ = proof.getInferences(conclusion)
					.iterator();
		}

	}

	private static class InferenceRecord<I extends Inference<?>> {

		I inference_;

		private final Iterator<?> premiseIterator_;

		InferenceRecord(I inference) {
			this.inference_ = inference;
			this.premiseIterator_ = inference.getPremises().iterator();
		}

	}

}
