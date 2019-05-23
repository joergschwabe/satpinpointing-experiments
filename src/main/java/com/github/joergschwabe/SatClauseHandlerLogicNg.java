package com.github.joergschwabe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceDerivabilityChecker;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator.Listener;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <Object> the type of axioms used by the inferences
 */
public class SatClauseHandlerLogicNg<I extends Inference<?>, A> {

	private IdProvider<A, I> idProvider;
	private Listener<A> listener_;
	private InferenceDerivabilityChecker<Object, Inference<?>> infDeriv;
	private int queryId;
	private Set<Integer> axiomIds;
	private FormulaFactory f;
	private PropositionalParser p;
	private SATSolver miniSat;

	public SatClauseHandlerLogicNg(IdProvider<A, I> idProvider, Listener<A> listener,
			InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, int queryId) {
		this.idProvider = idProvider;
		this.listener_ = listener;
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

	public void compute() throws TimeoutException, ParserException, ContradictionException {
		Set<Integer> repair_int;
		Set<A> minRepair;
		axiomIds = idProvider.getAxiomIds();

		while (miniSat.sat() == Tristate.TRUE) {
			Assignment model = miniSat.model();

			repair_int = translateModelToRepair(model);

			repair_int = computeMinimalRepair(repair_int);

			pushAxiomToSolver(repair_int);

			minRepair = translateToAxioms(repair_int);

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

	private Set<Integer> translateModelToRepair(Assignment model) throws ContradictionException {
		Set<Integer> repair = new HashSet<>();

		List<Variable> posLiterals = model.positiveLiterals();

		for (Variable var : posLiterals) {
			int varInt = Integer.parseInt(var.toString());
			if (axiomIds.contains(varInt)) {
				repair.add(varInt);
			}
		}

		return repair;
	}

	private void pushAxiomToSolver(Set<Integer> minRepair) throws ParserException {
		String formulaString = "";
		for (Integer axiomId : minRepair) {
			formulaString = formulaString + " | ~" + axiomId;
		}

		Formula formula = p.parse(formulaString.substring(3));
		miniSat.add(formula);
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