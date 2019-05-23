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

import com.google.common.base.Preconditions;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <C> the type of conclusions used in inferences
 * @param <I> the type of inferences used in the proof
 * @param <A> the type of axioms used by the inferences
 */
public class SatRepairComputationIntegerLogicNg<C, I extends Inference<? extends C>, A>
		extends MinimalSubsetsFromProofs<C, I, A> {

	private static final SatRepairComputationIntegerLogicNg.Factory<?, ?, ?> FACTORY_ = new Factory<Object, Inference<?>, Object>();

	private final IntegerProofTranslator<C, I, A> proofTranslator_;

	@SuppressWarnings("unchecked")
	public static <C, I extends Inference<? extends C>, A> MinimalSubsetsFromProofs.Factory<C, I, A> getFactory() {
		return (Factory<C, I, A>) FACTORY_;
	}

	private SatRepairComputationIntegerLogicNg(final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier, final InterruptMonitor monitor) {
		super(proof, justifier, monitor);
		this.proofTranslator_ = new IntegerProofTranslator<C, I, A>(proof, justifier);
	}

	public MinimalSubsetEnumerator<A> newEnumerator(final Object query) {
		return new Enumerator(query);
	}

	private class Enumerator implements MinimalSubsetEnumerator<A>, Producer<Inference<? extends Integer>> {

		private final Object query;
		private SatClauseHandlerLogicNg<I, A> satClauseHandler_;

		Enumerator(final Object query) {
			this.query = query;
		}

		public void enumerate(Listener<A> listener, PriorityComparator<? super Set<A>, ?> unused) {
			enumerate(listener);
		}

		public void enumerate(Listener<A> listener) {
			Preconditions.checkNotNull(listener);

			try {
				IdProvider<A, I> idProvider = new IdProvider<>();

				Proof<Inference<? extends Integer>> translatedProofGetInferences = proofTranslator_
						.getTranslatedProofGetInferences(idProvider);

				InferenceDerivabilityChecker<Object, Inference<?>> infDeriv = new InferenceDerivabilityChecker<Object, Inference<?>>(
						translatedProofGetInferences);

				int queryId = idProvider.getConclusionId(query);

				satClauseHandler_ = new SatClauseHandlerLogicNg<I, A>(idProvider, listener, infDeriv, queryId);

				Proof<Inference<? extends Integer>> translatedProof = proofTranslator_.getTranslatedProof(idProvider,
						query);

				Proofs.unfoldRecursively(translatedProof, queryId, this);

				satClauseHandler_.translateQuery();

				satClauseHandler_.compute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void produce(Inference<? extends Integer> inference) {
			// translate the inference to SAT
			try {
				satClauseHandler_.addInfToSolver(inference);
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
			return new SatRepairComputationIntegerLogicNg<C, I, A>(proof, justifier, monitor);
		}

	}

}
