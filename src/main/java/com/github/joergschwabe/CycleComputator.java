
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
	private Proof<Inference<? extends Integer>> proof;

	/**
	 * the current positions of iterators over inferences for conclusions
	 */
	private Deque<Iterator<? extends Inference<? extends Integer>>> inferenceStack;

	/**
	 * contains all blocked axioms
	 */
	private Set<Object> blocked;

	/**
	 * contains a map for used for unblocking
	 */
	private Map<Object, Set<Object>> blockedMap;

	/**
	 * contains a map for used for unblocking
	 */
	private Map<Inference<? extends Integer>, Set<Object>> premisesMap;

	/**
	 * contains all visited conclusions
	 */
	private Set<Object> visited;

	/**
	 * the inferences of considered path
	 */
	private Set<Inference<? extends Integer>> inferencePath;

	/**
	 * actual considered stronly connected component
	 */
	private List<Integer> consideredSCC;

	/**
	 * satClauseHandler for add cycle clauses
	 */
	private SatClauseHandler<I, A> satClauseHandler;

	public CycleComputator(Proof<Inference<? extends Integer>> proof,
			SatClauseHandler<I, A> satClauseHandler_) {
		this.proof = proof;
		this.satClauseHandler = satClauseHandler_;
	}

	public void addAllCycles(List<Integer> consideredSCC) throws IOException, ParserException, ContradictionException {
		initialize(consideredSCC);
		for (Object concl : consideredSCC) {
			inferenceStack.push(proof.getInferences(concl).iterator());
			blocked.clear();
			blockedMap.clear();
			findCycles(concl, concl);
			visited.add(concl);			
		}
	}

	private void initialize(List<Integer> consideredSCC) {
		this.consideredSCC = consideredSCC;
		inferenceStack = new LinkedList<Iterator<? extends Inference<? extends Integer>>>();
		blocked = new HashSet<Object>();
		blockedMap = new HashMap<Object, Set<Object>>(consideredSCC.size());
		premisesMap = new HashMap<Inference<? extends Integer>, Set<Object>>(consideredSCC.size());
		visited = new HashSet<Object>();
		inferencePath = new HashSet<Inference<? extends Integer>>();
	}

	private boolean findCycles(Object start, Object current) throws IOException, ParserException, ContradictionException {
		blocked.add(current);
		boolean foundCycle = false;
		for (Iterator<? extends Inference<? extends Integer>> iterator = inferenceStack.peek(); iterator.hasNext();) {
			Inference<? extends Integer> nextInf = iterator.next();
			inferencePath.add(nextInf);

			for (Object premise : getPremises(nextInf)) {

				// check if the premise was already visited
				if(visited.contains(premise)) {
					continue;
				}

				if(premise == start) {
					satClauseHandler.addCycleClause(inferencePath);
					foundCycle = true;
				} else {	
					if (!blocked.contains(premise)) {
						inferenceStack.push(proof.getInferences(premise).iterator());
						boolean gotCycle = findCycles(start, premise);
						foundCycle = gotCycle || foundCycle;
					}
				}
			}
			inferencePath.remove(nextInf);
		}

		inferenceStack.pop();

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
				Set<Object> blockedSet = blockedMap.get(premise);
				if(blockedSet == null) {
					blockedSet = new HashSet<Object>();
					blockedMap.put(premise, blockedSet);
				}
				blockedSet.add(premise);
			}
		}
	}

	private void unblock(Object axiom) {
		blocked.remove(axiom);
		Set<Object> blockedSet = blockedMap.get(axiom);
		if(blockedSet != null) {
			for (Object premise : blockedSet) {
				if(blocked.contains(premise)) {
					unblock(premise);
				}
			}
			blockedMap.remove(axiom);
		}
	}

	private Set<Object> getPremises(Inference<? extends Integer> nextInf) {
		Set<Object> premises = premisesMap.get(nextInf);
		if(premises != null) {
			return premises;
		}
		premises = new HashSet<Object>();
		for(Object premise : nextInf.getPremises()) {
			if(consideredSCC.contains(premise)) {
				premises.add(premise);
			}
		}
		premisesMap.put(nextInf, premises);
		return premises;
	}

}