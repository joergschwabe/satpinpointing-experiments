
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
 * @param <I>
 *            the type of the inferences returned by the proof
 * @param <A>
 */
public class CycleComputator<I extends Inference<?>, A> {

	/**
	 * the set of inferences from which the proofs are formed
	 */
	private final Proof<Inference<? extends Integer>> proof;

	/**
	 * the current positions of iterators over inferences for conclusions
	 */
	private final Deque<Iterator<? extends Inference<? extends Integer>>> inferenceStack_ = new LinkedList<Iterator<? extends Inference<? extends Integer>>>();

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
	private Map<Inference<? extends Integer>, Set<Object>> premisesMap_;

	/**
	 * contains all visited conclusions
	 */
	private final Set<Object> visited_ = new HashSet<Object>();

	/**
	 * the inferences of considered path
	 */
	private final Set<Inference<? extends Integer>> inferencePath_ = new HashSet<>();

	private List<Integer> consideredSCC;

	private SatClauseHandler<I, A> satClauseHandler;

	public CycleComputator(Proof<Inference<? extends Integer>> proof,
			SatClauseHandler<I, A> satClauseHandler_) {
		this.proof = proof;
		this.satClauseHandler = satClauseHandler_;
	}

	public void addAllCycles(List<Integer> consideredSCC) throws IOException, ParserException, ContradictionException {
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
	}

	private boolean findCycles(Object start, Object current) throws IOException, ParserException, ContradictionException {
		blocked.add(current);
		boolean foundCycle = false;
		for (Iterator<? extends Inference<? extends Integer>> iterator = inferenceStack_.peek(); iterator.hasNext();) {
			Inference<? extends Integer> nextInf = iterator.next();
			inferencePath_.add(nextInf);

			for (Object premise : getPremises(nextInf)) {

				// check if the premise was already visited
				if(visited_.contains(premise)) {
					continue;
				}

				if(premise == start) {
					satClauseHandler.addCycleClause(inferencePath_);
					foundCycle = true;
				} else {	
					if (!blocked.contains(premise)) {
						inferenceStack_.push(proof.getInferences(premise).iterator());
						boolean gotCycle = findCycles(start, premise);
						foundCycle = gotCycle || foundCycle;
					}
				}
			}
			inferencePath_.remove(nextInf);
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
		for (Inference<? extends Integer> nextInf : proof.getInferences(current)) {
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

	private Set<Object> getPremises(Inference<? extends Integer> nextInf) {
		Set<Object> premises = premisesMap_.get(nextInf);
		if(premises != null) {
			return premises;
		}
		premises = new HashSet<Object>();
		for(Object premise : nextInf.getPremises()) {
			if(consideredSCC.contains(premise)) {
				premises.add(premise);
			}
		}
		premisesMap_.put(nextInf, premises);
		return premises;
	}

}