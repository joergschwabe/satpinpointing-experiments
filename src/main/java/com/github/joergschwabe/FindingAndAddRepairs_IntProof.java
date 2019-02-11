package com.github.joergschwabe;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.Proofs;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator.Listener;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <A> the type of axioms used by the inferences
 */
public class FindingAndAddRepairs_IntProof<I extends Inference<?>, A> {

	private ISolver solver = SolverFactory.newDefault();

	private Map<A, Integer> mapAxiomToId = new HashMap<A, Integer>();

	private Map<A, Set<I>> mapAxiomToInf = new HashMap<A, Set<I>>();

	private Map<Object, Integer> mapConclusionToId = new HashMap<Object, Integer>();

	private Set<I> infSet = new HashSet<I>();

	private int id = 1;

	private Listener<A> listener;

	private Object query;

	private Map<I, Set<? extends A>> mapInfToAxioms;

	public FindingAndAddRepairs_IntProof(Listener<A> listener, Object query) throws ContradictionException {
		this.listener = listener;
		this.query = query;
		translateQuery();
	}

	private void translateQuery() throws ContradictionException {
		mapConclusionToId.put(query, id);
		IVecInt clause = new VecInt();
		clause.push(id++);
		solver.addClause(clause);
	}

	public void addInfsToSat(Map<I, Set<? extends A>> mapInfToAxiomsTemp) throws ContradictionException {
		IVecInt clause;

		mapInfToAxioms = mapInfToAxiomsTemp;

		infSet = mapInfToAxioms.keySet();

		for (I inf : infSet) {

			clause = new VecInt();

			Object o = inf.getConclusion();

			// FA -> F1
			addConclusionToClause(clause, o);
			clause.push(id);
			solver.addClause(clause);

			// F1 -> L1
			clause.clear();
			clause.push(-id++);
			addPremisesToClause(inf, clause);
			addAxiomsToClause(inf, mapInfToAxioms.get(inf), clause);
			solver.addClause(clause);
		}
	}

	public void compute() throws TimeoutException, ContradictionException {
		Set<A> repair;
		Set<A> minRepair;

		while (solver.isSatisfiable()) {
			int[] list = solver.findModel();

			repair = translateModelToRepair(list);

			minRepair = computeMinimalRepair(repair);

			pushAxiomToSolver(minRepair);

			listener.newMinimalSubset(minRepair);
		}
	}

	private void addConclusionToClause(IVecInt clause, Object o) {
		if (mapConclusionToId.containsKey(o)) {
			clause.push(-mapConclusionToId.get(o));
		} else {
			mapConclusionToId.put(o, id);
			clause.push(-id++);
		}
	}

	private void addPremisesToClause(I inf, IVecInt clause) {
		for (final Object premise : inf.getPremises()) {
			if (mapConclusionToId.containsKey(premise)) {
				clause.push(mapConclusionToId.get(premise));
			} else {
				mapConclusionToId.put(premise, id);
				clause.push(id++);
			}
		}
	}

	private void addAxiomsToClause(I inf, Set<? extends A> justifications, IVecInt clause) {
		for (final A axiom : justifications) {
			Set<I> infSetTemp = new HashSet<I>();
			if (mapAxiomToId.containsKey(axiom)) {
				clause.push(mapAxiomToId.get(axiom));
				infSetTemp = mapAxiomToInf.get(axiom);
				infSetTemp.add(inf);
				mapAxiomToInf.put(axiom, infSetTemp);
			} else {
				infSetTemp.add(inf);
				mapAxiomToInf.put(axiom, infSetTemp);
				mapAxiomToId.put(axiom, id);
				clause.push(id++);
			}
		}
	}

	private Set<A> translateModelToRepair(int[] list) throws ContradictionException {
		Set<A> repair = new HashSet<A>();

		for (A axiom : mapAxiomToId.keySet()) {
			if (list[mapAxiomToId.get(axiom) - 1] > 0) {
				repair.add(axiom);
			}
		}

		return repair;
	}

	private void pushAxiomToSolver(Set<A> minRepair) throws ContradictionException {
		IVecInt clause = new VecInt();

		for (A axiom : minRepair) {
			clause.push(-mapAxiomToId.get(axiom));
		}

		solver.addClause(clause);
	}

	private Set<A> computeMinimalRepair(Set<A> repair) {
		Set<A> minRepair = new HashSet<A>();
		minRepair.addAll(repair);

		final Proof<I> proof = new FilteredProof(minRepair);
		for (A axiom : repair) {

			minRepair.remove(axiom);

			if (Proofs.isDerivable(proof, query)) {
				minRepair.add(axiom);
			}
		}

		return minRepair;
	}

//	private Set<A> computeMinimalRepair(Set<A> repair) {
//		final Proof<I> proof = new FilteredProof(repair);
//		InferenceDerivabilityChecker<Object, Inference<?>> derChecker = new InferenceDerivabilityChecker<Object, Inference<?>>(proof);
//		Set<A> minRepair = new HashSet<>();
//		for (A axiom : repair) {
//			derChecker.block(axiom);
//
//			if (derChecker.isDerivable(query)) {
//				minRepair.add(axiom);
//				derChecker.unblock(axiom);
//			}
//		}
//
//		return minRepair;
//	}

	class FilteredProof extends NewProof<I> {

		private final Set<A> forbiddenAxioms_;

		public FilteredProof(Set<A> forbiddenAxioms) {
			super(infSet);
			forbiddenAxioms_ = forbiddenAxioms;
		}

		@Override
		public Collection<I> getInferences(Object conclusion) {

			Collection<I> allInferences = super.getInferences(conclusion);
			Collection<I> result = new ArrayList<I>(allInferences.size());
			for (I inf : allInferences) {
				boolean infOK = true;
				for (A axiom : mapInfToAxioms.get(inf)) {
					if (forbiddenAxioms_.contains(axiom)) {
						infOK = false;
						break;
					}
				}
				if (infOK) {
					result.add(inf);
				}
			}
			return result;
		}

	}

}
