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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;

import com.github.joergschwabe.experiments.ExperimentException;
import com.github.joergschwabe.proofs.JustificationCompleteProof;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

public final class Utils {

	private Utils() {
		// Empty.
	}

	public static boolean cleanDir(final File dir) {
		boolean success = true;
		if (dir.exists()) {
			success = recursiveDelete(dir) && success;
		}
		return dir.mkdirs() && success;
	}

	public static boolean cleanIfNotDir(final File dir) {
		boolean success = true;
		if (dir.exists()) {
			if (!dir.isDirectory()) {
				success = recursiveDelete(dir) && success;
			}
		} else {
			success = dir.mkdirs() && success;
		}
		return dir.mkdirs();
	}

	public static boolean recursiveDelete(final File file) {
		boolean success = true;
		if (file.isDirectory()) {
			for (final File f : file.listFiles()) {
				success = recursiveDelete(f) && success;
			}
		}
		return file.delete() && success;
	}

	public static String toFileName(final Object obj) {
		return obj.toString().replaceAll("[^a-zA-Z0-9_.-]", "_");
	}

	public static String sha1hex(final String str) {
		try {
			final MessageDigest md = MessageDigest.getInstance("SHA-1");
			final byte[] b = md.digest(str.getBytes());
			final StringBuilder result = new StringBuilder();
			for (int i = 0; i < b.length; i++) {
				result.append(Integer.toString((b[i] & 0xff) + 0x100, 16)
						.substring(1));
			}
			return result.toString();
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException();
		}
	}

	public static void closeQuietly(final Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (final IOException e) {
				// Ignore.
			}
		}
	}

	public static String dropExtension(final String fileName) {
		final int index = fileName.lastIndexOf('.');
		if (index < 0) {
			return fileName;
		} else {
			return fileName.substring(0, index);
		}
	}

	public static int digitCount(final int x) {
		if (x > 0) {
			return (int) Math.floor(Math.log10(x) + 1);
		} else if (x == 0) {
			return 1;
		} else {
			throw new IllegalArgumentException(
					"Argument must not be negative! x=" + x);
		}
	}

	public static <C, I extends Inference<? extends C>, A, IO, CO, AO> void traverseProofs(
			final JustificationCompleteProof<C, I, A> proof,
			final Function<? super I, IO> perInference,
			final Function<C, CO> perConclusion, final Function<A, AO> perAxiom)
			throws ExperimentException {
		traverseProofs(proof.getQuery(), proof.getProof(), proof.getJustifier(),
				perInference, perConclusion, perAxiom);
	}

	public static <C, I extends Inference<? extends C>, A, IO, CO, AO> void traverseProofs(
			final C expression, final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			final Function<? super I, IO> perInference,
			final Function<C, CO> perConclusion,
			final Function<A, AO> perAxiom) {

		final Queue<C> toDo = new LinkedList<>();
		final Set<C> done = new HashSet<>();

		toDo.add(expression);
		done.add(expression);

		for (;;) {
			final C next = toDo.poll();

			if (next == null) {
				break;
			}

			perConclusion.apply(next);

			for (final I inf : proof.getInferences(next)) {
				perInference.apply(inf);

				for (final A axiom : justifier.getJustification(inf)) {
					perAxiom.apply(axiom);
				}

				for (final C premise : inf.getPremises()) {
					if (done.add(premise)) {
						toDo.add(premise);
					}
				}
			}

		}
	}

	/**
	 * Checks if the given justification has a subset in the given collection of
	 * justifications
	 * 
	 * @param just
	 * @param justs
	 * @return {@code true} if the given justification is not a superset of any
	 *         justification in the given collection
	 */
	public static <J extends Set<?>> boolean isMinimal(J just,
			Collection<? extends J> justs) {
		for (J other : justs) {
			if (just.containsAll(other)) {
				return false;
			}
		}
		// otherwise
		return true;
	}

	/**
	 * Merges a given justification into a given collection of justifications.
	 * The justification is added to the collection unless its subset is already
	 * contained in the collection. Furthermore, all proper supersets of the
	 * justification are removed from the collection.
	 * 
	 * @param just
	 * @param justs
	 * @return {@code true} if the collection is modified as a result of this
	 *         operation and {@code false} otherwise
	 */
	public static <J extends Set<?>> boolean merge(J just,
			Collection<J> justs) {
		int justSize = just.size();
		final Iterator<J> oldJustIter = justs.iterator();
		boolean isASubsetOfOld = false;
		while (oldJustIter.hasNext()) {
			final J oldJust = oldJustIter.next();
			if (justSize < oldJust.size()) {
				if (oldJust.containsAll(just)) {
					// new justification is smaller
					oldJustIter.remove();
					isASubsetOfOld = true;
				}
			} else if (!isASubsetOfOld & just.containsAll(oldJust)) {
				// is a superset of some old justification
				return false;
			}
		}
		// justification survived all tests, it is minimal
		justs.add(just);
		return true;
	}

	/**
	 * FIXME: The order of arguments matter !!! The conclusion is copied from
	 * the justifications in the first argument, but not from the second!
	 * 
	 * @param first
	 * @param second
	 * @return the list of all pairwise unions of the justifications in the
	 *         first and the second collections, minimized under set inclusion
	 */
	public static <C, T> List<Justification<C, T>> join(
			Collection<? extends Justification<C, T>> first,
			Collection<? extends Justification<C, T>> second) {
		if (first.isEmpty() || second.isEmpty()) {
			return Collections.emptyList();
		}
		List<Justification<C, T>> result = new ArrayList<>(
				first.size() * second.size());
		for (Justification<C, T> firstSet : first) {
			for (Justification<C, T> secondSet : second) {
				Justification<C, T> union = firstSet.addElements(secondSet);
				merge(union, result);
			}
		}
		return result;
	}

	/**
	 * FIXME: The order of arguments matter !!! The conclusion is copied from
	 * the justifications in the first argument, but not from the second!
	 * 
	 * @param first
	 * @param second
	 * @return the list of all pairwise unions of the justifications in the
	 *         first and the second collections, minimized under set inclusion
	 */
	public static <C, T> List<Justification<C, T>> joinCheckingSubsets(
			Collection<? extends Justification<C, T>> first,
			Collection<? extends Justification<C, T>> second) {
		if (first.isEmpty() || second.isEmpty()) {
			return Collections.emptyList();
		}

		List<Justification<C, T>> result = new ArrayList<Justification<C, T>>(
				first.size() * second.size());
		/*
		 * If some set from one argument is a superset of something in the other
		 * argument, it can be merged into the result without joining it with
		 * anything from the other argument.
		 */
		final C conclusion = first.iterator().next().getConclusion();
		final List<Justification<C, T>> minimalSecond = new ArrayList<Justification<C, T>>(
				second.size());
		for (final Justification<C, T> secondSet : second) {
			if (isMinimal(secondSet, first)) {
				minimalSecond.add(secondSet);
			} else {
				merge(secondSet.copyTo(conclusion), result);
			}
		}

		for (Justification<C, T> firstSet : first) {
			if (isMinimal(firstSet, minimalSecond)) {
				for (Justification<C, T> secondSet : minimalSecond) {
					Justification<C, T> union = firstSet.addElements(secondSet);
					merge(union, result);
				}
			} else {
				merge(firstSet, result);
			}
		}

		return result;
	}

	public static class Counter implements Function<Object, Integer> {

		private int counter;

		public Counter() {
			this(0);
		}

		public Counter(final int first) {
			this.counter = first;
		}

		public int next() {
			return counter++;
		}

		@Override
		public Integer apply(final Object o) {
			return next();
		}

	}

	public static class Index<T> {

		private final Map<T, Integer> index = new HashMap<>();

		private final Function<? super T, Integer> newElement_;

		public Index(final Function<? super T, Integer> newElement) {
			Preconditions.checkNotNull(newElement);
			this.newElement_ = newElement;
		}

		public int get(final T element) {
			Integer result = index.get(element);
			if (result == null) {
				result = newElement_.apply(element);
				index.put(element, result);
			}
			return result;
		}

		public Map<T, Integer> getIndex() {
			return Collections.unmodifiableMap(index);
		}

	}

	public static class IndexRecorder<T> implements Function<T, Integer> {

		private final Function<? super T, Integer> newElement_;

		private final PrintWriter record_;

		public IndexRecorder(final Function<? super T, Integer> newElement,
				final PrintWriter record) {
			Preconditions.checkNotNull(newElement);
			this.newElement_ = newElement;
			Preconditions.checkNotNull(record);
			this.record_ = record;
		}

		@Override
		public Integer apply(final T newElement) {
			final Integer result = newElement_.apply(newElement);
			record_.print(result);
			record_.print(" ");
			record_.println(newElement);
			record_.flush();
			return result;
		}

	}

}
