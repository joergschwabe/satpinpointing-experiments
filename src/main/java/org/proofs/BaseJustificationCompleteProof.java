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

import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;

public class BaseJustificationCompleteProof<C, I extends Inference<? extends C>, A>
		implements JustificationCompleteProof<C, I, A> {

	private final C query_;
	private final Proof<? extends I> proof_;
	private final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier_;

	public BaseJustificationCompleteProof(final C query,
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier) {
		this.query_ = query;
		this.proof_ = proof;
		this.justifier_ = justifier;
	}

	@Override
	public C getQuery() {
		return query_;
	}

	@Override
	public Proof<? extends I> getProof() {
		return proof_;
	}

	@Override
	public InferenceJustifier<? super I, ? extends Set<? extends A>> getJustifier() {
		return justifier_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((justifier_ == null) ? 0 : justifier_.hashCode());
		result = prime * result + ((proof_ == null) ? 0 : proof_.hashCode());
		result = prime * result + ((query_ == null) ? 0 : query_.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		BaseJustificationCompleteProof other = (BaseJustificationCompleteProof) obj;
		if (justifier_ == null) {
			if (other.justifier_ != null) {
				return false;
			}
		} else if (!justifier_.equals(other.justifier_))
			return false;
		if (proof_ == null) {
			if (other.proof_ != null) {
				return false;
			}
		} else if (!proof_.equals(other.proof_))
			return false;
		if (query_ == null) {
			if (other.query_ != null) {
				return false;
			}
		} else if (!query_.equals(other.query_)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " of " + getQuery();
	}

}
