package com.github.joergschwabe;

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

import java.util.List;
import java.util.Set;

import org.liveontologies.puli.BaseInference;
import org.liveontologies.puli.Inferences;

public class JustifiedInference<C, A> extends BaseInference<C> {

	final Set<A> justification_;

	public JustifiedInference(final String name, final C conclusion,
			final List<? extends C> premises, final Set<A> axioms) {
		super(name, conclusion, premises);
		this.justification_ = axioms;
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof JustifiedInference<?, ?>) {
			return super.equals(o) && justification_
					.equals(((JustifiedInference<?, ?>) o).justification_);
		}
		// else
		return false;
	}

	@Override
	public synchronized int hashCode() {
		if (hash == 0) {
			hash = Inferences.hashCode(this) + justification_.hashCode();
		}
		return hash;
	}

	public Set<A> getJustification() {
		return justification_;
	}

}