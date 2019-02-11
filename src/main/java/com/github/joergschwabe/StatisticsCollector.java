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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.statistics.Stats;
import org.semanticweb.elk.reasoner.indexing.model.IndexedContextRoot;
import org.semanticweb.elk.reasoner.saturation.conclusions.model.ClassConclusion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.joergschwabe.experiments.ExperimentException;
import com.github.joergschwabe.proofs.JustificationCompleteProof;
import com.github.joergschwabe.proofs.ProofProvider;
import com.github.joergschwabe.proofs.adapters.Proofs;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

public abstract class StatisticsCollector<O extends StatisticsCollector.Options, C, I extends Inference<? extends C>, A> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(StatisticsCollector.class);

	public static final String QUERIES_OPT = "queries";
	public static final String RECORD_OPT = "record";
	public static final String CONCLUSION_STATS_OPT = "cstats";
	public static final String QUERY_AGES_OPT = "qages";
	public static final String CYCLE_OPT = "cycle";
	public static final String COMPONENT_OPT = "component";

	public static class Options {
		@Arg(dest = QUERIES_OPT)
		public File queryFile;
		@Arg(dest = RECORD_OPT)
		public File recordFile;
		@Arg(dest = CONCLUSION_STATS_OPT)
		public File conclusionStatsFile;
		@Arg(dest = QUERY_AGES_OPT)
		public File queryAgesFile;
		@Arg(dest = CYCLE_OPT)
		public boolean detectCycle;
		@Arg(dest = COMPONENT_OPT)
		public boolean countComponents;
	}

	public final void collectStatistics(final String[] args) {

		final ArgumentParser parser = ArgumentParsers
				.newArgumentParser(StatisticsCollector.class.getSimpleName())
				.description("Collect proof statistics.");
		parser.addArgument(QUERIES_OPT)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("query file");
		parser.addArgument(RECORD_OPT).type(File.class).help("record file");
		parser.addArgument("--" + CONCLUSION_STATS_OPT).type(File.class)
				.help("collect conclusion statistics");
		parser.addArgument("--" + QUERY_AGES_OPT).type(File.class)
				.help("collect query ages");
		parser.addArgument("--" + CYCLE_OPT).action(Arguments.storeTrue())
				.help("check whether inferences contain a cycle");
		parser.addArgument("--" + COMPONENT_OPT).action(Arguments.storeTrue())
				.help("count strongly connected components in inferences");
		addArguments(parser);

		BufferedReader conclusionReader = null;
		PrintWriter stats = null;
		PrintWriter conclusionStatsWriter = null;
		PrintWriter queryAgeWriter = null;

		try {

			final O opt = newOptions();
			parser.parseArgs(args, opt);

			LOGGER_.info("queryFile: {}", opt.queryFile);
			if (opt.recordFile.exists()) {
				Utils.recursiveDelete(opt.recordFile);
			}
			LOGGER_.info("recordFile: {}", opt.recordFile);
			final Map<C, ConclusionStat> conclusionStats;
			if (opt.conclusionStatsFile == null) {
				conclusionStats = null;
			} else {
				if (opt.conclusionStatsFile.exists()) {
					Utils.recursiveDelete(opt.conclusionStatsFile);
				}
				conclusionStats = new HashMap<>();
			}
			LOGGER_.info("conclusionStatsFile: {}", opt.conclusionStatsFile);
			if (opt.queryAgesFile != null) {
				if (opt.queryAgesFile.exists()) {
					Utils.recursiveDelete(opt.queryAgesFile);
				}
			}
			LOGGER_.info("queryAgesFile: {}", opt.queryAgesFile);
			LOGGER_.info("detectCycle: {}", opt.detectCycle);
			LOGGER_.info("countComponents: {}", opt.countComponents);

			final ProofProvider<String, C, I, A> proofProvider = init(opt);

			stats = new PrintWriter(opt.recordFile);
			final Recorder recorder = new Recorder(stats);
			if (opt.queryAgesFile != null) {
				queryAgeWriter = new PrintWriter(opt.queryAgesFile);
				queryAgeWriter.println("queryAge");
			}

			conclusionReader = new BufferedReader(
					new FileReader(opt.queryFile));

			final Utils.Counter conclusionTicks = new Utils.Counter(
					Integer.MIN_VALUE);

			String line;
			while ((line = conclusionReader.readLine()) != null) {

				LOGGER_.info("Collecting statistics for {} ...", line);

				final Recorder.RecordBuilder record = recorder.newRecord();
				record.put("query", line);

				collectStatistics(line, proofProvider, record, opt.detectCycle,
						opt.countComponents, conclusionStats, conclusionTicks,
						queryAgeWriter);

				recorder.flush();

			}

			if (opt.conclusionStatsFile != null) {
				conclusionStatsWriter = new PrintWriter(
						opt.conclusionStatsFile);
				conclusionStatsWriter.println(
						"conclusion,nOccurrencesInDifferentProofs,queryTicks");
				for (final Map.Entry<C, ConclusionStat> e : conclusionStats
						.entrySet()) {
					conclusionStatsWriter.print("\"");
					conclusionStatsWriter.print(e.getKey());
					conclusionStatsWriter.print("\",");
					conclusionStatsWriter.print(e.getValue().nOccurrences);
					for (final Integer tick : e.getValue().queryTicks) {
						conclusionStatsWriter.print(",");
						conclusionStatsWriter.print(tick);
					}
					conclusionStatsWriter.println();
				}
			}

		} catch (final FileNotFoundException e) {
			LOGGER_.error("File Not Found!", e);
			System.exit(2);
		} catch (final ExperimentException e) {
			LOGGER_.error("Could not classify the ontology!", e);
			System.exit(2);
		} catch (final IOException e) {
			LOGGER_.error("Error while reading the conclusion file!", e);
			System.exit(2);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		} finally {
			Utils.closeQuietly(conclusionReader);
			Utils.closeQuietly(stats);
			Utils.closeQuietly(conclusionStatsWriter);
			Utils.closeQuietly(queryAgeWriter);
		}

	}

	/**
	 * @return New instance of a subclass of {@link Options} that adds fields
	 *         required by {@link #init(Options)}.
	 */
	protected abstract O newOptions();

	/**
	 * Adds arguments to the provided parser that populate fields required by
	 * {@link #init(Options)}.
	 * 
	 * @param parser
	 */
	protected abstract void addArguments(ArgumentParser parser);

	/**
	 * @param options
	 *            The object instantiated with {@link #newOptions()} and
	 *            populated by
	 *            {@link ArgumentParser#parseArgs(String[], Object)} called on
	 *            the parser passed to {@link #addArguments(ArgumentParser)}.
	 * @return The proof provider from whose proof the statistics should be
	 *         collected.
	 * @throws ExperimentException
	 */
	protected abstract ProofProvider<String, C, I, A> init(O options)
			throws ExperimentException;

	private void collectStatistics(final String query,
			final ProofProvider<String, C, I, A> proofProvider,
			final Recorder.RecordBuilder record, final boolean detectCycle,
			final boolean countComponents,
			final Map<C, ConclusionStat> conclusionStats,
			final Utils.Counter conclusionTicks,
			final PrintWriter queryAgeWriter) throws ExperimentException {

		final long startNanos = System.nanoTime();

		final JustificationCompleteProof<C, I, A> proof = proofProvider
				.getProof(query);

		final Set<A> axioms = new HashSet<A>();
		final Set<C> conclusions = new HashSet<C>();
		final Set<IndexedContextRoot> contexts = new HashSet<IndexedContextRoot>();
		final Set<I> inferences = new HashSet<I>();

		Utils.traverseProofs(proof, new Function<I, Void>() {
			@Override
			public Void apply(final I inf) {
				inferences.add(inf);
				return null;
			}
		}, new Function<C, Void>() {
			@Override
			public Void apply(final C expr) {
				final int currentTick = conclusionTicks.next();
				conclusions.add(expr);
				if (expr instanceof ClassConclusion) {
					contexts.add(((ClassConclusion) expr).getTraceRoot());

				}
				if (conclusionStats != null) {
					ConclusionStat stat = conclusionStats.get(expr);
					if (stat == null) {
						stat = new ConclusionStat();
						stat.nOccurrences = 0;
						conclusionStats.put(expr, stat);
					} else {
						if (queryAgeWriter != null) {
							final int queryAge = currentTick
									- stat.getLastQueryTick();
							queryAgeWriter.println(queryAge);
						}
					}
					stat.nOccurrences++;
					stat.queryTicks.add(currentTick);
				}
				return null;
			}
		}, new Function<A, Void>() {
			@Override
			public Void apply(final A axiom) {
				axioms.add(axiom);
				return null;
			}
		});

		record.put("nAxiomsInAllProofs", axioms.size());
		record.put("nConclusionsInAllProofs", conclusions.size());
		record.put("nInferencesInAllProofs", inferences.size());
		record.put("nContextsInAllProofs", contexts.size());

		if (detectCycle) {
			final boolean hasCycle = Proofs.hasCycle(proof.getProof(),
					proof.getQuery());
			record.put("isCycleInInferenceGraph", hasCycle);
		}

		if (countComponents) {
			final StronglyConnectedComponents<C> components = StronglyConnectedComponentsComputation
					.computeComponents(proof.getProof(), proof.getQuery());

			final List<List<C>> comps = components.getComponents();
			final List<C> maxComp = Collections.max(comps, SIZE_COMPARATOR);
			record.put("sizeOfMaxComponentInInferenceGraph", maxComp.size());

			final Collection<List<C>> nonSingletonComps = Collections2
					.filter(comps, new Predicate<List<C>>() {
						@Override
						public boolean apply(final List<C> comp) {
							return comp.size() > 1;
						}
					});
			record.put("nNonSingletonComponentsInInferenceGraph",
					nonSingletonComps.size());
		}

		final long runTimeNanos = System.nanoTime() - startNanos;
		LOGGER_.info("... took {}s", runTimeNanos / 1000000000.0);
		record.put("time", runTimeNanos / 1000000.0);

		final Runtime runtime = Runtime.getRuntime();
		final long totalMemory = runtime.totalMemory();
		final long usedMemory = totalMemory - runtime.freeMemory();
		record.put("usedMemory", usedMemory);

		final Map<String, Object> stats = Stats.copyIntoMap(proofProvider,
				new TreeMap<String, Object>());
		for (final Map.Entry<String, Object> entry : stats.entrySet()) {
			record.put(entry.getKey(), entry.getValue());
		}

	}

	private static final Comparator<Collection<?>> SIZE_COMPARATOR = new Comparator<Collection<?>>() {
		@Override
		public int compare(final Collection<?> o1, final Collection<?> o2) {
			return Integer.compare(o1.size(), o2.size());
		}
	};

	private static class ConclusionStat {
		public int nOccurrences;
		public List<Integer> queryTicks = new ArrayList<>();

		public int getLastQueryTick() {
			return queryTicks.get(queryTicks.size() - 1);
		}
	}

}
