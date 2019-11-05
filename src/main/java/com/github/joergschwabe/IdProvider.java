package com.github.joergschwabe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.liveontologies.puli.Inference;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class IdProvider<A, I> {

	private final BiMap<Object, Integer> conclusionIds_ = HashBiMap.create();
	private final BiMap<Inference<? extends Integer>, Integer> inferenceIds_ = HashBiMap.create();
	private final Map<Integer, HashSet<Integer>> inferenceConclusionIds_ = new HashMap<>();
	private final BiMap<A, Integer> axiomIds_ = HashBiMap.create();
	private int nextId_ = 1;

	int getConclusionId(Object conclusion) {
		Integer result = conclusionIds_.get(conclusion);
		if (result == null) {
			result = nextId_++;
			conclusionIds_.put(conclusion, result);
			inferenceConclusionIds_.put(result, new HashSet<Integer>());
		}
		return result;
	}

	int getJustificationId(A premise) {
		Integer result = axiomIds_.get(premise);
		if (result == null) {
			result = nextId_++;
			axiomIds_.put(premise, result);
		}
		return result;
	}

	Set<Integer> getConclusionIds() {
		return conclusionIds_.values();
	}

	Set<Integer> getAxiomIds() {
		return axiomIds_.values();
	}

	A getAxiomFromId(Integer axiomId) { 
		return axiomIds_.inverse().get(axiomId);
	}

	int getInferenceId(Inference<? extends Integer> inference) { 
		Integer result = inferenceIds_.get(inference);
		if (result == null) {
			result = nextId_++;
			inferenceIds_.put(inference, result);
		}
		return result;
	}

	Inference<? extends Integer> getInferenceFromId(Integer id) { 
		return inferenceIds_.inverse().get(id);
	}

	void addConclusionInference(Inference<? extends Integer> inference) {
		Integer conclusionId = inference.getConclusion();
		Integer inferenceId = getInferenceId(inference);
		inferenceConclusionIds_.get(conclusionId).add(inferenceId);
	}

	Set<Integer> getInferenceIds(Integer conclusion) { 
		return inferenceConclusionIds_.get(conclusion);
	}

	public Set<Integer> getInferenceIds() {
		return inferenceIds_.values();
	}
}
