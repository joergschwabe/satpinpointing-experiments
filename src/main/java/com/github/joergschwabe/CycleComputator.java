/*-
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
import java.util.Collections;
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
	private final Deque<Iterator<? extends Object>> conclusionStack_ = new LinkedList<Iterator<? extends Object>>();
	
	/**
	 * contains the blocked axioms
	 */
	private final Deque<Iterator<Object>> blockedStack_ = new LinkedList<Iterator<Object>>();
	
	/**
	 * contains the blocked axioms
	 */
	private final Deque<Object> premiseStack_ = new LinkedList<Object>();

	/**
	 * contains all blocked axioms
	 */
	private Set<Object> blocked = new HashSet<>();

	/**
	 * contains a map for used for unblocking
	 */
	private Map<Object, Set<Object>> blockedMap_;

	/**
	 * contains a map for used for unblocking
	 */
	private Map<I, Set<Object>> premisesMap_;

	/**
	 * contains a map for used for unblocking
	 */
	private Deque<Boolean> cycleStore = new LinkedList<Boolean>();

	/**
	 * contains all visited conclusions
	 */
	private final Set<Object> visited_ = new HashSet<Object>();

	/**
	 * the inferences of considered path
	 */
	private final Deque<I> inferencePath_ = new LinkedList<I>();

	/**
	 * contains all computed cycles
	 */
	private Set<Set<I>> cycles_ = new HashSet<>();

	private List<Integer> consideredSCC;

	protected CycleComputator(final Proof<? extends I> proof) {
		this.proof = proof;
	}

	public Set<Set<I>> getCycles(List<Integer> consideredSCC) throws IOException {
		Collections.sort(consideredSCC);
		this.consideredSCC = consideredSCC;
		blockedMap_ = new HashMap<>(consideredSCC.size());
		premisesMap_ = new HashMap<>(consideredSCC.size());
		for (Object concl : consideredSCC) {
			blocked.clear();
			blockedMap_.clear();
			findCycles(concl);
			visited_.add(concl);			
		}
		return cycles_;
	}

	private void findCycles(Object start){
		inferenceStack_.push(proof.getInferences(start).iterator());
		blocked.add(start);
		cycleStore.push(false);
		boolean foundCycle = false;

		for(;;) {
			Iterator<? extends I> infIter = inferenceStack_.peek();
			if(infIter == null) {
				return;
			}

			if(infIter.hasNext()) {
				I nextInf = infIter.next();
				conclusionStack_.push(getPremises(nextInf).iterator());
				inferencePath_.push(nextInf);
			} else {
				inferenceStack_.pop();

				if(premiseStack_.size()>0) {
					Object premise = premiseStack_.pop();
					if(foundCycle) {
						unblock(premise);
					} else {
						addToBlockedMap(premise);
					}
				}
				foundCycle = cycleStore.pop() || foundCycle;
			}

			Iterator<? extends Object> conclIter = conclusionStack_.peek();
			if(conclIter == null) {
				return;
			}

			for(;;) {
				if(conclIter.hasNext()) {
					Object premise = conclIter.next();
					
					// check if the premise was already visited
					if(visited_.contains(premise)) {
						continue;
					}
					
					if(premise == start) {
						cycles_.add(new HashSet<I>(inferencePath_));
						foundCycle=true;
						continue;
					}
					if (!blocked.contains(premise)) {
						premiseStack_.push(premise);
						blocked.add(premise);
						cycleStore.push(foundCycle);
						foundCycle = false;
						inferenceStack_.push(proof.getInferences(premise).iterator());
						break;
					}
					continue;
				}
				if(conclusionStack_.size()>0) {
					conclusionStack_.pop();
				}
				if(inferencePath_.size()>0) {
					inferencePath_.pop();
				}
				break;
			}
		}
	}

	private void addToBlockedMap(Object current) {
		for (I nextInf : proof.getInferences(current)) {
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
		if(blockProcess(axiom)) {
			return;
		}

		for(;;) {
			Iterator<Object> blockIter = blockedStack_.peek();
			if(blockIter == null) {
				return;
			}
			for(;;) {
				if(blockIter.hasNext()) {
					Object premise = blockIter.next();
					if(!blocked.contains(premise)) {
						continue;
					}
					if(blockProcess(premise)) {
						continue;
					}
					break;
				}
				blockedStack_.pop();
				break;
			}
		}
	}

	private boolean blockProcess(Object axiom) {
		blocked.remove(axiom);
		Set<Object> blockedAxiomSet = blockedMap_.get(axiom);
		blockedMap_.remove(axiom);
		if(blockedAxiomSet == null) {
			return true;
		}
		blockedStack_.push(blockedAxiomSet.iterator());
		return false;
	}

	private Set<Object> getPremises(I inf) {
		Set<Object> premises = premisesMap_.get(inf);
		if(premises != null) {
			return premises;
		}
		premises = new HashSet<Object>();
		for(Object premise : inf.getPremises()) {
			if(consideredSCC.contains(premise)) {
				premises.add(premise);
			}
		}
		premisesMap_.put(inf, premises);
		return premises;
	}

}

