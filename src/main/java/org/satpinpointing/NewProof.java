package org.satpinpointing;

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



import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;

public class NewProof<I extends Inference<?>> implements Proof<I> {

	private Set<I> infSet;

	private final Map<Object, Collection<I>> cache_;
	
	public NewProof(Set<I> infSet){
		this.infSet = infSet;
		cache_ = new HashMap<Object, Collection<I>>();
	}
	
	public Collection<I> getInferences(Object conclusion) {
		Collection<I> result = cache_.get(conclusion);
		if (result != null) {
			return result;
		}
		result = new HashSet<I>();
		for (I inf : infSet) {
			if (conclusion.equals(inf.getConclusion())) {
				result.add(inf);
			}
		}
		return result;
	}

}
