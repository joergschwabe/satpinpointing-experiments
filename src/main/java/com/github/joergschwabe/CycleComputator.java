
package com.github.joergschwabe;

import java.io.IOException;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
	 * contains all visited conclusions
	 */
	private final Deque<Integer> visited_ = new LinkedList<Integer>();

	/**
	 * the inferences of considered path
	 */
	private final List<Inference<? extends Integer>> inferencePath_ = new LinkedList<Inference<? extends Integer>>();

	private final List<Integer> conclusionPath_ = new LinkedList<Integer>();

	private Set<Integer> consideredSCC = new HashSet<Integer>();

	private int queryId;

	private Deque<Iterator<Integer>> conclusionStack_ = new LinkedList<Iterator<Integer>>();

	private Set<Inference<? extends Integer>> inferenceSet;

	public CycleComputator(final Proof<Inference<? extends Integer>> proof, int queryId) {
		this.proof = proof;
		this.queryId = queryId;
	}

	public Set<Inference<? extends Integer>> getCycle(Set<Integer> conclusionSet, Set<Inference<? extends Integer>> inferenceSet) throws IOException, ParserException, ContradictionException {
		this.inferenceSet = inferenceSet;
		StronglyConnectedComponents<Integer> sccc = StronglyConnectedComponentsComputation.computeComponents(proof, queryId);
		for(List<Integer> consideredSCC : sccc.getComponents()) {
			consideredSCC.retainAll(conclusionSet);
			if(consideredSCC.size() > 1) {
				for(Integer concl : consideredSCC) {
					conclusionStack_.clear();
					inferenceStack_.clear();
					inferencePath_.clear();
					visited_.clear();
					conclusionPath_.clear();
					this.consideredSCC = new HashSet<Integer>(consideredSCC);
					inferenceStack_.push(getInferences(concl).iterator());
					visited_.add(concl);
					conclusionPath_.add(concl);
					Set<Inference<? extends Integer>> cycle = findCycle();
					if(cycle != null) {
						return cycle;
					}
				}
			}
		}
		return null;
	}

	private Deque<Inference<? extends Integer>> getInferences(Integer concl) {
		Deque<Inference<? extends Integer>> infDeq = new LinkedList<>();
		for(Inference<? extends Integer> inf : proof.getInferences(concl)){
			if(inferenceSet.contains(inf)) {
				infDeq.push(inf);
			}
		}
		return infDeq;
	}

	private Set<Inference<? extends Integer>> findCycle()
			throws IOException, ParserException, ContradictionException {
		for(;;) {
			Iterator<? extends Inference<? extends Integer>> infIter = inferenceStack_.peek();
			if(infIter == null) {
				return null;
			}

			if(infIter.hasNext()) {
				Inference<? extends Integer> nextInf = infIter.next();
				conclusionStack_.push(getPremises(nextInf).iterator());
				inferencePath_.add(nextInf);
			} else {
				visited_.pop();
				conclusionPath_.remove(conclusionPath_.size()-1);
				inferenceStack_.pop();
			}

			Iterator<? extends Integer> conclIter = conclusionStack_.peek();
			if(conclIter == null) {
				return null;
			}

			if(conclIter.hasNext()) {
				Integer premise = conclIter.next();
				
				if(visited_.contains(premise)) {
					List<Inference<? extends Integer>> list = inferencePath_.subList(conclusionPath_.indexOf(premise), inferencePath_.size());
					return new HashSet<Inference<? extends Integer>>(list);
				}

				visited_.push(premise);
				conclusionPath_.add(premise);
				inferenceStack_.push(getInferences(premise).iterator());

				continue;
			}
			conclusionStack_.pop();
			inferencePath_.remove(inferencePath_.size()-1);
		}

	}

	private Set<Integer> getPremises(Inference<? extends Integer> nextInf) {
		Set<Integer> premises = new HashSet<Integer>();
		for (Integer premise : nextInf.getPremises()) {
			if (consideredSCC.contains(premise)) {
				premises.add(premise);
			}
		}
		return premises;
	}

}