package com.github.joergschwabe;

import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class IdProvider<A, I> {

	private final BiMap<Object, Integer> conclusionIds_ = HashBiMap.create();
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

	Set<Integer> getAxiomIds() {
		return axiomIds_.inverse().keySet();
	}

	A getAxiomFromId(Integer axiomId) { 
		return axiomIds_.inverse().get(axiomId);
	}
}
