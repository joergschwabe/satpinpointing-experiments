package com.github.joergschwabe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceDerivabilityChecker;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <Object> the type of axioms used by the inferences
 */
public class SatClauseHandlerRepairLogicNg_intuitive<I extends Inference<?>, A> {

	private IdProvider<A, I> idProvider;
	private InferenceDerivabilityChecker<Object, Inference<?>> infDeriv;
	private int queryId;
	private FormulaFactory f;
	private PropositionalParser p;
	private SATSolver miniSat;

	public SatClauseHandlerRepairLogicNg_intuitive(IdProvider<A, I> idProvider,	InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, int queryId) {
		this.idProvider = idProvider;
		this.infDeriv = infDeriv;
		this.queryId = queryId;
		f = new FormulaFactory();
		p = new PropositionalParser(f);
		miniSat = MiniSat.miniSat(f);
	}

	public void translateQuery() throws ParserException {
		Formula formula = p.parse(Integer.toString(queryId));
		miniSat.add(formula);
	}

	public void addInfToSolver(Inference<? extends Integer> inference) throws ParserException {
		String formulaString = "~" + inference.getConclusion();
		for (Integer premise : inference.getPremises()) {
			formulaString = formulaString + " | " + premise;
		}

		Formula formula = p.parse(formulaString);
		miniSat.add(formula);
	}

	public SATSolver getSolver() {
		return miniSat;
	}

	Set<A> translateToAxioms(Set<Integer> minRepairInt) {
		Set<A> minRepair = new HashSet<>();
		for (Integer axiomId : minRepairInt) {
			minRepair.add(idProvider.getAxiomFromId(axiomId));
		}
		return minRepair;
	}

	Set<Integer> computeJustification(Set<Integer> axiomSet) {
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

	Set<Integer> getPositiveOntologieAxioms(Assignment model) throws ContradictionException {
		Set<Integer> axiomSet = new HashSet<>();
		Set<Integer> axiomIds = idProvider.getAxiomIds();

		List<Variable> posLiterals = model.positiveLiterals();

		for (Variable var : posLiterals) {
			int varInt = Integer.parseInt(var.toString());
			if (axiomIds.contains(varInt)) {
				axiomSet.add(varInt);
			}
		}
		return axiomSet;
	}

	void pushRepairToSolver(Set<Integer> minRepair) throws ParserException {
		String formulaString = "";
		for (Integer axiomId : minRepair) {
			formulaString = formulaString + " | " + axiomId;
		}

		Formula formula = p.parse(formulaString.substring(3));
		miniSat.add(formula);
	}

	void pushJustificationToSolver(Set<Integer> justification) throws ContradictionException, ParserException {
		String formulaString = "";
		for (Integer axiomId : justification) {
			formulaString = formulaString + " | ~" + axiomId;
		}

		Formula formula = p.parse(formulaString.substring(3));
		miniSat.add(formula);
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
}