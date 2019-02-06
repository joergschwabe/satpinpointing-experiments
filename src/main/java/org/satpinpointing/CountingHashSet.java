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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set with optimized implementation of inclusion of a collection, in case the
 * collection is of the same type.
 * 
 * @see Set#containsAll(Collection)
 * 
 * @author Yevgeny Kazakov
 *
 * @param <E>
 *            the type of elements maintained by this set
 */
class CountingHashSet<E> extends HashSet<E> {

	private static final long serialVersionUID = -2805422564617676450L;

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(CountingHashSet.class);

	private static final boolean COLLECT_STATS_ = true;

	private static int STATS_CONTAINS_ALL_COUNT_ = 0,
			STATS_CONTAINS_ALL_FILTERED_ = 0;

	private static final short SHIFT_ = 3;

	private static final int MASK_ = (1 << SHIFT_) - 1;

	private final int[] counts_ = new int[MASK_ + 1];

	public CountingHashSet() {
		super();
	}

	public CountingHashSet(int initialCapacity) {
		super(initialCapacity);
	}

	public CountingHashSet(Collection<? extends E> c) {
		this(c.size());
		addAll(c);
	}

	@Override
	public boolean add(E e) {
		boolean success = super.add(e);
		if (success) {
			counts_[e.hashCode() & MASK_] += 1;
		}
		return success;
	}

	@Override
	public boolean remove(Object o) {
		boolean success = super.remove(o);
		if (success) {
			counts_[o.hashCode() & MASK_] -= 1;
		}
		return success;
	}

	@Override
	public void clear() {
		super.clear();
		for (int i = 0; i < counts_.length; i++) {
			counts_[i] = 0;
		}
	}

	@Override
	public boolean containsAll(Collection<?> other) {
		if (COLLECT_STATS_) {
			STATS_CONTAINS_ALL_COUNT_++;
		}
		if (other instanceof CountingHashSet<?>) {
			int[] otherCounts = ((CountingHashSet<?>) other).counts_;
			for (int i = 0; i < counts_.length; i++) {
				if (counts_[i] < otherCounts[i]) {
					if (COLLECT_STATS_) {
						STATS_CONTAINS_ALL_FILTERED_++;
					}
					return false;
				}
			}
		}
		return super.containsAll(other);
	}

	public static void logStatistics() {
		if (LOGGER_.isDebugEnabled()) {
			float containsAllSuccessRatio = STATS_CONTAINS_ALL_COUNT_ == 0 ? 0f
					: (float) STATS_CONTAINS_ALL_FILTERED_
							/ STATS_CONTAINS_ALL_COUNT_;
			LOGGER_.debug(
					"{} out of {} ({}%) containsAll(Collection) tests filtered",
					STATS_CONTAINS_ALL_FILTERED_, STATS_CONTAINS_ALL_COUNT_,
					String.format("%.2f", containsAllSuccessRatio * 100));
		}
	}

	public static void resetStatistics() {
		STATS_CONTAINS_ALL_COUNT_ = 0;
		STATS_CONTAINS_ALL_FILTERED_ = 0;
	}

}
