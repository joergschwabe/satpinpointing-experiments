package org.satpinpointing;

/*-
 * #%L
 * Axiom Pinpointing Experiments
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2019 Live Ontologies Project
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceDerivabilityChecker;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator.Listener;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import com.google.common.primitives.Ints;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <Object> the type of axioms used by the inferences
 */
public class SatClauseHandler<I extends Inference<?>, A> {

	private ISolver solver = SolverFactory.newDefault();

	public SatClauseHandler() {
	}

	public SatClauseHandler<I, A>.InfToSatTranslator getInfToSatTranslator(IdProvider<A, I> idProvider,
			Listener<A> listener, InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, Integer queryId) {
		return new InfToSatTranslator(idProvider, listener, infDeriv, queryId);
	}

	public class InfToSatTranslator {

		private IdProvider<A, I> idProvider;
		private Listener<A> listener_;
		private InferenceDerivabilityChecker<Object, Inference<?>> infDeriv;
		private int queryId;
		private Set<Integer> axiomIds;

		public InfToSatTranslator(IdProvider<A, I> idProvider2, Listener<A> listener,
				InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, Integer queryId) {
			this.idProvider = idProvider2;
			this.listener_ = listener;
			this.infDeriv = infDeriv;
			this.queryId = queryId;
		}

		public void translateQuery() throws ContradictionException {
			IVecInt clause = new VecInt();
			clause.push(queryId);
			solver.addClause(clause);
		}

		public void addInfToSolver(Inference<? extends Integer> inference) throws ContradictionException {
			IVecInt clause = new VecInt();

			// FA -> F1
			clause.push(-inference.getConclusion());
			int infId = idProvider.getNextId();
			clause.push(infId);
			solver.addClause(clause);

			// F1 -> L1
			clause.clear();
			clause.push(-infId);
			for (Integer premise : inference.getPremises()) {
				clause.push(premise);
			}

			solver.addClause(clause);
		}

		public void compute() throws TimeoutException, ContradictionException {
			Set<Integer> repair;
			Set<Integer> minRepairInt;
			Set<A> minRepair;
			axiomIds = idProvider.getAxiomIds();

			while (solver.isSatisfiable()) {
				int[] list = solver.findModel();
				List<Integer> listInt = Ints.asList(list);

				repair = translateModelToRepair(listInt);

				minRepairInt = computeMinimalRepair(repair);
//				minRepair = repair;

				pushAxiomToSolver(minRepairInt);

				minRepair = translateToAxioms(minRepairInt);
				
				listener_.newMinimalSubset(minRepair);
			}
		}

		private Set<A> translateToAxioms(Set<Integer> minRepairInt) {
			Set<A> minRepair = new HashSet<>();
			for (Integer axiomId : minRepairInt) {
				minRepair.add(idProvider.getAxiomFromId(axiomId));
			}
			return minRepair;
		}

		private Set<Integer> translateModelToRepair(List<Integer> listInt) throws ContradictionException {
			Set<Integer> repair = new HashSet<>();

			for (Integer modelId : listInt) {
				if (modelId > 0 && axiomIds.contains(modelId)) {
					repair.add(modelId);
				}
			}
			return repair;
		}

		private void pushAxiomToSolver(Set<Integer> minRepair) throws ContradictionException {
			IVecInt clause = new VecInt();

			for (Integer axiomId : minRepair) {
				clause.push(-axiomId);
			}

			solver.addClause(clause);
		}

		private Set<Integer> computeMinimalRepair(Set<Integer> repair) {
			Set<Integer> minRepair = new HashSet<Integer>();

			for (Integer axiomId : repair) {
				infDeriv.block(axiomId);
			}

			for (Integer axiomId : repair) {
				infDeriv.unblock(axiomId);
				
				if (infDeriv.isDerivable(queryId)) {
					minRepair.add(axiomId);
					infDeriv.block(axiomId);
				}
			}
			
			for (Integer axiomId : minRepair) {
				infDeriv.unblock(axiomId);
			}

			return minRepair;
		}
	}
}