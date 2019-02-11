package com.github.joergschwabe;

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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class ConclusionDepthComputation<C, I extends Inference<? extends C>> {

	private final static Logger LOGGER_ = LoggerFactory
			.getLogger(ConclusionDepthComputation.class);

	private final Proof<I> inferences_;

	private final Map<C, Integer> depth_ = new HashMap<>();

	private final ListMultimap<C, I> inferencesByDeepestPremise_ = ArrayListMultimap
			.create();

	private final Queue<C> toDo_ = new ArrayDeque<>();

	private final Queue<ConclusionDepth<C>> toPropagate_ = new ArrayDeque<>();

	private final Set<C> done_ = new HashSet<>();

	public ConclusionDepthComputation(Proof<I> inferences) {
		this.inferences_ = inferences;
	}

	public Integer getDepth(C conclusion) {
		todo(conclusion);
		process();
		Integer result = depth_.get(conclusion);
		return result;
	}

	private void todo(C c) {
		if (done_.add(c)) {
			toDo_.add(c);
		}
	}

	private void process() {
		for (;;) {
			C next = toDo_.poll();
			if (next == null) {
				break;
			}
			for (I inf : inferences_.getInferences(next)) {
				processInference(inf);
				for (C premise : inf.getPremises()) {
					todo(premise);
				}
			}
		}
		for (;;) {
			ConclusionDepth<C> next = toPropagate_.poll();
			if (next == null) {
				break;
			}
			Integer depth = depth_.get(next.conclusion);
			if (depth != null && depth <= next.depth) {
				continue;
			}
			// else
			depth_.put(next.conclusion, next.depth);
			for (I inf : inferencesByDeepestPremise_
					.removeAll(next.conclusion)) {
				processInference(inf);
			}
		}
	}

	void processInference(I inf) {
		C deepestPremise = null;
		int maxPremiseDepth = 0;
		for (C premise : inf.getPremises()) {
			Integer depth = depth_.get(premise);
			if (depth == null) {
				inferencesByDeepestPremise_.put(premise, inf);
				return;
			}
			if (depth > maxPremiseDepth) {
				deepestPremise = premise;
				maxPremiseDepth = depth;
			}
		}
		if (deepestPremise != null) {
			inferencesByDeepestPremise_.put(deepestPremise, inf);
		}
		todoDepth(inf.getConclusion(), maxPremiseDepth + 1);

	}

	public void todoDepth(C conclusion, int depth) {
		LOGGER_.debug("{}: new depth: {}", conclusion, depth);
		toPropagate_.add(new ConclusionDepth<C>(conclusion, depth));
	}

	private static class ConclusionDepth<C> {

		final C conclusion;

		int depth;

		ConclusionDepth(C conclusion, int depth) {
			this.conclusion = conclusion;
			this.depth = depth;
		}

	}

}
