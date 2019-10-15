
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
import org.logicng.io.parsers.ParserException;
import org.sat4j.specs.ContradictionException;

/**
 * @author Jörg Schwabe
 *
 * @param <I> the type of the inferences returned by the proof
 *
 * @param <A> the type of the axioms
 */
public class CycleComputator {

	/**
	 * the set of inferences from which the proofs are formed
	 */
	private final Proof<Inference<? extends Integer>> proof;

	/**
	 * the current positions of iterators over inferences for conclusions
	 */
	private final Deque<Iterator<? extends Inference<? extends Integer>>> inferenceStack_ = new LinkedList<Iterator<? extends Inference<? extends Integer>>>();

	/**
	 * contains all blocked axioms
	 */
	private Set<Object> blocked = new HashSet<Object>();

	/**
	 * contains a map for used for unblocking
	 */
	private Map<Object, Set<Object>> blockedMap_ = new HashMap<Object, Set<Object>>();

	/**
	 * contains a map for used for unblocking
	 */
	private Map<Inference<? extends Integer>, Set<Object>> premisesMap_ = new HashMap<Inference<? extends Integer>, Set<Object>>();

	/**
	 * contains all visited conclusions
	 */
	private final Set<Object> visited_ = new HashSet<Object>();

	/**
	 * the inferences of considered path
	 */
	private final Set<Inference<? extends Integer>> inferencePath_ = new HashSet<Inference<? extends Integer>>();

	private List<Integer> consideredSCC;

	private Set<Set<Inference<? extends Integer>>> cycles = new HashSet<Set<Inference<? extends Integer>>>();

	public CycleComputator(final Proof<Inference<? extends Integer>> proof) {
		this.proof = proof;
	}

	public Set<Set<Inference<? extends Integer>>> addAllCycles(List<Integer> consideredSCC) throws IOException, ParserException, ContradictionException {
		this.consideredSCC = consideredSCC;
		for (Object concl : consideredSCC) {
			blocked.clear();
			blockedMap_.clear();
			visited_.clear();

			findCycles(concl, concl);

			visited_.add(concl);
		}
		return cycles;
	}

	private boolean findCycles(Object start, Object current)
			throws IOException, ParserException, ContradictionException {
		blocked.add(current);
		inferenceStack_.push(proof.getInferences(current).iterator());
		boolean foundCycle = false;
		for (Iterator<? extends Inference<? extends Integer>> iterator = inferenceStack_.peek(); iterator.hasNext();) {
			Inference<? extends Integer> nextInf = iterator.next();
			inferencePath_.add(nextInf);

			for (Object premise : getPremises(nextInf)) {

				// check if the premise was already visited
				if (visited_.contains(premise)) {
					continue;
				}

				if (premise == start) {
					Set<Inference<? extends Integer>> cycle = new HashSet<Inference<? extends Integer>>();
					cycle.addAll(inferencePath_);
					cycles.add(cycle);
					foundCycle = true;
				} else {
					if (!blocked.contains(premise)) {
						boolean gotCycle = findCycles(start, premise);
						foundCycle = gotCycle || foundCycle;
					}
				}
			}
			inferencePath_.remove(nextInf);
		}

		inferenceStack_.pop();

		if (foundCycle) {
			unblock(current);
		} else {
			addToBlockedMap(current);
		}

		return foundCycle;
	}

	private void addToBlockedMap(Object current) {
		for (Inference<? extends Integer> nextInf : proof.getInferences(current)) {
			for (Object premise : getPremises(nextInf)) {
				Set<Object> blockedSet = blockedMap_.get(premise);
				if (blockedSet == null) {
					blockedSet = new HashSet<Object>();
					blockedMap_.put(premise, blockedSet);
				}
				blockedSet.add(current);
			}
		}
	}

	private void unblock(Object axiom) {
		blocked.remove(axiom);
		Set<Object> blockedSet = blockedMap_.get(axiom);
		if (blockedSet != null) {
			for (Object premise : blockedSet) {
				if (blocked.contains(premise)) {
					unblock(premise);
				}
			}
			blockedMap_.remove(axiom);
		}
	}

	private Set<Object> getPremises(Inference<? extends Integer> nextInf) {
		Set<Object> premises = premisesMap_.get(nextInf);
		if (premises != null) {
			return premises;
		}
		premises = new HashSet<Object>();
		for (Object premise : nextInf.getPremises()) {
			if (consideredSCC.contains(premise)) {
				premises.add(premise);
			}
		}
		premisesMap_.put(nextInf, premises);
		return premises;
	}

}