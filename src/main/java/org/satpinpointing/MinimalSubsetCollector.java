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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator.Factory;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.liveontologies.puli.pinpointing.PriorityComparators;

public class MinimalSubsetCollector<C, I extends Inference<? extends C>, A> {

	private final Factory<C, A> enumeratorFactory_;

	private final CancellableMonitor monitor_ = new CancellableMonitor();

	public MinimalSubsetCollector(
			final MinimalSubsetsFromProofs.Factory<C, I, A> factory,
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier) {
		this.enumeratorFactory_ = factory.create(proof, justifier, monitor_);
	}

	public Collection<? extends Set<A>> collect(final C query,
			final int sizeLimit) {
		final int limit = sizeLimit <= 0 ? Integer.MAX_VALUE : sizeLimit;

		final List<Set<A>> sets = new ArrayList<>();

		final MinimalSubsetEnumerator.Listener<A> listener = new MinimalSubsetEnumerator.Listener<A>() {

			@Override
			public void newMinimalSubset(final Set<A> set) {
				if (set.size() <= limit) {
					sets.add(set);
				} else {
					monitor_.cancel();
				}
			}

		};

		enumeratorFactory_.newEnumerator(query).enumerate(listener,
				PriorityComparators.<A> cardinality());

		return sets;
	}

	public Collection<? extends Set<A>> collect(final C query) {
		return collect(query, Integer.MAX_VALUE);
	}

	private static class CancellableMonitor implements InterruptMonitor {

		private volatile boolean cancelled_ = false;

		@Override
		public boolean isInterrupted() {
			return cancelled_;
		}

		public void cancel() {
			cancelled_ = true;
		}

	}

}
