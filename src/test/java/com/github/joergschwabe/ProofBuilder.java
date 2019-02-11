/*-
 * #%L
 * Proof Utility Library
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Live Ontologies Project
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
package com.github.joergschwabe;

import org.liveontologies.puli.BaseProof;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;

public class ProofBuilder<C> {

	private final BaseProof<Inference<C>> proof_ = new BaseProof<Inference<C>>();

	/**
	 * use {@link #create()}
	 */
	ProofBuilder() {
	}

	public static <C> ProofBuilder<C> create() {
		return new ProofBuilder<C>();
	}

	public Proof<? extends Inference<C>> build() {
		return proof_;
	}

	public ThisInferenceBuilder conclusion(C conclusion) {
		ThisInferenceBuilder result = new ThisInferenceBuilder();
		result.conclusion(conclusion);
		return result;
	}

	public class ThisInferenceBuilder extends InferenceBuilder<C> {

		protected ThisInferenceBuilder() {
			super(INF_NAME);
		}

		@Override
		ThisInferenceBuilder conclusion(C conclusion) {
			super.conclusion(conclusion);
			return this;
		}

		@Override
		public ThisInferenceBuilder premise(C premise) {
			super.premise(premise);
			return this;
		}

		public Inference<C> add() {
			Inference<C> inference = build();
			proof_.produce(inference);
			return inference;
		}

	}

}
