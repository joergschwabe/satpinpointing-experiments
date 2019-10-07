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
import java.util.Collection;
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
	 * contains all blocked axioms
	 */
	private Set<Object> blocked = new HashSet<>();

	/**
	 * contains a map for used for unblocking
	 */
	private Map<Object, Set<Object>> blockedMap_;

	/**
	 * contains all visited conclusions
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
	
	/**
	 * contains all conclusionIds of the proof
	 */
	private Set<Integer> conclusionSet;

	/**
	 * contains all computed cycles
	 */
	private Set<Collection<I>> cycles_ = new HashSet<>();

	protected CycleComputator(final Proof<? extends I> proof, Set<Integer> axiomSet, Set<Integer> conclusionSet) {
		this.proof = proof;
		this.axiomSet = axiomSet;
		this.conclusionSet = conclusionSet;
	}

	public Set<Collection<I>> getCycles(Object conclusion) throws IOException {
		blockedMap_ =  new HashMap<>(conclusionSet.size());
		for (Iterator<Integer> iterator = conclusionSet.iterator(); iterator.hasNext();) {
			Object concl = iterator.next();
			inferenceStack_.push(proof.getInferences(concl).iterator());
			blocked.clear();
			blockedMap_.clear();
			findCycles(concl, concl);
			visited_.add(concl);			
		}
		return cycles_;
	}

	private boolean findCycles(Object start, Object current) throws IOException {
		conclusionStack_.push(current);
		blocked.add(current);
		boolean foundCycle = false;
		for (Iterator<? extends I> iterator = inferenceStack_.peek(); iterator.hasNext();) {
			I nextInf = iterator.next();

			Set<Object> premises = getPremises(nextInf);

			for (Object premise : premises) {

				// check if the premise was already visited
				if(visited_.contains(premise)) {
					continue;
				}

				inferencePath_.push(nextInf);

				if(premise == start) {
					cycles_.add(new HashSet<I>(inferencePath_));
					foundCycle = true;
				} else {	
					if (!blocked.contains(premise)) {
						inferenceStack_.push(proof.getInferences(premise).iterator());
						boolean gotCycle = findCycles(start, premise);
						foundCycle = gotCycle || foundCycle;
					}
				}
				inferencePath_.pop();
			}
		}

		inferenceStack_.pop();
		conclusionStack_.pop();

		if(foundCycle) {
			unblock(current);
		} else {
			addToBlockedMap(current);
		}
		
		return foundCycle;
	}

	private void addToBlockedMap(Object current) {
		for (Iterator<? extends I> iterator = proof.getInferences(current).iterator(); iterator.hasNext();) {
			I nextInf = iterator.next();
			for (Object premise : getPremises(nextInf)) {
				Set<Object> blockedSet = blockedMap_.get(premise);
				if(blockedSet == null) {
					Set<Object> newSet = new HashSet<Object>();
					newSet.add(current);
					blockedMap_.put(premise, newSet);
				} else {
					blockedSet.add(premise);
				}
			}
		}
	}

	private void unblock(Object axiom) {
		blocked.remove(axiom);
		Set<Object> blockedSet = blockedMap_.get(axiom);
		if(blockedSet != null) {
			for (Object premise : blockedSet) {
				if(blocked.contains(premise)) {
					unblock(premise);
				}
			}
		}
		blockedMap_.remove(axiom);
	}

	private Set<Object> getPremises(I inf) {
		Set<Object> premises = new HashSet<Object>();
		for(Object elem : inf.getPremises()) {
			if(!axiomSet.contains(elem)) {
				premises.add(elem);
			}
		}
		return premises;
	}

}
