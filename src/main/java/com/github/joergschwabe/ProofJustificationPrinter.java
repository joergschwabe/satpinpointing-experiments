package com.github.joergschwabe;

/*-
 * #%L
 * Axiom Pinpointing Experiments
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2018 Live Ontologies Project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.ProofPrinter;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;

/**
 * A simple pretty printer of proofs together with justification numbers for
 * conclusions.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <C>
 *            the type of the conclusions in inferences
 * @param <I>
 *            the type of inferences in proofs
 * @param <A>
 *            the type of the axioms in proofs
 */
public class ProofJustificationPrinter<C, I extends Inference<? extends C>, A>
		extends ProofPrinter<C, I, A> {

	private final MinimalSubsetCollector<C, I, A> collector_;

	private final int sizeLimit_;

	ProofJustificationPrinter(
			final MinimalSubsetsFromProofs.Factory<C, I, A> factory,
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			int sizeLimit) {
		super(proof, justifier);
		this.collector_ = new MinimalSubsetCollector<>(factory, proof,
				justifier);
		this.sizeLimit_ = sizeLimit;
	}

	public static <C, I extends Inference<? extends C>, A> void print(
			final MinimalSubsetsFromProofs.Factory<C, I, A> factory,
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			C conclusion, int sizeLimit) throws IOException {
		ProofPrinter<C, I, A> pp = new ProofJustificationPrinter<>(factory,
				proof, justifier, sizeLimit);
		pp.printProof(conclusion);
	}

	public static <C, I extends Inference<? extends C>, A> void print(
			final MinimalSubsetsFromProofs.Factory<C, I, A> factory,
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			C conclusion) throws IOException {
		print(factory, proof, justifier, conclusion, Integer.MAX_VALUE);
	}

	@Override
	protected void writeConclusion(C conclusion) throws IOException {
		BufferedWriter w = getWriter();
		w.write('[');
		w.write(Integer
				.toString(collector_.collect(conclusion, sizeLimit_).size()));
		w.write(']');
		w.write(' ');
		super.writeConclusion(conclusion);
	}

}
