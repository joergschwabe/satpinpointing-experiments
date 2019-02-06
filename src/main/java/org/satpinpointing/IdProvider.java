package org.satpinpointing;

/*-
 * #%L
 * Axiom Pinpointing Experiments
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2019 Live Ontologies Project
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

import java.util.HashSet;
import java.util.Set;

import org.liveontologies.puli.Inference;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class IdProvider<A, I> {

	private final BiMap<Object, Integer> conclusionIds_ = HashBiMap.create();
	private final BiMap<A, Integer> axiomIds_ = HashBiMap.create();
	private final BiMap<Inference<Integer>, I> inferencesMap_  = HashBiMap.create();
	private final BiMap<I, Set<Integer>> justificationIds_ = HashBiMap.create();
	private int nextId_ = 1;

	int getConclusionId(Object conclusion) {
		Integer result = conclusionIds_.get(conclusion);
		if (result == null) {
			result = nextId_++;
			conclusionIds_.put(conclusion, result);
		}
		return result;
	}

	int getAxiomId(A premise) {
		Integer result = axiomIds_.get(premise);
		if (result == null) {
			result = nextId_++;
			axiomIds_.put(premise, result);
		}
		return result;
	}

	int getNextId() {
		return nextId_++;
	}

	Set<Integer> getAxiomIds() {
		return axiomIds_.inverse().keySet();
	}

	A getAxiomFromId(Integer axiomId) { 
		return axiomIds_.inverse().get(axiomId);
	}

	Set<Integer> getJustificationIds(I inference) {
		return justificationIds_.get(inference);
	}

	public void setJustifications(I inference, Set<? extends A> justifications) {
		if(!justificationIds_.keySet().contains(inference)) {
			Set<Integer> axiomSet = new HashSet<>();
			for (A axiom : justifications) {
				axiomSet.add(getAxiomId(axiom));
			}
			justificationIds_.put(inference, axiomSet);
		}
	}

	public void setInferenceMap(I inference, Inference<Integer> translatedInference) {
		inferencesMap_.put(translatedInference, inference);
	}
	
	public I getInference(Inference<Integer> translatedInference) {
		return inferencesMap_.get(translatedInference);
	}
}
