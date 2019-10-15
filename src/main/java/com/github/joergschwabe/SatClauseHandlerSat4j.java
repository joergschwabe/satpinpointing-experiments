package com.github.joergschwabe;

import java.util.HashSet;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceDerivabilityChecker;
import org.sat4j.core.VecInt;
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
public class SatClauseHandlerSat4j<I extends Inference<?>, A> extends SatClauseHandler<I, A> {

	private IdProvider<A, I> idProvider;
	private int queryId;
	private ISolver solver;

	public SatClauseHandlerSat4j(IdProvider<A, I> idProvider,
			InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, Integer queryId, ISolver solver) {
		super(idProvider, infDeriv, queryId);
		this.idProvider = idProvider;
		this.queryId = queryId;
		this.solver = solver;
	}

	public ISolver getISolver() throws TimeoutException, ContradictionException {
		return solver;
	}

	public Set<Integer> getPositiveOntologieAxioms(int[] list) throws ContradictionException {
		Set<Integer> axiomSet = new HashSet<>();
		Set<Integer> axiomIds = idProvider.getAxiomIds();

		for (Integer modelId : list) {
			if (modelId > 0 && axiomIds.contains(modelId)) {
				axiomSet.add(modelId);
			}
		}
		return axiomSet;
	}

	public void translateQuery() throws ContradictionException {
		IVecInt clause = new VecInt();
		clause.push(queryId);
		solver.addClause(clause);
	}

	public void addInfToSolver(Inference<? extends Integer> inference) throws ContradictionException {
		IVecInt clause = new VecInt();

		clause.push(-inference.getConclusion());
		for (Integer premise : inference.getPremises()) {
			clause.push(premise);
		}

		solver.addClause(clause);
	}

	public void addInfImplicationToSolver(Inference<? extends Integer> inference) throws ContradictionException {
		if (inference.getPremises().isEmpty()) {
			return;
		}

		Integer inferenceId = idProvider.getInferenceId(inference);

		// FA -> F1
		for (Integer premise : inference.getPremises()) {
			IVecInt clause = new VecInt();
			clause.push(-inferenceId);
			clause.push(premise);
			solver.addClause(clause);
		}
	}

	public void pushNegClauseToSolver(Set<Integer> axiomSet) throws ContradictionException {
		IVecInt clause = new VecInt();

		for (Integer axiomId : axiomSet) {
			clause.push(-axiomId);
		}

		solver.addClause(clause);
	}

	void pushPosClauseToSolver(Set<Integer> axiomSet) throws ContradictionException {
		IVecInt clause = new VecInt();
		for (Integer axiomId : axiomSet) {
			clause.push(axiomId);
		}
		solver.addClause(clause);
	}

	public void addConclusionInferencesClauses() throws ContradictionException {
		for (Integer conclusionId : idProvider.getConclusionIds()) {
			IVecInt clause = new VecInt();
			clause.push(-conclusionId);
			for (Integer inferenceId : idProvider.getInferenceIds(conclusionId)) {
				clause.push(inferenceId);
			}

			solver.addClause(clause);
		}
	}

	public void addCycleClauses(Set<Set<Inference<? extends Integer>>> cycles) throws ContradictionException {
		for (Set<Inference<? extends Integer>> cycle : cycles) {
			IVecInt clause = new VecInt();
			for (Inference<? extends Integer> inf : cycle) {
				int infId = idProvider.getInferenceId(inf);
				clause.push(-infId);
			}
			solver.addClause(clause);
		}
	}
}