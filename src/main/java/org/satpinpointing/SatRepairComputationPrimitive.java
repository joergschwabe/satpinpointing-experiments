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



import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.liveontologies.puli.pinpointing.PriorityComparator;

import com.google.common.base.Preconditions;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <C>
 *            the type of conclusions used in inferences
 * @param <I>
 *            the type of inferences used in the proof
 * @param <A>
 *            the type of axioms used by the inferences
 */
public class SatRepairComputationPrimitive<C, I extends Inference<? extends C>, A> extends MinimalSubsetsFromProofs<C, I, A> {

	private static final SatRepairComputationPrimitive.Factory<?, ?, ?> FACTORY_ = new Factory<Object, Inference<?>, Object>();

	@SuppressWarnings("unchecked")
	public static <C, I extends Inference<? extends C>, A> MinimalSubsetsFromProofs.Factory<C, I, A> getFactory() {
		return (Factory<C, I, A>) FACTORY_;
	}

	private SatRepairComputationPrimitive(final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier, final InterruptMonitor monitor) {
		super(proof, justifier, monitor);
	}

	public MinimalSubsetEnumerator<A> newEnumerator(final Object query) {
		return new Enumerator(query);
	}

	private class Enumerator implements MinimalSubsetEnumerator<A> {

		private final Object query;

		Enumerator(final Object query) {
			this.query = query;
		}

		public void enumerate(Listener<A> listener, PriorityComparator<? super Set<A>, ?> unused) {
			enumerate(listener);
		}

		public void enumerate(Listener<A> listener) {
			Preconditions.checkNotNull(listener);

			try {
				
				FindingAndAddRepairs_IntProof findingAndAddRepairs = new FindingAndAddRepairs_IntProof(listener, query);
				Map<I, Set<? extends A>> mapInfToAxioms= new HashMap<I, Set<? extends A>>();

				Queue<Object> conclusionsToTranslate = new LinkedList<Object>();
				Set<Object> conclusionsTranslated = new HashSet<Object>();
				conclusionsToTranslate.add(query);

				for (;;) {
					Object nextConclusion = conclusionsToTranslate.poll();

					if (nextConclusion == null) {
						break;
					}

					if (!conclusionsTranslated.add(nextConclusion)) {
						continue;
					}

					for (I inf : getProof().getInferences(nextConclusion)) {
						mapInfToAxioms.put(inf, getJustification(inf));
						conclusionsToTranslate.addAll(inf.getPremises());
					}
				}
				findingAndAddRepairs.addInfsToSat(mapInfToAxioms);
				findingAndAddRepairs.compute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * The factory.
	 * 
	 * @author Jörg Schwabe
	 *
	 * @param <C>
	 *            the type of conclusions used in inferences
	 * @param <I>
	 *            the type of inferences used in the proof
	 * @param <A>
	 *            the type of axioms used by the inferences
	 */
	private static class Factory<C, I extends Inference<? extends C>, A>
			implements MinimalSubsetsFromProofs.Factory<C, I, A> {

		public MinimalSubsetEnumerator.Factory<C, A> create(final Proof<? extends I> proof,
				final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
				final InterruptMonitor monitor) {
			return new SatRepairComputationPrimitive<C, I, A>(proof, justifier, monitor);
		}

	}

}
