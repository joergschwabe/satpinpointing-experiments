/*-
 * #%L
 * Proof Utility Library
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Live Ontologies Project
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
package com.github.joergschwabe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;

/**
 * @author Jörg Schwabe
 *
 * @param <I>
 *            the type of the inferences returned by the proof
 */
public class CycleComputator<I extends Inference<?>> {

	/**
	 * the set of inferences from which the proofs are formed
	 */
	private final Proof<? extends I> proof;

	/**
	 * the current positions of iterators over inferences for conclusions
	 */
	private final Deque<Iterator<? extends I>> inferenceStack_ = new LinkedList<Iterator<? extends I>>();

	/**
	 * the current positions of iterators over conclusions for inferences
	 */
	private final Deque<Object> conclusionStack_ = new LinkedList<>();

	/**
	 * accumulates the printed conclusions to avoid repetitions
	 */
	private final Set<Object> visited_ = new HashSet<Object>();

	/**
	 * the inferences of considered path
	 */
	private final Deque<I> inferencePath_ = new LinkedList<I>();

	/**
	 * contains all axioms of the ontology
	 */
	private final Set<Integer> axiomSet;

	private final Map<Object, HashSet<Object>> blockedMap_ = new HashMap<>();

	/**
	 * contains all computed cycles
	 */
	private final Set<List<I>> cycles_ = new HashSet<>();

	private List<Object> conclusions_ = new ArrayList<>();

	private Set<Object> blocked = new HashSet<>();

	protected CycleComputator(final Proof<? extends I> proof, Set<Integer> axiomSet) {
		this.proof = proof;
		this.axiomSet = axiomSet;
	}

	public Set<List<I>> getCycles(Object conclusion) throws IOException {
		conclusions_.add(conclusion);
		int size = 0;
		while(size < conclusions_.size()) {
			Object concl = conclusions_.get(size);
			inferenceStack_.push(proof.getInferences(concl).iterator());
			blocked.clear();
			blockedMap_.clear();
			process(concl, concl);
			visited_.add(concl);
			size++;
		}
		return cycles_;
	}

	private boolean process(Object start, Object current) throws IOException {
		if(visited_.contains(current)) {
			return false;
		}
		conclusionStack_.push(current);
		blocked.add(current);
		boolean foundCycle = false;
		for (Iterator<? extends I> iterator = inferenceStack_.peek(); iterator.hasNext();) {
			I nextInf = iterator.next();
			inferencePath_.push(nextInf);
			for (Object premise : getPremises(nextInf)) {
				if(premise == start) {
					cycles_.add(new ArrayList<I>(inferencePath_));
					foundCycle = true;
				} else if (!blocked.contains(premise)) {
					if(!conclusions_.contains(premise)) {
						conclusions_.add(premise);						
					}
					inferenceStack_.push(proof.getInferences(premise).iterator());
					boolean gotCycle = process(start, premise);
					foundCycle = gotCycle || foundCycle;
				}
			}
			inferencePath_.pop();
		}

		inferenceStack_.pop();
		conclusionStack_.pop();
		
		if(foundCycle) {
			unblock(current);
		} else {
			for (Iterator<? extends I> iterator = proof.getInferences(current).iterator(); iterator.hasNext();) {
				I nextInf = iterator.next();
				for (Object premise : getPremises(nextInf)) {
					Set<Object> bSet = blockedMap_.get(premise);
					if(bSet == null) {
						HashSet<Object> set = new HashSet<Object>();
						set.add(current);
						blockedMap_.put(premise, set);
					} else {
						bSet.add(premise);
					}
				}
			}
		}
		
		return foundCycle;
	}

	private void unblock(Object current) {
		blocked.remove(current);
		HashSet<Object> blockedSet = blockedMap_.get(current);
		if(blockedSet != null) {
			for (Object premise : blockedSet) {
				if(blocked.contains(premise)) {
					unblock(premise);
				}
			}
		}
		blockedMap_.remove(current);
	}

	private List<?> getPremises(I inf) {
		List<Object> premises = new ArrayList<Object>();
		for(Object elem : inf.getPremises()) {
			if(!axiomSet.contains(elem)) {
				premises.add(elem);
			}
		}
		return premises;
	}

}
