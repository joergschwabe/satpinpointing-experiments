package com.github.joergschwabe;

import java.util.HashSet;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceDerivabilityChecker;
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
 * @param <Object> the type of axioms used by the inferences
 */
public class SatClauseHandlerSat4j_bestHT<I extends Inference<?>, A> {

	private ISolver solver;

	private IdProvider<A, I> idProvider;
	private InferenceDerivabilityChecker<Object, Inference<?>> infDeriv;
	private int queryId;

	public SatClauseHandlerSat4j_bestHT(IdProvider<A, I> idProvider, InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, Integer queryId) {
		this.idProvider = idProvider;
		this.infDeriv = infDeriv;
		this.queryId = queryId;
		this.solver = SolverFactory.newBestHT();
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

	public ISolver getSolver() throws TimeoutException, ContradictionException {
		return solver;
	}

	Set<A> translateToAxioms(Set<Integer> repair) {
		Set<A> minRepair = new HashSet<>();
		for (Integer axiomId : repair) {
			minRepair.add(idProvider.getAxiomFromId(axiomId));
		}
		return minRepair;
	}

	Set<Integer> translateModelToRepair(int[] list) throws ContradictionException {
		Set<Integer> repair = new HashSet<>();
		Set<Integer> axiomIds = idProvider.getAxiomIds();

		for (Integer modelId : list) {
			if (modelId > 0 && axiomIds.contains(modelId)) {
				repair.add(modelId);
			}
		}
		return repair;
	}

	void pushRepairToSolver(Set<Integer> minRepair) throws ContradictionException {
		IVecInt clause = new VecInt();

		for (Integer axiomId : minRepair) {
			clause.push(-axiomId);
		}

		solver.addClause(clause);
	}

	Set<Integer> computeMinimalRepair(Set<Integer> repair) {
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