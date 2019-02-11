package com.github.joergschwabe.proofs.adapters;

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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.liveontologies.puli.Delegator;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Inferences;
import org.liveontologies.puli.Proof;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * A proof containing inferences with at most two premises, obtained from
 * original proof by binarization. Premises and conclusions of the binarized
 * inferences are lists of premises and conclusions of the original inferences.
 * It is guaranteed that if one can derive a conclusion {@code C} using a set of
 * axioms in justificaiton of inferences, then using the same justification one
 * can derive the conlcusion {@code [C]}, that is, the singleton list of
 * {@code [C]}.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <C>
 *            the type of conclusion and premises used by the original
 *            inferences
 * @param <I>
 *            the type of inferences used in proofs
 */
class BinarizedProofAdapter<C, I extends Inference<? extends C>>
		implements Proof<Inference<List<C>>> {

	private final Proof<? extends I> original_;

	BinarizedProofAdapter(final Proof<? extends I> original) {
		this.original_ = original;
	}

	@Override
	public Collection<? extends Inference<List<C>>> getInferences(
			final Object conclusion) {
		if (conclusion instanceof List<?>) {
			List<?> conclusionList = (List<?>) conclusion;
			switch (conclusionList.size()) {
			case 0:
				return Collections.emptyList();
			case 1:
				Object member = conclusionList.get(0);
				return Collections2.transform(original_.getInferences(member),
						ToBinaryInference.<C, I> get());
			default:
				List<C> originalConclusions = new ArrayList<C>(
						conclusionList.size());
				for (int i = 0; i < conclusionList.size(); i++) {
					C originalConclusion = null;
					for (I inf : original_
							.getInferences(conclusionList.get(i))) {
						originalConclusion = inf.getConclusion();
						break;
					}
					if (originalConclusion == null) {
						return Collections.emptySet();
					}
					// else
					originalConclusions.add(originalConclusion);
				}
				Inference<List<C>> inf = new BinaryListInference<C>(
						originalConclusions);
				return Collections.singleton(inf);
			}
		}
		// else
		return Collections.emptySet();
	}

	/**
	 * An inference producing a list from the singleton list of the first
	 * element and the sublist of the remaining elements.
	 * 
	 * @author Yevgeny Kazakov
	 *
	 * @param <C>
	 */
	private static class BinaryListInference<C> extends Delegator<List<C>>
			implements Inference<List<C>> {

		public BinaryListInference(final List<C> conclusion) {
			super(conclusion);
			if (conclusion.size() <= 1) {
				throw new IllegalArgumentException();
			}
		}

		@Override
		public List<C> getConclusion() {
			return getDelegate();
		}

		@Override
		public List<? extends List<C>> getPremises() {
			List<List<C>> result = new ArrayList<List<C>>(2);
			result.add(Collections.singletonList(getDelegate().get(0)));
			result.add(getDelegate().subList(1, getDelegate().size()));
			return result;
		}

		@Override
		public String toString() {
			return Inferences.toString(this);
		}

		@Override
		public String getName() {
			return getClass().getSimpleName();
		}

	}

	private static class ToBinaryInference<C, I extends Inference<? extends C>>
			implements Function<I, Inference<List<C>>> {

		private static final ToBinaryInference<?, ?> INSTANCE_ = new ToBinaryInference<>();

		@Override
		public Inference<List<C>> apply(final I input) {
			return new BinaryInferenceAdapter<C, I>(input);
		}

		@SuppressWarnings("unchecked")
		static <C, I extends Inference<? extends C>> Function<I, Inference<List<C>>> get() {
			return (ToBinaryInference<C, I>) INSTANCE_;
		}

	}

	private static class BinaryInferenceAdapter<C, I extends Inference<? extends C>>
			extends Delegator<I> implements Inference<List<C>> {

		BinaryInferenceAdapter(final I original) {
			super(original);
		}

		@Override
		public List<C> getConclusion() {
			return Collections.singletonList((C) getDelegate().getConclusion());
		}

		@Override
		public List<? extends List<C>> getPremises() {
			List<? extends C> originalPremises = getDelegate().getPremises();
			int originalPremiseCount = originalPremises.size();
			switch (originalPremiseCount) {
			case 0:
				return Collections.emptyList();
			case 1:
				return Collections.singletonList(
						Collections.<C> singletonList(originalPremises.get(0)));
			default:
				List<C> firstPremise = null, secondPremise = new ArrayList<C>(
						originalPremiseCount - 1);
				boolean first = true;
				for (C premise : originalPremises) {
					if (first) {
						first = false;
						firstPremise = Collections.singletonList(premise);
					} else {
						secondPremise.add(premise);
					}
				}
				List<List<C>> result = new ArrayList<List<C>>(2);
				result.add(firstPremise);
				result.add(secondPremise);
				return result;
			}
		}

		@Override
		public String toString() {
			return Inferences.toString(this);
		}

		@Override
		public String getName() {
			return getDelegate().getName();
		}

	}

	static class Justifier<C, I extends Inference<? extends C>, A> implements
			InferenceJustifier<Inference<List<C>>, Set<? extends A>> {

		private final InferenceJustifier<? super I, ? extends Set<? extends A>> original_;

		Justifier(
				final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier) {
			this.original_ = justifier;
		}

		@Override
		public Set<? extends A> getJustification(
				final Inference<List<C>> inference) {
			if (!(inference instanceof BinaryInferenceAdapter)) {
				return Collections.emptySet();
			}
			// else
			@SuppressWarnings("unchecked") // TODO: any way to avoid cast?
			final BinaryInferenceAdapter<C, I> binaryInference = (BinaryInferenceAdapter<C, I>) inference;
			return original_.getJustification(binaryInference.getDelegate());
		}

	}

}
