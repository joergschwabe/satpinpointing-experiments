package com.github.joergschwabe;

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
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import com.google.common.base.Preconditions;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <C> the type of conclusions used in inferences
 * @param <I> the type of inferences used in the proof
 * @param <A> the type of axioms used by the inferences
 */
public class SatRepairComputationSat4j_intuitive<C, I extends Inference<? extends C>, A>
		extends MinimalSubsetsFromProofs<C, I, A> {

	private static final SatRepairComputationSat4j_intuitive.Factory<?, ?, ?> FACTORY_ = new Factory<Object, Inference<?>, Object>();


	@SuppressWarnings("unchecked")
	public static <C, I extends Inference<? extends C>, A> MinimalSubsetsFromProofs.Factory<C, I, A> getFactory() {
		return (Factory<C, I, A>) FACTORY_;
	}

	private SatRepairComputationSat4j_intuitive(final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier, final InterruptMonitor monitor) {
		super(proof, justifier, monitor);
	}

	public MinimalSubsetEnumerator<A> newEnumerator(final Object query) {
		return new Enumerator(query);
	}

	private class Enumerator implements MinimalSubsetEnumerator<A>, Producer<Inference<? extends Integer>> {

		private final Object query;
		private SatClauseHandlerRepairSat4j_intuitive<I, A> satClauseHandler_;
		private IntegerProofTranslator<C, I, A> proofTranslator_;
		private Listener<A> listener_;
		private IdProvider<A, I> idProvider_;
		
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

			satClauseHandler_ = new SatClauseHandlerRepairSat4j_intuitive<I, A>(idProvider_, infDeriv, queryId_);

			Proof<Inference<? extends Integer>> translatedProof = proofTranslator_.getTranslatedProof(idProvider_,
					query);

			Proofs.unfoldRecursively(translatedProof, queryId_, this);

			try {
				compute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private void compute() throws TimeoutException, ContradictionException {
			Set<Integer> axiomSet;
			Set<Integer> minJust_int;
			Set<Integer> minRepair_int;
			Set<A> minRepair;

			ISolver solver = satClauseHandler_.getSolver();

			while (solver.isSatisfiable()) {
				int[] list = solver.model();

				axiomSet = satClauseHandler_.getPositiveOntologieAxioms(list);

				if(satClauseHandler_.isQueryDerivable(axiomSet)) {
					minJust_int = satClauseHandler_.computeMinimalJustification(axiomSet);

					try {
						satClauseHandler_.pushJustificationToSolver(minJust_int);
					} catch (ContradictionException e) {
						break;
					}					
				} else {
					minRepair_int = satClauseHandler_.computeMinimalRepair(axiomSet);

					try {
						satClauseHandler_.pushRepairToSolver(minRepair_int);
					} catch (ContradictionException e) {
						minRepair = satClauseHandler_.translateToAxioms(minRepair_int);

						listener_.newMinimalSubset(minRepair);
						
						break;
					}					
					
					minRepair = satClauseHandler_.translateToAxioms(minRepair_int);

					listener_.newMinimalSubset(minRepair);
				}


				if (isInterrupted()) {
					break;
				}
			}

			
		}

		@Override
		public void produce(Inference<? extends Integer> inference) {
			// translate the inference to SAT
//			try {
//				satClauseHandler_.addInfToSolver(inference);
//			} catch (ContradictionException e) {
//				e.printStackTrace();
//			}
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
			return new SatRepairComputationSat4j_intuitive<C, I, A>(proof, justifier, monitor);
		}

	}

}
