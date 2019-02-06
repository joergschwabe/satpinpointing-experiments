package org.proofs.adapters;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Inferences;
import org.liveontologies.puli.Proof;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class DirectSatEncodingProofAdapter
		implements Proof<Inference<Integer>> {

	public static DirectSatEncodingProofAdapter load(
			final InputStream assumptions, final InputStream cnf)
			throws IOException, NumberFormatException {

		final Set<Integer> axioms = new HashSet<Integer>();

		final BufferedReader axiomReader = new BufferedReader(
				new InputStreamReader(assumptions));
		readAxioms(axiomReader, axioms);

		final ListMultimap<Object, Inference<Integer>> inferences = ArrayListMultimap
				.create();

		final BufferedReader cnfReader = new BufferedReader(
				new InputStreamReader(cnf));
		String line;
		while ((line = cnfReader.readLine()) != null) {

			if (line.isEmpty() || line.startsWith("c")
					|| line.startsWith("p")) {
				continue;
			}

			final String[] literals = line.split("\\s");
			final List<Integer> premises = new ArrayList<Integer>(
					literals.length - 2);
			final List<Integer> justification = new ArrayList<Integer>(
					literals.length - 2);
			Integer conclusion = null;
			boolean terminated = false;
			for (int i = 0; i < literals.length; i++) {

				final int l = Integer.valueOf(literals[i]);
				if (l < 0) {
					final int premise = -l;
					if (axioms.contains(premise)) {
						justification.add(premise);
					} else {
						premises.add(premise);
					}
				} else if (l > 0) {
					if (conclusion != null) {
						throw new IOException(
								"Non-Horn clause! \"" + line + "\"");
					} else {
						conclusion = l;
					}
				} else {
					// l == 0
					if (i != literals.length - 1) {
						throw new IOException(
								"Clause terminated before the end of line! \""
										+ line + "\"");
					} else {
						terminated = true;
					}
				}

			}
			if (conclusion == null) {
				throw new IOException(
						"Clause has no positive literal! \"" + line + "\"");
			}
			if (!terminated) {
				throw new IOException(
						"Clause not terminated at the end of line! \"" + line
								+ "\"");
			}

			final Inference<Integer> inference = new DirectSatEncodingInference(
					conclusion, premises, ImmutableSet.copyOf(justification));
			inferences.put(conclusion, inference);
		}

		return new DirectSatEncodingProofAdapter(inferences);
	}

	private static void readAxioms(final BufferedReader axiomReader,
			final Set<Integer> axioms) throws IOException {

		final StringBuilder number = new StringBuilder();

		boolean readingNumber = false;

		int ch;
		while ((ch = axiomReader.read()) >= 0) {

			final int digit = Character.digit(ch, 10);
			if (digit < 0) {
				if (readingNumber) {
					// The number ended.
					final Integer n = Integer.valueOf(number.toString());
					if (n > 0) {
						axioms.add(n);
					}
					readingNumber = false;
				} else {
					// Still not reading any number.
				}
			} else {
				if (readingNumber) {
					// Have the next digit of a number.
					number.append(digit);
				} else {
					// The number started.
					number.setLength(0);
					number.append(digit);
					readingNumber = true;
				}
			}

		}

	}

	private final Multimap<Object, Inference<Integer>> inferences_;

	private DirectSatEncodingProofAdapter(
			final Multimap<Object, Inference<Integer>> inferences) {
		this.inferences_ = inferences;
	}

	@Override
	public Collection<Inference<Integer>> getInferences(
			final Object conclusion) {
		return inferences_.get(conclusion);
	}

	private static class DirectSatEncodingInference
			implements Inference<Integer> {

		private final Integer conclusion_;
		private final List<? extends Integer> premises_;
		private final Set<Integer> justification_;

		DirectSatEncodingInference(final Integer conclusion,
				final List<? extends Integer> premises,
				final Set<Integer> justification) {
			this.conclusion_ = conclusion;
			this.premises_ = premises;
			this.justification_ = justification;
		}

		@Override
		public Integer getConclusion() {
			return conclusion_;
		}

		@Override
		public List<? extends Integer> getPremises() {
			return premises_;
		}

		public Set<Integer> getJustification() {
			return justification_;
		}

		@Override
		public String toString() {
			return Inferences.toString(this);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ (conclusion_ == null ? 0 : conclusion_.hashCode());
			result = prime * result
					+ (premises_ == null ? 0 : premises_.hashCode());
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
			final DirectSatEncodingInference other = (DirectSatEncodingInference) obj;

			return (conclusion_ == null ? other.conclusion_ == null
					: conclusion_.equals(other.conclusion_))
					&& (premises_ == null ? other.premises_ == null
							: premises_.equals(other.premises_));
		}

		@Override
		public String getName() {
			return getClass().getSimpleName();
		}

	}

	public static final InferenceJustifier<Inference<? extends Integer>, ? extends Set<Integer>> JUSTIFIER = new InferenceJustifier<Inference<? extends Integer>, Set<Integer>>() {

		@Override
		public Set<Integer> getJustification(
				final Inference<? extends Integer> inference) {

			if (inference instanceof DirectSatEncodingInference) {
				return ((DirectSatEncodingInference) inference)
						.getJustification();
			}
			// else

			return Collections.emptySet();
		}

	};

}
