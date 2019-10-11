
package com.github.joergschwabe;

import java.io.IOException;
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
	 * contains the blocked axioms
	 */
	private final Deque<Iterator<Object>> blockedStack_ = new LinkedList<Iterator<Object>>();

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
		this.consideredSCC = consideredSCC;
		blockedMap_ = new HashMap<>(consideredSCC.size());
		premisesMap_ = new HashMap<>(consideredSCC.size());
		for (Object concl : consideredSCC) {
			inferenceStack_.push(proof.getInferences(concl).iterator());
			blocked.clear();
			blockedMap_.clear();
			findCycles(concl, concl);
			visited_.add(concl);			
		}
		return cycles_;
	}

	private boolean findCycles(Object start, Object current) throws IOException {
		blocked.add(current);
		boolean foundCycle = false;
		for (Iterator<? extends I> iterator = inferenceStack_.peek(); iterator.hasNext();) {
			I nextInf = iterator.next();

			for (Object premise : getPremises(nextInf)) {

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

		if(foundCycle) {
			unblock(current);
		} else {
			addToBlockedMap(current);
		}
		
		return foundCycle;
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