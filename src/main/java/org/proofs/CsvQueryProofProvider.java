package org.proofs;

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

import org.satpinpointing.experiments.CsvQueryDecoder;
import org.satpinpointing.experiments.ExperimentException;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.statistics.NestedStats;

public class CsvQueryProofProvider<Q, C, I extends Inference<? extends C>, A>
		implements ProofProvider<String, C, I, A> {

	private final CsvQueryDecoder.Factory<Q> decoder_;
	private final ProofProvider<Q, C, I, A> proofProvider_;

	public CsvQueryProofProvider(final CsvQueryDecoder.Factory<Q> decoder,
			final ProofProvider<Q, C, I, A> proofProvider) {
		this.decoder_ = decoder;
		this.proofProvider_ = proofProvider;
	}

	@NestedStats(name = "proof")
	public ProofProvider<Q, C, I, A> getProofProvider() {
		return proofProvider_;
	}

	@Override
	public JustificationCompleteProof<C, I, A> getProof(final String query)
			throws ExperimentException {
		return proofProvider_.getProof(CsvQueryDecoder.decode(query, decoder_));
	}

	@Override
	public void dispose() {
		proofProvider_.dispose();
	}

}
