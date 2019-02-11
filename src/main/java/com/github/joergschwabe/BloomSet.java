package com.github.joergschwabe;

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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

import org.liveontologies.puli.statistics.ResetStats;
import org.liveontologies.puli.statistics.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Sets;

/**
 * A set enhanced with a Bloom filter to quickly check set inclusion. The Bloom
 * filter uses just one hash function; containment of elements is not optimized.
 * As it is common with Bloom filters, removal of elements is not supported.
 * 
 * @see Set#contains(Object)
 * @see Set#containsAll(Collection)
 * 
 * @author Yevgeny Kazakov
 *
 * @param <C>
 *            the type of the conclusion for which the justification is computed
 * @param <A>
 *            the type of axioms in the justification
 */
public class BloomSet<C, A> extends ForwardingSet<A>
		implements Justification<C, A>, Comparable<BloomSet<C, A>> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(BloomSet.class);

	private static final boolean COLLECT_STATS_ = true;

	private static long STATS_CONTAINS_ALL_COUNT_ = 0,
			STATS_CONTAINS_ALL_POSITIVE_ = 0, STATS_CONTAINS_ALL_FILTERED_ = 0;

	@Stat
	public static long containsAllCount() {
		return STATS_CONTAINS_ALL_COUNT_;
	}

	@Stat
	public static long containsAllPositive() {
		return STATS_CONTAINS_ALL_POSITIVE_;
	}

	@Stat
	public static long containsAllFiltered() {
		return STATS_CONTAINS_ALL_FILTERED_;
	}

	private static final short SHIFT_ = 6; // 2^6 = 64 bits is good enough

	// = 11..1 SHIFT_ times
	private static final int MASK_ = (1 << SHIFT_) - 1;

	private final C conclusion_;

	private final Set<A> elements_;

	/**
	 * cache the size to avoid the unnecessary pointer access
	 */
	private final int size_;

	/**
	 * use this value for the second priority in the comparator (the first
	 * priority is size)
	 */
	private final int priority2_;

	/**
	 * filter for subset tests of SHIFT_ bits, each elements in the set sets one
	 * bit to 1
	 */
	private final long filter_;

	private BloomSet(C conclusion, Set<A> elements, int size, int priority2,
			long filter) {
		this.conclusion_ = conclusion;
		this.elements_ = elements;
		this.size_ = size;
		this.priority2_ = priority2;
		this.filter_ = filter;
	}

	@SafeVarargs
	public BloomSet(C conclusion, Collection<? extends A>... collections) {
		Builder<A> elementsBuilder = new ImmutableSet.Builder<A>();
		this.conclusion_ = conclusion;
		for (int i = 0; i < collections.length; i++) {
			elementsBuilder.addAll(collections[i]);
		}
		this.elements_ = elementsBuilder.build();
		this.size_ = elements_.size();
		// try to group justifications for the same conclusions together
		this.priority2_ = conclusion.hashCode();
		this.filter_ = buildFilter();
	}

	@Override
	public C getConclusion() {
		return conclusion_;
	}

	@Override
	public int size() {
		return size_;
	}

	@Override
	public Justification<C, A> copyTo(C conclusion) {
		return new BloomSet<C, A>(conclusion, elements_, size_,
				conclusion.hashCode(), filter_);
	}

	@Override
	public Justification<C, A> addElements(Set<? extends A> added) {
		if (containsAll(added)) {
			return this;
		}
		// else
		return new BloomSet<C, A>(conclusion_, Sets.union(this, added));
	}

	@Override
	public Justification<C, A> removeElements(Set<? extends A> removed) {
		if (Sets.intersection(this, removed).isEmpty()) {
			return this;
		}
		// else
		return new BloomSet<C, A>(conclusion_,
				Sets.difference(this, removed));
	}

	private long buildFilter() {
		long result = 0;
		for (A e : elements_) {
			result |= 1 << (e.hashCode() & MASK_);
		}
		return result;
	}
	
	@Override
	public boolean contains(Object object) {
		int mask = 1 << object.hashCode() & MASK_;
		if ((filter_ & mask) != mask) {
			return false;
		}
		// else
		return super.contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (COLLECT_STATS_) {
			STATS_CONTAINS_ALL_COUNT_++;
		}
		if (c instanceof BloomSet<?, ?>) {
			BloomSet<?, ?> other = (BloomSet<?, ?>) c;
			if ((filter_ & other.filter_) != other.filter_) {
				if (COLLECT_STATS_) {
					STATS_CONTAINS_ALL_FILTERED_++;
				}
				return false;
			}
		}
		if (super.containsAll(c)) {
			if (COLLECT_STATS_) {
				STATS_CONTAINS_ALL_POSITIVE_++;
			}
			return true;
		}
		// else
		return false;
	}

	@Override
	public String toString() {
		Object[] elements = toArray();
		Arrays.sort(elements, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				return String.valueOf(o1).compareTo(String.valueOf(o2));
			}
		});
		return getConclusion() + ": " + Arrays.toString(elements);
	}

	@Override
	public int compareTo(BloomSet<C, A> o) {
		// first prioritize smaller justifications
		int sizeDiff = size_ - o.size_;
		if (sizeDiff != 0) {
			return sizeDiff;
		}
		// this makes sure that justifications for
		// the same conclusions of the same size
		// are processed consequently, if possible
		return priority2_ - o.priority2_;
	}

	public static void logStatistics() {

		if (LOGGER_.isDebugEnabled()) {
			if (STATS_CONTAINS_ALL_COUNT_ != 0) {
				long negativeTests = STATS_CONTAINS_ALL_COUNT_
						- STATS_CONTAINS_ALL_POSITIVE_;
				if (negativeTests > 0) {
					float negativeSuccessRatio = (float) STATS_CONTAINS_ALL_FILTERED_
							/ negativeTests;
					LOGGER_.debug(
							"{} containsAll tests, {} negative, {} ({}%) filtered",
							STATS_CONTAINS_ALL_COUNT_, negativeTests,
							STATS_CONTAINS_ALL_FILTERED_,
							String.format("%.2f", negativeSuccessRatio * 100));
				} else {
					LOGGER_.debug("{} containsAll tests, all positive",
							STATS_CONTAINS_ALL_COUNT_);
				}
			}
		}
	}

	@ResetStats
	public static void resetStatistics() {
		STATS_CONTAINS_ALL_COUNT_ = 0;
		STATS_CONTAINS_ALL_FILTERED_ = 0;
		STATS_CONTAINS_ALL_POSITIVE_ = 0;
	}

	@Override
	protected Set<A> delegate() {
		return elements_;
	}

}
