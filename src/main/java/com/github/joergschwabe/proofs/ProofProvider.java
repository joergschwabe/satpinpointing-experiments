package com.github.joergschwabe.proofs;

import org.liveontologies.puli.Inference;

import com.github.joergschwabe.experiments.ExperimentException;

public interface ProofProvider<Q, C, I extends Inference<? extends C>, A> {

	JustificationCompleteProof<C, I, A> getProof(Q query)
			throws ExperimentException;

	void dispose();

}
