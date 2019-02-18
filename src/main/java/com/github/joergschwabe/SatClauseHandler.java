package com.github.joergschwabe;

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

	public SatClauseHandler() {
	}

	public SatClauseHandler<I, A>.InfToSatTranslator getInfToSatTranslator(IdProvider<A, I> idProvider,
			Listener<A> listener, InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, Integer queryId) {
		return new InfToSatTranslator(idProvider, listener, infDeriv, queryId);
	}

	public class InfToSatTranslator {
		
		private ISolver solver;

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
			this.solver = SolverFactory.newDefault();
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
			for (Integer premise : inference.getPremises()) {
				clause.push(premise);
			}

			solver.addClause(clause);
		}

		public void compute() throws TimeoutException, ContradictionException {
			Set<Integer> repair_int;
			Set<A> minRepair;
			axiomIds = idProvider.getAxiomIds();

			while (solver.isSatisfiable()) {
				int[] list = solver.model();

				repair_int = translateModelToRepair(list);

				repair_int = computeMinimalRepair(repair_int);

				pushAxiomToSolver(repair_int);

				minRepair = translateToAxioms(repair_int);
				
				listener_.newMinimalSubset(minRepair);
			}
		}

		private Set<A> translateToAxioms(Set<Integer> repair) {
			Set<A> minRepair = new HashSet<>();
			for (Integer axiomId : repair) {
				minRepair.add(idProvider.getAxiomFromId(axiomId));
			}
			return minRepair;
		}

		private Set<Integer> translateModelToRepair(int[] list) throws ContradictionException {
			Set<Integer> repair = new HashSet<>();

			for (Integer modelId : list) {
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