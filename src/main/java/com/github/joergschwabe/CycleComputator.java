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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
	private final Deque<Iterator<?>> conclusionStack_ = new LinkedList<Iterator<?>>();

	/**
	 * accumulates the printed conclusions to avoid repetitions
	 */
	private final Set<Object> printed_ = new HashSet<Object>();

	/**
	 * the conclusions of considered path
	 */
	private final List<Object> conclusionPath_ = new ArrayList<Object>();

	/**
	 * the inferences of considered path
	 */
	private final List<I> inferencePath_ = new ArrayList<I>();

	/**
	 * contains all axioms of the ontology
	 */
	private final Set<Integer> axiomSet;

	/**
	 * contains all computed cycles
	 */
	private final Set<List<I>> cycles_ = new HashSet<>();

	protected CycleComputator(final Proof<? extends I> proof, Set<Integer> axiomSet) {
		this.proof = proof;
		this.axiomSet = axiomSet;
	}

	public Set<List<I>> getCycles(Object conclusion) throws IOException {
		process(conclusion);
		process();
		return cycles_;
	}

	private boolean process(Object conclusion) throws IOException {
		boolean newConclusion = printed_.add(conclusion);
		if (newConclusion) {
			inferenceStack_.push(proof.getInferences(conclusion).iterator());
		}
		return newConclusion;
	}

	private void process() throws IOException {
		for (;;) {
			// processing inferences
			Iterator<? extends I> infIter = inferenceStack_.peek();
			if (infIter == null) {
				return;
			}
			// else
			if (infIter.hasNext()) {
				I nextInf = infIter.next();
				if(nextInf.getPremises().isEmpty()) {
					continue;
				}
				conclusionStack_.push(getConclusions(nextInf).iterator());
				inferencePath_.add(nextInf);
			} else {
				if(conclusionPath_.size() > 0) {
					conclusionPath_.remove(conclusionPath_.size()-1);
				}
				inferenceStack_.pop();
			}
			// processing conclusions
			Iterator<?> conclIter = conclusionStack_.peek();
			if (conclIter == null) {
				return;
			}
			// else
			for (;;) {
				if (conclIter.hasNext()) {
					Object nextConclusion = conclIter.next();
					conclusionPath_.add(nextConclusion);
					if (process(nextConclusion)) {
						break;
					}
					// else
					if(conclusionPath_.subList(0, conclusionPath_.size()-1).contains(nextConclusion)) {
						int firstIndex = conclusionPath_.indexOf(nextConclusion);
						ArrayList<I> infCycle = new ArrayList<>();
						infCycle.addAll(inferencePath_.subList(firstIndex+1, inferencePath_.size()));
						cycles_.add(infCycle);
					}
					if(conclusionPath_.size() > 0) {
						conclusionPath_.remove(conclusionPath_.size()-1);
					}
					continue;
				}
				if(inferencePath_.size() > 0) {
					inferencePath_.remove(inferencePath_.size()-1);
				}
				conclusionStack_.pop();
				
				break;
			}
		}
	}

	private List<?> getConclusions(I inf) {
		List<Object> premises = new ArrayList<Object>();
		for(Object elem : inf.getPremises()) {
			if(!axiomSet.contains(elem)) {
				premises.add(elem);
			}
		}
		return premises;
	}

}
