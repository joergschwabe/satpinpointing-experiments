package com.github.joergschwabe.proofs;

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

import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;

import com.github.joergschwabe.experiments.ExperimentException;

/**
 * The proof returned by {@link #getProof()} with the justifier returned by
 * {@link #getJustifier()} is complete w.r.t. justifications of the conclusion
 * returned by {@link #getQuery()}.
 * 
 * @author Peter Skocovsky
 * 
 * @param <C>
 *            The type of conclusions over which are the inferences in the
 *            proof.
 * @param <I>
 *            The type of inferences in the proof.
 * @param <A>
 *            The type of objects by sets of which are the inferences justified.
 */
public interface JustificationCompleteProof<C, I extends Inference<? extends C>, A> {

	/**
	 * @return The proof returned by {@link #getProof()} with the justifier
	 *         returned by {@link #getJustifier()} must be complete w.r.t.
	 *         justifications of this conclusion.
	 */
	C getQuery();

	/**
	 * @return Proof that, with the justifier returned by
	 *         {@link #getJustifier()}, is complete w.r.t. justifications of the
	 *         conclusion returned by {@link #getQuery()}.
	 * @throws ExperimentException
	 */
	Proof<? extends I> getProof() throws ExperimentException;

	/**
	 * @return The justifier with which the proof returned by
	 *         {@link #getProof()} must be complete w.r.t. justifications of the
	 *         conclusion returned by {@link #getQuery()}.
	 */
	InferenceJustifier<? super I, ? extends Set<? extends A>> getJustifier();

}
