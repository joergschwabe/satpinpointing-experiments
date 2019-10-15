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
import org.logicng.solvers.SATSolver;
import org.sat4j.specs.ContradictionException;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <Object> the type of axioms used by the inferences
 */
public class SatClauseHandlerLogicNg<I extends Inference<?>, A> extends SatClauseHandler<I, A> {

	private IdProvider<A, I> idProvider;
	private int queryId;
	private FormulaFactory f;
	private PropositionalParser p;
	private SATSolver solver;

	public SatClauseHandlerLogicNg(IdProvider<A, I> idProvider,
			InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, int queryId, SATSolver solver) {
		super(idProvider, infDeriv, queryId);
		this.idProvider = idProvider;
		this.queryId = queryId;
		this.solver = solver;
		f = new FormulaFactory();
		p = new PropositionalParser(f);
	}

	public SATSolver getSATSolver() throws ContradictionException {
		return this.solver;
	}

	public void translateQuery() throws ParserException {
		Formula formula = p.parse(Integer.toString(queryId));
		solver.add(formula);
	}

	public Set<Integer> getPositiveOntologieAxioms(Assignment model) throws ContradictionException {
		Set<Integer> axiomSet = new HashSet<>();

		List<Variable> posLiterals = model.positiveLiterals();

		Set<Integer> axiomIds = idProvider.getAxiomIds();
		for (Variable var : posLiterals) {
			int varInt = Integer.parseInt(var.toString());
			if (axiomIds.contains(varInt)) {
				axiomSet.add(varInt);
			}
		}

		return axiomSet;
	}

	public void addInfToSolver(Inference<? extends Integer> inference) throws ParserException {
		String formulaString = "~" + inference.getConclusion();
		for (Integer premise : inference.getPremises()) {
			formulaString = formulaString + " | " + premise;
		}

		Formula formula = p.parse(formulaString);
		this.solver.add(formula);
	}

	public void addInfImplicationToSolver(Inference<? extends Integer> inference) throws ParserException {
		if (inference.getPremises().isEmpty()) {
			return;
		}

		String formulaString = "";
		for (Integer premise : inference.getPremises()) {
			formulaString = formulaString + " & " + premise;
		}

		formulaString = " | (" + formulaString.substring(3) + ")";
		Formula formula = p.parse("~" + idProvider.getInferenceId(inference) + formulaString);
		solver.add(formula);
	}

	public void pushNegClauseToSolver(Set<Integer> axiomSet) throws ParserException {
		String formulaString = "";
		for (Integer axiomId : axiomSet) {
			formulaString = formulaString + " | ~" + axiomId;
		}

		Formula formula = p.parse(formulaString.substring(3));
		this.solver.add(formula);
	}

	void pushPosClauseToSolver(Set<Integer> axiomSet) throws ParserException {
		String formulaString = "";
		for (Integer axiomId : axiomSet) {
			formulaString = formulaString + " | " + axiomId;
		}
		Formula formula = p.parse(formulaString.substring(3));
		this.solver.add(formula);
	}

	public void addConclusionInferencesClauses() throws ParserException {
		String formulaString = "";
		for (Integer conclusionId : idProvider.getConclusionIds()) {
			formulaString = "~" + conclusionId;
			for (Integer inferenceId : idProvider.getInferenceIds(conclusionId)) {
				formulaString += " | " + inferenceId;
			}

			Formula formula = p.parse(formulaString);
			this.solver.add(formula);
		}
	}

	public void addCycleClause(Set<Inference<? extends Integer>> cycle) throws ParserException {
		String formulaString ="";
		for(Inference<? extends Integer> inf : cycle) {
			int infId = idProvider.getInferenceId(inf);
			formulaString += " | ~" + infId;
		}
		Formula formula = p.parse(formulaString.substring(3));
		this.solver.add(formula);
	}
}