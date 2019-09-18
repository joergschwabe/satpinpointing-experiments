package com.github.joergschwabe;

import java.util.HashSet;
import java.util.Set;

import org.liveontologies.puli.Inference;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class IdProvider<A, I> {

	private final BiMap<Object, Integer> conclusionIds_ = HashBiMap.create();
	private final BiMap<Inference<? extends Integer>, Integer> inferenceIds_ = HashBiMap.create();
	private final BiMap<Integer, HashSet<Integer>> inferenceConclusionIds_ = HashBiMap.create();
	private final BiMap<A, Integer> axiomIds_ = HashBiMap.create();
	private int nextId_ = 1;

	int getConclusionId(Object conclusion) {
		Integer result = conclusionIds_.get(conclusion);
		if (result == null) {
			result = nextId_++;
			conclusionIds_.put(conclusion, result);
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

	void addConclusionInference(Inference<? extends Integer> inference) {
		Integer conclusionId = inference.getConclusion();
		Integer inferenceId = getInferenceId(inference);
		if(inferenceConclusionIds_.keySet().contains(conclusionId)) {
			inferenceConclusionIds_.get(conclusionId).add(inferenceId);
		} else {
			HashSet<Integer> inferenceSet = new HashSet<Integer>();
			inferenceSet.add(inferenceId);
			inferenceConclusionIds_.put(conclusionId, inferenceSet);			
		}
	}

	Set<Integer> getInferenceIds(Object conclusion) { 
		return inferenceConclusionIds_.get(conclusion);
	}
}
