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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.Delegator;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.collections.BloomTrieCollection2;
import org.liveontologies.puli.collections.Collection2;
import org.liveontologies.puli.pinpointing.AbstractMinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.liveontologies.puli.pinpointing.PriorityComparator;
import org.liveontologies.puli.statistics.NestedStats;
import org.liveontologies.puli.statistics.ResetStats;
import org.liveontologies.puli.statistics.Stat;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

/**
 * 
 * @author Peter Skocovsky
 *
 * @param <C>
 *            the type of conclusion and premises used by the inferences
 * @param <I>
 *            the type of inferences used in the proof
 * @param <A>
 *            the type of axioms used by the inferences
 */
public class TopDownJustificationComputation<C, I extends Inference<? extends C>, A>
		extends MinimalSubsetsFromProofs<C, I, A> {

	private static final TopDownJustificationComputation.Factory<?, ?, ?> FACTORY_ = new Factory<>();

	@SuppressWarnings("unchecked")
	public static <C, I extends Inference<? extends C>, A> MinimalSubsetsFromProofs.Factory<C, I, A> getFactory() {
		return (Factory<C, I, A>) FACTORY_;
	}

	/**
	 * used to select the conclusion to expand
	 */
	private final Comparator<C> rank_;

	// Statistics
	private int producedJobsCount_ = 0, nonMinimalJobsCount_ = 0,
			expansionCount_ = 0, expandedInferencesCount_ = 0;

	private TopDownJustificationComputation(final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			final InterruptMonitor monitor) {
		super(proof, justifier, monitor);
		this.rank_ = new Comparator<C>() {
			@Override
			public int compare(final C first, final C second) {
				int result = Integer.compare(proof.getInferences(first).size(),
						proof.getInferences(second).size());
				if (result != 0) {
					return result;
				}
				// else
				return Integer.compare(first.hashCode(), second.hashCode());
			}
		};
	}

	@Override
	public MinimalSubsetEnumerator<A> newEnumerator(final C query) {
		return new JustificationEnumerator(query);
	}

	private class JustificationEnumerator
			extends AbstractMinimalSubsetEnumerator<A> {

		private final C conclusion_;

		/**
		 * newly computed jobs to be propagated
		 */
		private Queue<JobFactory<C, I, A, ?>.Job> toDoJobs_;

		/**
		 * Used to minimize the jobs
		 */
		private final Collection2<Set<Object>> minimalJobs_ = new BloomTrieCollection2<>();

		private final Collection2<Set<A>> minimalJustifications_ = new BloomTrieCollection2<>();

		private Listener<A> listener_ = null;

		private JobFactory<C, I, A, ?> jobFactory_;

		JustificationEnumerator(final C query) {
			this.conclusion_ = query;
		}

		@Override
		public void enumerate(final Listener<A> listener,
				final PriorityComparator<? super Set<A>, ?> priorityComparator) {
			Preconditions.checkNotNull(listener);
			if (priorityComparator == null) {
				enumerate(listener);
				return;
			}
			// else

			this.toDoJobs_ = new PriorityQueue<>();
			this.minimalJobs_.clear();
			this.minimalJustifications_.clear();
			this.jobFactory_ = JobFactory.create(priorityComparator);
			this.listener_ = listener;

			initialize(conclusion_);
			process();

			this.listener_ = null;
		}

		private void initialize(final C goal) {
			produce(jobFactory_.newJob(goal));
		}

		private void process() {
			JobFactory<C, I, A, ?>.Job job;
			while ((job = toDoJobs_.poll()) != null) {

				if (minimalJustifications_.isMinimal(job.justification_)
						&& minimalJobs_.isMinimal(job)) {
					minimalJobs_.add(job);
					if (job.premises_.isEmpty()) {
						minimalJustifications_.add(job.justification_);
						if (listener_ != null) {
							listener_.newMinimalSubset(job.justification_);
						}
					} else {
						expansionCount_++;
						for (final I inf : getInferences(
								chooseConclusion(job.premises_))) {
							expandedInferencesCount_++;
							final JobFactory<C, I, A, ?>.Job newJob = job
									.expand(inf, getInferenceJustifier());
							produce(newJob);
						}
					}
				} else {
					nonMinimalJobsCount_++;
				}

				if (isInterrupted()) {
					break;
				}

			}
		}

		private C chooseConclusion(final Collection<C> conclusions) {
			// select the conclusion with the smallest rank
			C result = null;
			for (C c : conclusions) {
				if (result == null || rank_.compare(c, result) < 0) {
					result = c;
				}
			}
			return result;
		}

		private void produce(final JobFactory<C, I, A, ?>.Job job) {
			producedJobsCount_++;
			toDoJobs_.add(job);
		}

	}

	@Stat
	public int nProducedJobs() {
		return producedJobsCount_;
	}

	@Stat
	public int nNonMinimalJobs() {
		return nonMinimalJobsCount_;
	}

	@Stat
	public double ratioInferencesPerExpansion() {
		return ((double) expandedInferencesCount_) / expansionCount_;
	}

	@ResetStats
	public void resetStats() {
		producedJobsCount_ = 0;
		nonMinimalJobsCount_ = 0;
		expansionCount_ = 0;
		expandedInferencesCount_ = 0;
	}

	@NestedStats
	public static Class<?> getNestedStats() {
		return BloomTrieCollection2.class;
	}

	private static class JobFactory<C, I extends Inference<? extends C>, A, P> {

		private final PriorityComparator<? super Set<A>, P> priorityComparator_;

		private JobFactory(
				final PriorityComparator<? super Set<A>, P> priorityComparator) {
			this.priorityComparator_ = priorityComparator;
		}

		public static <C, I extends Inference<? extends C>, A, P> JobFactory<C, I, A, P> create(
				final PriorityComparator<? super Set<A>, P> priorityComparator) {
			return new JobFactory<>(priorityComparator);
		}

		private Job newJob(final C goal) {
			return new Job(Collections.singleton(goal),
					Collections.<A> emptySet());
		}

		/**
		 * A set of premises and justification that can be used for deriving the
		 * goal conclusion.
		 * 
		 * @author Peter Skocovsky
		 * @author Yevgeny Kazakov
		 */
		private class Job extends AbstractSet<Object>
				implements Comparable<Job> {

			private final Set<C> premises_;
			private final Set<A> justification_;
			private final P priority_;

			private Job(final Set<C> premises, final Set<A> justification) {
				this.premises_ = premises;
				this.justification_ = justification;
				this.priority_ = priorityComparator_.getPriority(justification);
			}

			public Job expand(final I inference,
					final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier) {
				final Set<C> newPremises = new HashSet<>(premises_);
				newPremises.remove(inference.getConclusion());
				newPremises.addAll(inference.getPremises());
				Set<A> newJustification = justification_;
				Set<? extends A> toExpand = justifier
						.getJustification(inference);
				if (newJustification.containsAll(toExpand)) {
					newJustification = justification_;
				} else {
					newJustification = new HashSet<A>(justification_.size());
					newJustification.addAll(justification_);
					newJustification.addAll(toExpand);
				}
				return new Job(newPremises, newJustification);
			}

			@Override
			public Iterator<Object> iterator() {
				return Iterators.concat(premises_.iterator(),
						Iterators.transform(justification_.iterator(),
								new Function<A, Distinguisher>() {

									@Override
									public Distinguisher apply(final A axiom) {
										return new Distinguisher(axiom);
									}

								}));
			}

			@Override
			public boolean containsAll(final Collection<?> c) {
				if (c instanceof JobFactory.Job) {
					final JobFactory<?, ?, ?, ?>.Job other = (JobFactory<?, ?, ?, ?>.Job) c;
					return premises_.containsAll(other.premises_)
							&& justification_.containsAll(other.justification_);
				}
				// else
				return super.containsAll(c);
			}

			@Override
			public boolean contains(final Object o) {
				if (o instanceof JobFactory.Distinguisher) {
					@SuppressWarnings("unchecked")
					final Distinguisher distinguisher = (Distinguisher) o;
					return justification_.contains(distinguisher.getDelegate());
				} else {
					return premises_.contains(o);
				}
			}

			@Override
			public boolean remove(final Object o) {
				if (o instanceof JobFactory.Distinguisher) {
					@SuppressWarnings("unchecked")
					final Distinguisher distinguisher = (Distinguisher) o;
					return justification_.remove(distinguisher.getDelegate());
				} else {
					return premises_.remove(o);
				}
			}

			@Override
			public int size() {
				return premises_.size() + justification_.size();
			}

			@Override
			public int compareTo(final Job other) {
				final int result = priorityComparator_.compare(priority_,
						other.priority_);
				if (result != 0) {
					return result;
				}
				// else
				return Integer.compare(premises_.size(),
						other.premises_.size());
			}

		}

		private class Distinguisher extends Delegator<A> {

			public Distinguisher(final A delegate) {
				super(delegate);
			}

		}

	}

	/**
	 * The factory for creating a {@link BottomUpJustificationComputation}
	 * 
	 * @author Peter Skocovsky
	 *
	 * @param <C>
	 *            the type of conclusion and premises used by the inferences
	 * @param <I>
	 *            the type of inferences used in proofs
	 * @param <A>
	 *            the type of axioms used by the inferences
	 */
	private static class Factory<C, I extends Inference<? extends C>, A>
			implements MinimalSubsetsFromProofs.Factory<C, I, A> {

		@Override
		public MinimalSubsetEnumerator.Factory<C, A> create(
				final Proof<? extends I> proof,
				final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
				final InterruptMonitor monitor) {
			return new TopDownJustificationComputation<>(proof, justifier,
					monitor);
		}

	}

}
