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

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes strongly connected components of conclusions induced by the graph of
 * inferences for the given root conclusion: the conclusion of the inferences is
 * reachable from all premises of the inferences. The implementation uses the
 * standard linear time <a href=
 * "https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm"
 * >Tarjan's strongly connected component algorithm<a> . To prevent stack
 * overflow, recursive calls (usually used in the standard formulations) are
 * avoided by means of custom stacks.
 * 
 * @param <C>
 *            type of conclusions used in proofs
 * @param <I>
 *            type of inferences used in proofs
 * 
 * @author Yevgeny Kazakov
 *
 */
public class StronglyConnectedComponentsComputation<C, I extends Inference<? extends C>> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(StronglyConnectedComponentsComputation.class);

	/**
	 * the proof which induces the graph
	 */
	private Proof<? extends I> proof_;

	/**
	 * conclusions on the current path, assume to alternate with elements of
	 * {@link #inferenceStack_}
	 */
	private final Deque<ConclusionRecord<C, I>> conclusionStack_ = new LinkedList<>();

	/**
	 * inferences on the current path, assume to alternate with elements of
	 * {@link #conclusionStack_}
	 */
	private final Deque<InferenceRecord<C, I>> inferenceStack_ = new LinkedList<>();

	/**
	 * accumulates the current component candidates
	 */
	private final Deque<C> stack_ = new LinkedList<>();

	/**
	 * components of visited elements not in {@link #stack_} in topological
	 * order: child components come before parent components reachable from them
	 * by means of inferences
	 */
	private final List<List<C>> components_ = new ArrayList<>();

	/**
	 * the number to be assigned to the next visited element, increases with
	 * every assignment
	 */
	private int id_ = 0;

	/**
	 * serves two purposes: (1) for elements in {@link #stack_} represents the
	 * order in which elements are created so that the smallest element in
	 * {@link #stack_} can be found; (2) for created elements not on
	 * {@link #stack_} represents the component ID of the element, i.e., the
	 * index of the list in {@link #components_} in which the element occurs
	 */
	private final Map<C, Integer> index_ = new HashMap<>();

	/**
	 * assigns to elements on #stack_ the minimal identifier of the reachable
	 * element on on #stack_; used to identify cycles
	 */
	private final Map<C, Integer> lowlink_ = new HashMap<C, Integer>();

	public StronglyConnectedComponentsComputation(final Proof<? extends I> root,
			C conclusion) {
		this.proof_ = root;
		toDo(conclusion);
		process();
	}

	/**
	 * Computes strongly connected components of conclusions induced by the
	 * graph of inferences for the given root conclusion: the conclusion of the
	 * inferences is reachable from all premises of the inferences.
	 * 
	 * @param inferences
	 * @param root
	 * @return the {@link StronglyConnectedComponents} in which the components
	 *         are listed in the inference order: conclusions of inferences
	 *         appear in the same or letter components than the premises of the
	 *         inferences; root appears in the last component
	 */
	public static <C> StronglyConnectedComponents<C> computeComponents(
			final Proof<? extends Inference<? extends C>> inferences, C root) {
		StronglyConnectedComponentsComputation<C, ?> computation = new StronglyConnectedComponentsComputation<>(
				inferences, root);
		return new StronglyConnectedComponents<C>(computation.components_,
				computation.index_);
	}

	private void toDo(C conclusion) {
		index_.put(conclusion, id_);
		lowlink_.put(conclusion, id_);
		id_++;
		stack_.push(conclusion);
		conclusionStack_.push(new ConclusionRecord<>(conclusion, this));
		LOGGER_.trace("{}: conclusion pushed", conclusion);
	}

	private void toDo(final I inf) {
		inferenceStack_.push(new InferenceRecord<C, I>(inf));
		LOGGER_.trace("{}: inference pushed", inf);
	}

	private void process() {
		for (;;) {
			ConclusionRecord<C, I> conclRec = conclusionStack_.peek();
			if (conclRec == null) {
				return;
			}
			if (conclRec.inferenceIterator_.hasNext()) {
				// process the next inference
				toDo(conclRec.inferenceIterator_.next());
			} else {
				// conclusion is processed
				conclusionStack_.pop();
				LOGGER_.trace("{}: conclusion popped", conclRec.conclusion_);
				if (lowlink_.get(conclRec.conclusion_)
						.equals(index_.get(conclRec.conclusion_))) {
					// the smallest element of the component found, collect it
					List<C> component = new ArrayList<C>();
					int componentId = components_.size();
					for (;;) {
						C member = stack_.pop();
						component.add(member);
						lowlink_.remove(member);
						index_.put(member, componentId);
						if (member == conclRec.conclusion_) {
							// component is fully collected
							break;
						}
					}
					components_.add(component);
					LOGGER_.trace("component #{}: {}", componentId, component);
				}
			}
			InferenceRecord<C, I> infRec = inferenceStack_.peek();
			if (infRec == null) {
				return;
			}
			for (;;) {
				if (infRec.premiseIterator_.hasNext()) {
					C premise = infRec.premiseIterator_.next();
					if (index_.get(premise) == null) {
						// premise not yet processed
						toDo(premise);
						break;
					}
				} else {
					infRec = inferenceStack_.pop();
					LOGGER_.trace("{}: inference popped", infRec.inference_);
					// update conclusion id to the earliest of those reachable
					// on stack
					C conclusion = infRec.inference_.getConclusion();
					int conclusionLowLink = lowlink_.get(conclusion);
					for (C premise : infRec.inference_.getPremises()) {
						Integer premiseLowLink = lowlink_.get(premise);
						if (premiseLowLink != null
								&& premiseLowLink < conclusionLowLink) {
							// the premise is on the stack and reaches an
							// earlier element on the stack
							conclusionLowLink = premiseLowLink;
						}
					}
					lowlink_.put(conclusion, conclusionLowLink);
					break;
				}
			}
		}

	}

	private static class ConclusionRecord<C, I extends Inference<? extends C>> {

		private final C conclusion_;

		private final Iterator<? extends I> inferenceIterator_;

		ConclusionRecord(C conclusion,
				StronglyConnectedComponentsComputation<C, I> computation) {
			this.conclusion_ = conclusion;
			this.inferenceIterator_ = computation.proof_
					.getInferences(conclusion).iterator();
		}

	}

	private static class InferenceRecord<C, I extends Inference<? extends C>> {

		private final I inference_;

		private final Iterator<? extends C> premiseIterator_;

		InferenceRecord(final I inference) {
			this.inference_ = inference;
			this.premiseIterator_ = inference.getPremises().iterator();
		}

	}

}
