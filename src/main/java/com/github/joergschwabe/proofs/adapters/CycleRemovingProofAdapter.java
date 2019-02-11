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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.BaseProof;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proof obtained from the given proof by eliminating cyclic inferences of
 * length 1 and 2. An inference is cyclic of length 1 if one of the premises of
 * the inferences is the same as its conclusion. An inference is cyclic of
 * length 2 if there is a premise such that all inferences in the original proof
 * that produce this premise use the conclusion of the inference as one of the
 * premises.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <I>
 *            The type of the inferences.
 * @param <A>
 *            the type of axioms used by the inferences
 * 
 */
class CycleRemovingProofAdapter<I extends Inference<?>> extends BaseProof<I> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(CycleRemovingProofAdapter.class);

	/**
	 * inferences that are filtered
	 */
	private final Proof<I> originalInferences_;

	/**
	 * conclusions for which to process inferences recursively
	 */
	private final Queue<Object> toDoConclusions_ = new LinkedList<Object>();

	/**
	 * caches the processed conclusions and {@link #toDoConclusions_}
	 */
	private final Set<Object> doneConclusions_ = new HashSet<Object>();

	/**
	 * inferences indexed by a premise that cannot be (currently) obtained as a
	 * conclusion of an inference in this inference that does not use the
	 * conclusion of the inference as one of the premises
	 */
	private final Map<Object, List<I>> blocked_ = new HashMap<Object, List<I>>();

	/**
	 * the inferences that are (no longer) blocked as a result of adding other
	 * (unblocked) inferences to this proof
	 */
	private final Queue<I> unblocked_ = new LinkedList<I>();

	CycleRemovingProofAdapter(final Proof<I> originalInferences) {
		this.originalInferences_ = originalInferences;
	}

	@Override
	public Collection<? extends I> getInferences(Object conclusion) {
		toDo(conclusion);
		process();
		return super.getInferences(conclusion);
	}

	private void toDo(Object conclusion) {
		if (doneConclusions_.add(conclusion)) {
			toDoConclusions_.add(conclusion);
		}
	}

	private void process() {
		Object conclusion;
		while ((conclusion = toDoConclusions_.poll()) != null) {
			for (final I inf : originalInferences_.getInferences(conclusion)) {
				process(inf);
			}
		}
	}

	private void process(I next) {
		for (Object premise : next.getPremises()) {
			toDo(premise);
		}
		checkBlocked(next);
		while ((next = unblocked_.poll()) != null) {
			produce(next);
			final List<I> blockedByNext = blocked_.remove(next.getConclusion());
			if (blockedByNext == null) {
				continue;
			}
			// else
			for (final I inf : blockedByNext) {
				checkBlocked(inf);
			}
		}
	}

	private void checkBlocked(final I next) {
		if (next.getPremises().contains(next.getConclusion())) {
			LOGGER_.trace("{}: permanently blocked", next);
			return;
		}
		Object blockedPremise = getBlockedPremise(next);
		if (blockedPremise == null) {
			LOGGER_.trace("{}: unblocked", next);
			unblocked_.add(next);
		} else {
			LOGGER_.trace("{}: blocked by {}", next, blockedPremise);
			block(next, blockedPremise);
		}
	}

	private void block(final I inference, Object conclusion) {
		List<I> blockedForConclusion = blocked_.get(conclusion);
		if (blockedForConclusion == null) {
			blockedForConclusion = new ArrayList<I>();
			blocked_.put(conclusion, blockedForConclusion);
		}
		blockedForConclusion.add(inference);
	}

	/**
	 * @param next
	 * @return an inference premise that cannot be derived by other inferences
	 *         that do not use the conclusion of this inference as one of the
	 *         premises; returns {@code null} if such a premise does not exist
	 */
	private Object getBlockedPremise(final I next) {
		Object conclusion = next.getConclusion();
		for (Object premise : next.getPremises()) {
			if (!derivableWithoutPremise(premise, conclusion)) {
				return premise;
			}
		}
		// else
		return null;
	}

	/**
	 * @param conclusion
	 * @param nonpremise
	 * @return {@code true} if there exists an inference in {@link #output_}
	 *         with the given conclusion which does not use the given premise
	 */
	private boolean derivableWithoutPremise(Object conclusion,
			Object nonpremise) {
		boolean derivable = false;
		for (final I inf : getInferences(conclusion)) {
			if (derivable |= !inf.getPremises().contains(nonpremise)) {
				return true;
			}
		}
		// else
		return false;
	}

}
