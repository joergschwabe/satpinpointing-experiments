package com.github.joergschwabe;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceDerivabilityChecker;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Producer;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.Proofs;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.liveontologies.puli.pinpointing.PriorityComparator;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.sat4j.specs.ContradictionException;

import com.google.common.base.Preconditions;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <C> the type of conclusions used in inferences
 * @param <I> the type of inferences used in the proof
 * @param <A> the type of axioms used by the inferences
 */
public class SatJustificationComputationLogicNg_glucose<C, I extends Inference<? extends C>, A>
		extends MinimalSubsetsFromProofs<C, I, A> {

	private static final SatJustificationComputationLogicNg_glucose.Factory<?, ?, ?> FACTORY_ = new Factory<Object, Inference<?>, Object>();


	@SuppressWarnings("unchecked")
	public static <C, I extends Inference<? extends C>, A> MinimalSubsetsFromProofs.Factory<C, I, A> getFactory() {
		return (Factory<C, I, A>) FACTORY_;
	}

	private SatJustificationComputationLogicNg_glucose(final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier, final InterruptMonitor monitor) {
		super(proof, justifier, monitor);
	}

	public MinimalSubsetEnumerator<A> newEnumerator(final Object query) {
		return new Enumerator(query);
	}

	private class Enumerator implements MinimalSubsetEnumerator<A>, Producer<Inference<? extends Integer>> {


		private final Object query;
		private SatClauseHandlerLogicNg<I, A> satClauseHandler_;
		private IntegerProofTranslator<C, I, A> proofTranslator_;
		private Listener<A> listener_;
		private IdProvider<A, I> idProvider_;
		private CycleComputator cycleComputator;
		
		Enumerator(final Object query) {
			this.query = query;
		}

		public void enumerate(Listener<A> listener, PriorityComparator<? super Set<A>, ?> unused) {
			enumerate(listener);
		}

		public void enumerate(Listener<A> listener) {
			Preconditions.checkNotNull(listener);
			this.listener_ = listener;
			
			idProvider_ = new IdProvider<>();

			this.proofTranslator_ = new IntegerProofTranslator<C, I, A>(getProof(), getInferenceJustifier());
			Proof<Inference<? extends Integer>> translatedProofGetInferences = proofTranslator_
					.getTranslatedProofDiv(idProvider_);

			InferenceDerivabilityChecker<Object, Inference<?>> infDeriv = new InferenceDerivabilityChecker<Object, Inference<?>>(
					translatedProofGetInferences);

			int queryId_ = idProvider_.getConclusionId(query);

			satClauseHandler_ = new SatClauseHandlerLogicNg<I, A>(idProvider_, infDeriv, queryId_, MiniSat.glucose(new FormulaFactory()));

			Proof<Inference<? extends Integer>> translatedProof = proofTranslator_.getTranslatedProof(idProvider_,
					query);

			Proofs.unfoldRecursively(translatedProof, queryId_, this);

			cycleComputator = new CycleComputator(translatedProof, queryId_);

			try {
				satClauseHandler_.translateQuery();

				satClauseHandler_.addConclusionInferencesClauses();

				compute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private void compute() throws ContradictionException, ParserException, IOException {
			Set<Integer> axiomSet;
			Set<Integer> conclusionSet;
			Set<Inference<? extends Integer>> inferenceSet;
			Set<Integer> justification_int;
			Set<A> justification;

			SATSolver solver = satClauseHandler_.getSATSolver();

			while (solver.sat() == Tristate.TRUE) {
				Assignment model = solver.model();

				axiomSet = satClauseHandler_.getPositiveOntologieAxioms(model);

				if(satClauseHandler_.isQueryDerivable(axiomSet)) {
					if(axiomSet.isEmpty()) {
						listener_.newMinimalSubset(new HashSet<A>());
						break;
					}

					justification_int = satClauseHandler_.computeJustification(axiomSet);

					if(justification_int.isEmpty()) {
						listener_.newMinimalSubset(new HashSet<A>());
						break;
					}

					satClauseHandler_.pushNegClauseToSolver(justification_int);
					
					justification = satClauseHandler_.translateToAxioms(justification_int);

					listener_.newMinimalSubset(justification);
				} else {
					conclusionSet = satClauseHandler_.getConclusionAxioms(model);

					inferenceSet = satClauseHandler_.getPositiveInferences(model);

					Set<Inference<? extends Integer>> cycle = cycleComputator.getCycle(conclusionSet, inferenceSet);
					
					satClauseHandler_.addCycleClause(cycle);
				}

				if (isInterrupted()) {
					break;
				}
			}			
		}

		@Override
		public void produce(Inference<? extends Integer> inference) {
			// translate the inference to SAT
			try {
				satClauseHandler_.addInfImplicationToSolver(inference);
				idProvider_.addConclusionInference(inference);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * The factory.
	 * 
	 * @author Jörg Schwabe
	 *
	 * @param <C> the type of conclusions used in inferences
	 * @param <I> the type of inferences used in the proof
	 * @param <A> the type of axioms used by the inferences
	 */
	private static class Factory<C, I extends Inference<? extends C>, A>
			implements MinimalSubsetsFromProofs.Factory<C, I, A> {

		public MinimalSubsetEnumerator.Factory<C, A> create(final Proof<? extends I> proof,
				final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
				final InterruptMonitor monitor) {
			return new SatJustificationComputationLogicNg_glucose<C, I, A>(proof, justifier, monitor);
		}

	}

}
