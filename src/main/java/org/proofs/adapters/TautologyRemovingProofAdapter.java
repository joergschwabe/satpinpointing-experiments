package org.proofs.adapters;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.DelegatingProof;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * A proof obtained from the given proof by removing all inferences that derive
 * "tautologies" from non-tautologies. A conclusion counts as a tautology if it
 * is derivable by inferences with the empty justification, i.e., the (only)
 * justification for this conclusion is the empty one. In the resulting proof,
 * tautologies are derived only from tautologies (by a single inference).
 * 
 * @author Yevgeny Kazakov
 *
 * @param <I>
 *            the type of inferences used in the proof
 * @param <A>
 *            the type of axioms used by the inferences
 * 
 */
class TautologyRemovingProofAdapter<I extends Inference<?>, A>
		extends DelegatingProof<I, Proof<I>> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(TautologyRemovingProofAdapter.class);

	private final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier_;

	/**
	 * the set of tautologies detected so far
	 */
	private final Set<Object> tautologies_ = new HashSet<Object>();

	/**
	 * tautologies to propagate
	 */
	private final Queue<Object> toDoTautologies_ = new LinkedList<Object>();

	/**
	 * index to retrieve inferences with empty justifications by their premises;
	 * only such inferences can derive new tautologies
	 */
	private final Multimap<Object, I> inferencesByPremises_ = ArrayListMultimap
			.create();

	/**
	 * a temporary queue for initialization of {@link #toDoTautologies_} and
	 * {@link #inferencesByPremises_}
	 */
	private final Queue<Object> toDoInit_ = new LinkedList<Object>();

	/**
	 * collects once they are inserted to {@link #toDoInit_} to avoid duplicates
	 */
	private final Set<Object> doneInit_ = new HashSet<Object>();

	TautologyRemovingProofAdapter(final Proof<I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier) {
		super(proof);
		this.justifier_ = justifier;
	}

	@Override
	public Collection<? extends I> getInferences(Object conclusion) {
		toDoInit(conclusion);
		initialize();
		process();
		Collection<? extends I> inferences = getDelegate()
				.getInferences(conclusion);
		if (isATautology(conclusion)) {
			// find one tautological inference
			for (final I inf : inferences) {
				if (!justifier_.getJustification(inf).isEmpty()) {
					continue;
				}
				boolean inferenceIsATautology = true;
				for (Object premise : inf.getPremises()) {
					if (!isATautology(premise)) {
						inferenceIsATautology = false;
						break;
					}
				}
				if (!inferenceIsATautology) {
					continue;
				}
				// else
				return Collections.singleton(inf);
			}

			return Collections.emptyList();
		}
		return inferences;
	}

	private void toDoInit(Object conclusion) {
		if (doneInit_.add(conclusion)) {
			toDoInit_.add(conclusion);
		}
	}

	private void toDoTautology(Object conclusion) {
		if (tautologies_.add(conclusion)) {
			toDoTautologies_.add(conclusion);
			LOGGER_.trace("new tautology {}", conclusion);
		}
	}

	private boolean isATautology(Object conclusion) {
		return tautologies_.contains(conclusion);
	}

	/**
	 * initializes {@link #toDoTautologies_}
	 */
	private void initialize() {
		Object conclusion;
		while ((conclusion = toDoInit_.poll()) != null) {
			for (final I inf : getDelegate().getInferences(conclusion)) {
				LOGGER_.trace("recursing by {}", inf);
				boolean noJustification = justifier_.getJustification(inf)
						.isEmpty();
				boolean conclusionIsATautology = noJustification;
				for (Object premise : inf.getPremises()) {
					toDoInit(premise);
					if (noJustification) {
						inferencesByPremises_.put(premise, inf);
						conclusionIsATautology &= isATautology(premise);
					}
				}
				if (conclusionIsATautology) {
					toDoTautology(inf.getConclusion());
				}
			}
		}

	}

	private void process() {
		Object tautology;
		while ((tautology = toDoTautologies_.poll()) != null) {
			for (final I inf : inferencesByPremises_.get(tautology)) {
				boolean conclusionIsATautology = true;
				for (Object premise : inf.getPremises()) {
					if (!isATautology(premise)) {
						conclusionIsATautology = false;
						break;
					}
				}
				if (conclusionIsATautology) {
					toDoTautology(inf.getConclusion());
				}
			}
		}
	}

}
