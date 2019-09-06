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
public class SatClauseHandlerSat4j_intuitive<I extends Inference<?>, A> {

	private ISolver solver;

	private IdProvider<A, I> idProvider;
	private InferenceDerivabilityChecker<Object, Inference<?>> infDeriv;
	private int queryId;

	public SatClauseHandlerSat4j_intuitive(IdProvider<A, I> idProvider, InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, Integer queryId) {
		this.idProvider = idProvider;
		this.infDeriv = infDeriv;
		this.queryId = queryId;
		this.solver = SolverFactory.newDefault();
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

	Set<Integer> getPositiveOntologieAxioms(int[] list) throws ContradictionException {
		Set<Integer> axiomSet = new HashSet<>();
		Set<Integer> axiomIds = idProvider.getAxiomIds();

		for (Integer modelId : list) {
			if (modelId > 0 && axiomIds.contains(modelId)) {
				axiomSet.add(modelId);
			}
		}
		return axiomSet;
	}

	void pushRepairToSolver(Set<Integer> minRepair) throws ContradictionException {
		IVecInt clause = new VecInt();

		for (Integer axiomId : minRepair) {
			clause.push(axiomId);
		}
		solver.addClause(clause);
	}

	void pushJustificationToSolver(Set<Integer> minJustification) throws ContradictionException {
		IVecInt clause = new VecInt();

		for (Integer axiomId : minJustification) {
			clause.push(-axiomId);
		}
		solver.addClause(clause);
	}

	Set<Integer> computeMinimalRepair(Set<Integer> axiomSet) {
		Set<Integer> maxAxioms = new HashSet<Integer>(axiomSet);
		Set<Integer> axiomSetAll = new HashSet<Integer>(idProvider.getAxiomIds());
		axiomSetAll.removeAll(axiomSet);

		for (Integer axiomId : axiomSetAll) {
			infDeriv.block(axiomId);
		}

		for (Integer axiomId : axiomSetAll) {
			infDeriv.unblock(axiomId);

			if (!infDeriv.isDerivable(queryId)) {
				maxAxioms.add(axiomId);
			} else {
				infDeriv.block(axiomId);				
			}
		}

		for (Integer axiomId : axiomSetAll) {
			infDeriv.unblock(axiomId);
		}

		Set<Integer> minRepair = new HashSet<Integer>(idProvider.getAxiomIds());
		minRepair.removeAll(maxAxioms);
		return minRepair;
	}

	Set<Integer> computeMinimalJustification(Set<Integer> axiomSet) {
		Set<Integer> justification = new HashSet<Integer>(axiomSet);
		Set<Integer> axiomSetAll = new HashSet<Integer>(idProvider.getAxiomIds());
		axiomSetAll.removeAll(axiomSet);

		for (Integer axiomId : axiomSetAll) {
			infDeriv.block(axiomId);
		}
		
		for (Integer axiomId : axiomSet) {
			infDeriv.block(axiomId);

			if (infDeriv.isDerivable(queryId)) {
				justification.remove(axiomId);
			} else {
				infDeriv.unblock(axiomId);
			}
		}

		axiomSetAll = new HashSet<Integer>(idProvider.getAxiomIds());
		for (Integer axiomId : axiomSetAll) {
			infDeriv.unblock(axiomId);
		}		

		return justification;
	}

	public boolean isQueryDerivable(Set<Integer> axiomSet) {
		Set<Integer> axiomSetAll = new HashSet<Integer>(idProvider.getAxiomIds());
		axiomSetAll.removeAll(axiomSet);
		for (Integer axiomId : axiomSetAll) {
			infDeriv.block(axiomId);
		}

		boolean returnValue = false;
		if (infDeriv.isDerivable(queryId)) {
			returnValue = true;
		}

		for (Integer axiomId : axiomSetAll) {
			infDeriv.unblock(axiomId);
		}
		
		return returnValue;
	}
}