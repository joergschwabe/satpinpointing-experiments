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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.AbstractMinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.liveontologies.puli.pinpointing.PriorityComparator;
import org.liveontologies.puli.statistics.NestedStats;
import org.liveontologies.puli.statistics.ResetStats;
import org.liveontologies.puli.statistics.Stat;
import org.semanticweb.elk.util.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

/**
 * Resets the whole context, does not cache anything!
 * 
 * @author Peter Skocovsky
 *
 * @param <C>
 * @param <I>
 * @param <A>
 */
public class MinPremisesBottomUp<C, I extends Inference<? extends C>, A>
		extends MinimalSubsetsFromProofs<C, I, A> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(MinPremisesBottomUp.class);

	private static final MinPremisesBottomUp.Factory<?, ?, ?> FACTORY_ = new Factory<>();

	/**
	 * a map from conclusions to their justifications
	 */
	private final ListMultimap<C, Justification<C, A>> justifications_ = ArrayListMultimap
			.create();

	/**
	 * a map from premises to inferences for relevant conclusions
	 */
	private final Multimap<C, I> inferencesByPremises_ = ArrayListMultimap
			.create();

	/**
	 * a map from premises and inferences for which they are used to their
	 * justifications
	 */
	private final Multimap<Pair<I, C>, Justification<C, A>> premiseJustifications_ = ArrayListMultimap
			.create();

	// Statistics

	private int countInferences_ = 0, countConclusions_ = 0,
			countJustificationCandidates_ = 0, countBlocked_ = 0;

	private MinPremisesBottomUp(final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			final InterruptMonitor monitor) {
		super(proof, justifier, monitor);
	}

	private void reset() {
		justifications_.clear();
		inferencesByPremises_.clear();
		premiseJustifications_.clear();
	}

	@Override
	public MinimalSubsetEnumerator<A> newEnumerator(final C query) {
		return new JustificationEnumerator(query);
	}

	@Stat
	public int nProcessedInferences() {
		return countInferences_;
	}

	@Stat
	public int nProcessedConclusions() {
		return countConclusions_;
	}

	@Stat
	public int nProcessedJustificationCandidates() {
		return countJustificationCandidates_;
	}

	@Stat
	public int nJustificationsOfAllConclusions() {
		return justifications_.size();
	}

	@Stat
	public int nBlockedJustifications() {
		return countBlocked_;
	}

	@Stat
	public int maxNJustificationsOfAConclusion() {
		int max = 0;
		for (final C conclusion : justifications_.keySet()) {
			final List<Justification<C, A>> justs = justifications_
					.get(conclusion);
			if (justs.size() > max) {
				max = justs.size();
			}
		}
		return max;
	}

	@ResetStats
	public void resetStats() {
		countInferences_ = 0;
		countConclusions_ = 0;
		countJustificationCandidates_ = 0;
		countBlocked_ = 0;
	}

	@NestedStats
	public static Class<?> getNestedStats() {
		return BloomSet.class;
	}

	@SuppressWarnings("unchecked")
	public static <C, I extends Inference<? extends C>, A> MinimalSubsetsFromProofs.Factory<C, I, A> getFactory() {
		return (Factory<C, I, A>) FACTORY_;
	}

	@SafeVarargs
	private static <C, A> Justification<C, A> createJustification(C conclusion,
			Collection<? extends A>... collections) {
		return new BloomSet<C, A>(conclusion, collections);
	}

	/**
	 * Performs computation of justifications for the given conclusion. Can
	 * compute and reuse justifications for other conclusions.
	 * 
	 * @author Yevgeny Kazakov
	 */
	private class JustificationEnumerator
			extends AbstractMinimalSubsetEnumerator<A> {

		private final C conclusion_;

		/**
		 * the conclusions that are relevant for the computation of the
		 * justifications, i.e., those from which the conclusion for which the
		 * justifications are computed can be derived
		 */
		private final Set<C> relevant_ = new HashSet<C>();

		/**
		 * temporary queue to compute {@link #relevant_}
		 */
		private final Queue<C> toInitialize_ = new LinkedList<C>();

		/**
		 * newly computed justifications to be propagated
		 */
		private PriorityQueue<JobFactory<C, A, ?>.Job> toDoJustifications_ = null;

		private Listener<A> listener_ = null;

		private JobFactory<C, A, ?> jobFactory_;

		/**
		 * the justifications will be returned here, they come in increasing
		 * size order
		 */
		private final List<? extends Set<A>> result_;

		JustificationEnumerator(C conclusion) {
			this.conclusion_ = conclusion;
			this.result_ = justifications_.get(conclusion);
		}

		@Override
		public void enumerate(final Listener<A> listener,
				final PriorityComparator<? super Set<A>, ?> priorityComparator) {
			Preconditions.checkNotNull(listener);
			this.listener_ = listener;
			if (priorityComparator == null) {
				enumerate(listener);
				return;
			}
			// else

			if (jobFactory_ != null && jobFactory_.priorityComparator_
					.equals(priorityComparator)) {
				// Visit already computed justifications. They should be in the
				// correct order.
				for (final Justification<C, A> just : justifications_
						.get(conclusion_)) {
					listener.newMinimalSubset(just);
				}
			} else {
				// Reset everything.
				this.jobFactory_ = JobFactory.create(priorityComparator);
				reset();
			}

			this.toDoJustifications_ = new PriorityQueue<JobFactory<C, A, ?>.Job>();

			toInitialize(conclusion_);
			initialize();
			process();

			this.listener_ = null;
		}

		/**
		 * traverse inferences to find relevant conclusions and create the queue
		 * of justifications to be propagated reusing previously computed
		 * justifications
		 */
		private void initialize() {

			C conclusion;
			while ((conclusion = toInitialize_.poll()) != null) {
				countConclusions_++;
				LOGGER_.trace("{}: computation of justifiations initialized",
						conclusion);
				boolean derived = false;
				for (final I inf : getInferences(conclusion)) {
					LOGGER_.trace("{}: new inference", inf);
					derived = true;
					countInferences_++;
					for (C premise : inf.getPremises()) {
						inferencesByPremises_.put(premise, inf);
						toInitialize(premise);
					}
					if (inf.getPremises().isEmpty()) {
						toDoJustifications_.add(jobFactory_.newJob(
								createJustification((C) inf.getConclusion(),
										getJustification(inf))));
						countJustificationCandidates_++;
					}
				}
				if (!derived) {
					LOGGER_.warn("{}: lemma not derived!", conclusion);
				}
			}

		}

		private void toInitialize(C conclusion) {
			if (!relevant_.contains(conclusion)) {
				countConclusions_++;
				relevant_.add(conclusion);
				toInitialize_.add(conclusion);
			}
		}

		/**
		 * process new justifications until the fixpoint
		 */
		private void process() {
			JobFactory<C, A, ?>.Job job;
			while ((job = toDoJustifications_.poll()) != null) {
				Justification<C, A> just = job.justification;
				if (isInterrupted()) {
					return;
				}

				C conclusion = just.getConclusion();
				if (!relevant_.contains(conclusion)) {
					countBlocked_++;
					LOGGER_.trace("blocked {}", just);
					continue;
				}
				List<Justification<C, A>> justs = justifications_
						.get(conclusion);
				if (!Utils.isMinimal(just, justs)) {
					continue;
				}
				if (!Utils.isMinimal(just, result_)) {
					countBlocked_++;
					LOGGER_.trace("blocked {}", just);
					continue;
				}
				// else
				justs.add(just);
				LOGGER_.trace("new {}", just);
				if (conclusion_.equals(conclusion) && listener_ != null) {
					listener_.newMinimalSubset(just);
				}

				if (just.isEmpty()) {

					// all justifications are computed,
					// the inferences are not needed anymore
					for (final I inf : getInferences(conclusion)) {
						for (C premise : inf.getPremises()) {
							inferencesByPremises_.remove(premise, inf);
							final Pair<I, C> key = Pair.create(inf, premise);
							premiseJustifications_.removeAll(key);
							premiseJustifications_.put(key,
									just.copyTo(premise));
						}
					}

				} else {

					/*
					 * minimize premise justifications of inferences deriving
					 * this conclusion
					 * 
					 * if the justification is empty and the inferences are
					 * removed, there is no need to minimize their premise
					 * justifications
					 */
					for (final I inf : getInferences(conclusion)) {
						final Justification<C, A> justLessInf = just
								.removeElements(getJustification(inf));
						for (final C premise : inf.getPremises()) {
							final Iterator<Justification<C, A>> premiseJustIt = premiseJustifications_
									.get(Pair.create(inf, premise)).iterator();
							while (premiseJustIt.hasNext()) {
								final Justification<C, A> premiseJust = premiseJustIt
										.next();
								if (premiseJust.containsAll(justLessInf)) {
									premiseJustIt.remove();
								}
							}
						}
					}

				}

				/*
				 * add the justification to premise justifications if inferences
				 * where this conclusion is the premise iff it is minimal w.r.t.
				 * justifications of the inference conclusion
				 */
				final Collection<I> inferences = inferencesByPremises_
						.get(conclusion);
				if (inferences == null || inferences.isEmpty()) {
					continue;
				}
				final List<I> infsToPropagate = new ArrayList<>(
						inferences.size());
				for (final I inf : inferences) {
					final Collection<Justification<C, A>> premiseJusts = premiseJustifications_
							.get(Pair.create(inf, conclusion));

					final Justification<C, A> justWithInf = just
							.addElements(getJustification(inf));
					if (Utils.isMinimal(justWithInf,
							justifications_.get(inf.getConclusion()))) {
						premiseJusts.add(just);
						infsToPropagate.add(inf);
					}

				}

				/*
				 * propagating justification over inferences
				 */
				for (final I inf : infsToPropagate) {

					Collection<Justification<C, A>> conclusionJusts = new ArrayList<Justification<C, A>>();
					Justification<C, A> conclusionJust = just
							.copyTo(inf.getConclusion())
							.addElements(getJustification(inf));
					conclusionJusts.add(conclusionJust);
					for (final C premise : inf.getPremises()) {
						if (!premise.equals(conclusion)) {
							conclusionJusts = Utils.join(conclusionJusts,
									premiseJustifications_
											.get(Pair.create(inf, premise)));
						}
					}

					for (Justification<C, A> conclJust : conclusionJusts) {
						toDoJustifications_.add(jobFactory_.newJob(conclJust));
						countJustificationCandidates_++;
					}

				}

			}

		}

	}

	private static class JobFactory<C, A, P> {

		private final PriorityComparator<? super Set<A>, P> priorityComparator_;

		private JobFactory(
				final PriorityComparator<? super Set<A>, P> priorityComparator) {
			this.priorityComparator_ = priorityComparator;
		}

		public static <C, A, P> JobFactory<C, A, P> create(
				final PriorityComparator<? super Set<A>, P> priorityComparator) {
			return new JobFactory<>(priorityComparator);
		}

		public Job newJob(final Justification<C, A> justification) {
			return new Job(priorityComparator_.getPriority(justification),
					justification);
		}

		private class Job implements Comparable<Job> {

			private final P priority_;

			public final Justification<C, A> justification;

			public Job(final P priority,
					final Justification<C, A> justification) {
				this.priority_ = priority;
				this.justification = justification;
			}

			@Override
			public int compareTo(final Job other) {
				final int result = priorityComparator_.compare(priority_,
						other.priority_);
				if (result != 0) {
					return result;
				}
				// else
				return Integer.compare(justification.getConclusion().hashCode(),
						other.justification.getConclusion().hashCode());
			}

		}

	}

	/**
	 * The factory for creating a {@link MinPremisesBottomUp}
	 * 
	 * @author Yevgeny Kazakov
	 *
	 * @param <C>
	 *            the type of conclusion and premises used by the inferences
	 * 
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
			return new MinPremisesBottomUp<>(proof, justifier, monitor);
		}

	}

}
