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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A convenience class for collecting basic information about the inferences
 * that are used for deriving a given conclusion within a given proof.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <I>
 *            type of inferences used in the proof
 * @param <A>
 *            type of axioms used in justifications
 */
public class ProofInfoForConclusion<I extends Inference<?>, A> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ProofInfoForConclusion.class);

	private final Set<Object> usedConclusions_ = new HashSet<>();
	private final Set<A> usedAxioms_ = new HashSet<>();
	private final List<I> usedInferences_ = new ArrayList<>();

	private final Queue<Object> toDo_ = new LinkedList<>();

	private final Proof<I> proof_;
	private final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier_;

	ProofInfoForConclusion(final Proof<I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			Object conclusion) {
		this.proof_ = proof;
		this.justifier_ = justifier;
		toDo(conclusion);
		process();
	}

	/**
	 * @return the inferences used in the proofs for the given conclusion
	 */
	public List<I> getUsedInferences() {
		return usedInferences_;
	}

	/**
	 * @return the conclusions used in the proofs for the given conclusion
	 */
	public Set<?> getUsedConclusions() {
		return usedConclusions_;
	}

	/**
	 * @return the axioms used in justifications of inferences used in the
	 *         proofs for the given conclusion
	 */
	public Set<A> getUsedAxioms() {
		return usedAxioms_;
	}

	public void log() {
		LOGGER_.debug("{} used inferences", usedInferences_.size());
		LOGGER_.debug("{} used conclusions", usedConclusions_.size());
		LOGGER_.debug("{} used axioms", usedAxioms_.size());
	}

	private void toDo(Object conclusion) {
		if (usedConclusions_.add(conclusion)) {
			toDo_.add(conclusion);
		}
	}

	private void process() {
		Object next;
		while ((next = toDo_.poll()) != null) {
			for (final I inf : proof_.getInferences(next)) {
				usedInferences_.add(inf);
				usedAxioms_.addAll(justifier_.getJustification(inf));
				for (Object premise : inf.getPremises()) {
					toDo(premise);
				}
			}
		}
	}

}
