package org.satpinpointing.experiments;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.satpinpointing.RunJustificationExperiments;
import org.satpinpointing.Utils;
import org.proofs.JustificationCompleteProof;
import org.proofs.ProofProvider;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.statistics.NestedStats;
import org.liveontologies.puli.statistics.Stat;
import org.liveontologies.puli.statistics.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

public abstract class BaseJustificationExperiment<O extends BaseJustificationExperiment.Options, C, I extends Inference<? extends C>, A>
		extends AbstractJustificationExperiment {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(BaseJustificationExperiment.class);

	public static final String INDEX_FILE_NAME = "axiom_index";

	public static final String SAVE_OPT = "s";

	public static class Options {
		@Arg(dest = SAVE_OPT)
		public File outputDir;
	}

	private File outputDir_;
	private PrintWriter indexWriter_;
	private Utils.Index<A> axiomIndex_;

	private MinimalSubsetEnumerator.Factory<C, A> computation_ = null;
	private String lastQuery_ = null;
	private ProofProvider<String, C, I, A> proofProvider_ = null;
	private JustificationCompleteProof<C, I, A> proof_;
	private volatile long runStartTimeNanos_;

	private JustificationCounter justificationListener_;

	// Statistics
	private int minJustSizeize_, maxJustSize_;
	private double obtainingInferencesTimeMillis_;
	private double firstQuartileJustSize_, medianJustSize_, meanJustSize_,
			thirdQuartileJustSize_;
	@Stat
	public String just1Time;
	@Stat
	public String just2Time;
	@Stat
	public String justHalfTime;

	@Override
	public final void init(final String[] args) throws ExperimentException {

		final ArgumentParser parser = ArgumentParsers
				.newArgumentParser(getClass().getSimpleName());
		parser.addArgument("-" + SAVE_OPT).type(File.class).help(
				"if provided, save justification into specified directory");

		addArguments(parser);

		try {

			final O options = newOptions();
			parser.parseArgs(args, options);

			LOGGER_.info("outputDir: {}", options.outputDir);
			this.outputDir_ = options.outputDir;
			if (outputDir_ == null) {
				this.justificationListener_ = new JustificationCounter();
				this.indexWriter_ = null;
				this.axiomIndex_ = null;
			} else {
				Utils.cleanDir(outputDir_);
				this.justificationListener_ = new JustificationCollector();
				this.indexWriter_ = new PrintWriter(new FileWriter(
						new File(outputDir_, INDEX_FILE_NAME), true));
				this.axiomIndex_ = new Utils.Index<>(new Utils.IndexRecorder<>(
						new Utils.Counter(1), indexWriter_));
			}

			init(options);
			proofProvider_ = newProofProvider();

		} catch (final SecurityException e) {
			throw new ExperimentException(e);
		} catch (final IOException e) {
			throw new ExperimentException(e);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		}

	}

	/**
	 * Adds arguments to the provided parser that populate fields required by
	 * {@link #init(Options)}.
	 * 
	 * @param parser
	 */
	protected abstract void addArguments(ArgumentParser parser);

	/**
	 * @return New instance of a subclass of {@link Options} that adds fields
	 *         required by {@link #init(Options)}.
	 */
	protected abstract O newOptions();

	/**
	 * @param options
	 *            The object instantiated with {@link #newOptions()} and
	 *            populated by
	 *            {@link ArgumentParser#parseArgs(String[], Object)} called on
	 *            the parser passed to {@link #addArguments(ArgumentParser)}.
	 * @throws ExperimentException
	 */
	protected abstract void init(O options) throws ExperimentException;

	/**
	 * Called after {@link #init(Options)}.
	 * 
	 * @return The proof provider used for the experiments.
	 * @throws ExperimentException
	 */
	protected abstract ProofProvider<String, C, I, A> newProofProvider()
			throws ExperimentException;

	/**
	 * Called after {@link #init(Options)}.
	 * 
	 * @param proof
	 * @param justifier
	 * @param monitor
	 * @return The computation used for the experiments.
	 * @throws ExperimentException
	 */
	protected abstract MinimalSubsetEnumerator.Factory<C, A> newComputation(
			Proof<? extends I> proof,
			InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			InterruptMonitor monitor) throws ExperimentException;

	@Override
	public void before(final String query) throws ExperimentException {
		justificationListener_.reset();
		resetStats();
		if (computation_ != null) {
			Stats.resetStats(computation_);
		}
		if (proofProvider_ != null) {
			Stats.resetStats(proofProvider_);
		}

		lastQuery_ = query;

		final long startTimeNanos = System.nanoTime();
		proof_ = proofProvider_.getProof(query);
		obtainingInferencesTimeMillis_ = (System.nanoTime() - startTimeNanos)
				/ RunJustificationExperiments.NANOS_IN_MILLIS;

	}

	@Override
	public void run(final InterruptMonitor monitor) throws ExperimentException {
		runStartTimeNanos_ = System.nanoTime();

		computation_ = newComputation(proof_.getProof(), proof_.getJustifier(),
				monitor);
		computation_.newEnumerator(proof_.getQuery())
				.enumerate(justificationListener_);

	}

	@Override
	public void after() throws ExperimentException {

		computeJustStats(justificationListener_.getSizes());

		final List<Long> justTimes = justificationListener_.getTimes();
		if (justTimes != null && justTimes.size() >= 1) {
			just1Time = "" + ((justTimes.get(0) - runStartTimeNanos_)
					/ RunJustificationExperiments.NANOS_IN_MILLIS);
			if (justTimes.size() >= 2) {
				just2Time = "" + ((justTimes.get(1) - runStartTimeNanos_)
						/ RunJustificationExperiments.NANOS_IN_MILLIS);
			}
			final int halfIndex;
			final int size = justTimes.size();
			if (size % 2 == 0) {
				halfIndex = size / 2 - 1;
			} else {
				halfIndex = size / 2;
			}
			justHalfTime = "" + ((justTimes.get(halfIndex) - runStartTimeNanos_)
					/ RunJustificationExperiments.NANOS_IN_MILLIS);
		}

		if (outputDir_ == null) {
			return;
		}
		// else

		final Collection<Set<A>> justs = justificationListener_
				.getJustifications();

		PrintWriter out = null;
		try {

			out = new PrintWriter(
					new File(outputDir_, Utils.toFileName(lastQuery_)));

			for (final Set<A> just : justs) {
				for (final A axiom : just) {
					out.print(axiomIndex_.get(axiom));
					out.print(" ");
				}
				out.println();
			}

		} catch (final FileNotFoundException e) {
			LOGGER_.error(e.getMessage(), e);
		} finally {
			Utils.closeQuietly(out);
		}

	}

	@Override
	public void dispose() {
		Utils.closeQuietly(indexWriter_);
		if (proofProvider_ != null) {
			proofProvider_.dispose();
		}
	}

	@NestedStats(name = "justificationComputation")
	public MinimalSubsetEnumerator.Factory<C, A> getJustificationComputation() {
		return computation_;
	}

	@NestedStats(name = "proofProvider")
	public ProofProvider<String, C, I, A> getProofProvider() {
		return proofProvider_;
	}

	private class JustificationCounter
			implements MinimalSubsetEnumerator.Listener<A> {

		private final List<Integer> justSizes_ = new ArrayList<>();
		private final List<Long> justTimes_ = new ArrayList<>();

		@Override
		public void newMinimalSubset(final Set<A> justification) {
			fireNewJustification();
			justTimes_.add(System.nanoTime());
			justSizes_.add(justification.size());
		}

		public Collection<Set<A>> getJustifications() {
			return null;
		}

		public List<Integer> getSizes() {
			return justSizes_;
		}

		public List<Long> getTimes() {
			return justTimes_;
		}

		public void reset() {
			justSizes_.clear();
			justTimes_.clear();
		}

	}

	private class JustificationCollector extends JustificationCounter {

		private final Collection<Set<A>> justifications_ = new ArrayList<>();

		@Override
		public void newMinimalSubset(final Set<A> justification) {
			super.newMinimalSubset(justification);
			justifications_.add(justification);
		}

		public Collection<Set<A>> getJustifications() {
			return justifications_;
		}

		@Override
		public void reset() {
			super.reset();
			justifications_.clear();
		}

	}

	@Stat
	public int minJustSize() {
		return minJustSizeize_;
	}

	@Stat
	public int maxJustSize() {
		return maxJustSize_;
	}

	@Stat
	public double obtainingInferencesTime() {
		return obtainingInferencesTimeMillis_;
	}

	@Stat
	public double firstQuartileJustSize() {
		return firstQuartileJustSize_;
	}

	@Stat
	public double medianJustSize() {
		return medianJustSize_;
	}

	@Stat
	public double meanJustSize() {
		return meanJustSize_;
	}

	@Stat
	public double thirdQuartileJustSize() {
		return thirdQuartileJustSize_;
	}

	private void resetStats() {
		minJustSizeize_ = maxJustSize_ = 0;
		firstQuartileJustSize_ = medianJustSize_ = meanJustSize_ = thirdQuartileJustSize_ = 0.0;
		just1Time = just2Time = justHalfTime = "";
	}

	private void computeJustStats(final List<Integer> sizes) {

		if (sizes == null || sizes.isEmpty()) {
			resetStats();
			return;
		}
		// else

		Collections.sort(sizes);

		minJustSizeize_ = sizes.get(0);
		maxJustSize_ = sizes.get(sizes.size() - 1);

		firstQuartileJustSize_ = firstQuartile(sizes);
		medianJustSize_ = median(sizes);
		meanJustSize_ = mean(sizes);
		thirdQuartileJustSize_ = thirdQuartile(sizes);

	}

	private double median(final List<Integer> numbers) {
		final int half = numbers.size() / 2;
		if (numbers.size() % 2 == 0) {
			return (numbers.get(half - 1) + numbers.get(half)) / 2.0;
		} else {
			return numbers.get(half);
		}
	}

	private double firstQuartile(final List<Integer> numbers) {
		final int half = numbers.size() / 2;
		if (numbers.size() % 2 == 0) {
			return median(numbers.subList(0, half));
		} else {
			return median(numbers.subList(0, half + 1));
		}
	}

	private double thirdQuartile(final List<Integer> numbers) {
		final int half = numbers.size() / 2;
		return median(numbers.subList(half, numbers.size()));
	}

	private double mean(final List<Integer> numbers) {
		int sum = 0;
		for (final Integer number : numbers) {
			sum += number;
		}
		return sum / (double) numbers.size();
	}

}
