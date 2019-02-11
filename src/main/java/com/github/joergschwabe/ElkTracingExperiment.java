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
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.statistics.Stats;
import org.semanticweb.elk.loading.AbstractAxiomLoader;
import org.semanticweb.elk.loading.AxiomLoader;
import org.semanticweb.elk.loading.ElkLoadingException;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkObject;
import org.semanticweb.elk.owl.interfaces.ElkSubClassOfAxiom;
import org.semanticweb.elk.owl.iris.ElkFullIri;
import org.semanticweb.elk.owl.visitors.ElkAxiomProcessor;
import org.semanticweb.elk.reasoner.Reasoner;
import org.semanticweb.elk.util.concurrent.computation.InterruptMonitor;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.joergschwabe.experiments.CsvQueryDecoder;
import com.github.joergschwabe.experiments.ExperimentException;
import com.github.joergschwabe.proofs.ElkProofProvider;
import com.github.joergschwabe.proofs.JustificationCompleteProof;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;

public class ElkTracingExperiment {

	private static final Logger LOG = LoggerFactory
			.getLogger(ElkTracingExperiment.class);

	public static void main(final String[] args) {

		if (args.length < 3) {
			LOG.error("Insufficient arguments!");
			System.exit(1);
		}

		final File recordFile = new File(args[0]);
		if (recordFile.exists()) {
			Utils.recursiveDelete(recordFile);
		}
		final int repetitionCount = Integer.valueOf(args[1]);
		final long warmUpTimeoutMillis = Long.valueOf(args[2]);
		final String ontologyFileName = args[3];
		final String queryFileName = args[4];

		final OWLOntologyManager manager = OWLManager
				.createOWLOntologyManager();

		InputStream ontologyIS = null;
		PrintWriter stats = null;

		try {

			final ElkProofProvider elkProofProvider = new ElkProofProvider(
					new File(ontologyFileName), manager);

			LOG.info("Warm up ...");
			long warmUpEndTimeMillis = System.currentTimeMillis()
					+ warmUpTimeoutMillis;
			int iteration = 0;
			while (warmUpEndTimeMillis < System.currentTimeMillis()) {
				run(elkProofProvider, queryFileName, iteration,
						new RecordProducer() {
							@Override
							public void produce(final Record record) {
								// Empty.
							}
						});
				iteration++;
			}
			LOG.info("... and now the real stuff B-)");

			final Set<String> queries = new LinkedHashSet<>();
			final ArrayListMultimap<String, Record> recordsPerQuery = ArrayListMultimap
					.create();
			for (int rep = 0; rep < repetitionCount; rep++) {
				LOG.info("Repetition {} of {}", rep + 1, repetitionCount);
				run(elkProofProvider, queryFileName, iteration,
						new RecordProducer() {
							@Override
							public void produce(final Record record) {
								queries.add(record.query);
								recordsPerQuery.put(record.query, record);
							}
						});
				iteration++;
			}

			stats = new PrintWriter(recordFile);
			// @formatter:off
			stats.print("query," + "nAxiomsInAllProofs,"
					+ "nConclusionsInAllProofs," + "nInferencesInAllProofs,"
					+ "time");
			// @formatter:on
			List<String> statNames = null;
			for (final String query : queries) {
				Integer nAxiomsInAllProofs = null;
				Integer nConclusionsInAllProofs = null;
				Integer nInferencesInAllProofs = null;
				final List<Double> times = new ArrayList<>();
				final ArrayListMultimap<String, Object> statistics = ArrayListMultimap
						.create();
				boolean firstIter = true;
				final List<Record> records = recordsPerQuery.get(query);
				for (final Record record : records) {
					times.add(record.time);
					for (final Map.Entry<String, Object> e : record.stats
							.entrySet()) {
						statistics.put(e.getKey(), e.getValue());
					}
					if (firstIter) {
						firstIter = false;
						nAxiomsInAllProofs = record.nAxiomsInAllProofs;
						nConclusionsInAllProofs = record.nConclusionsInAllProofs;
						nInferencesInAllProofs = record.nInferencesInAllProofs;
						continue;
					}
					// else
					if (nAxiomsInAllProofs != record.nAxiomsInAllProofs) {
						throw new RuntimeException("nAxiomsInAllProofs differs "
								+ nAxiomsInAllProofs + " "
								+ record.nAxiomsInAllProofs);
					}
					if (nConclusionsInAllProofs != record.nConclusionsInAllProofs) {
						throw new RuntimeException(
								"nConclusionsInAllProofs differs "
										+ nConclusionsInAllProofs + " "
										+ record.nConclusionsInAllProofs);
					}
					// @formatter:off
//					if (nInferencesInAllProofs != record.nInferencesInAllProofs) {
//						throw new RuntimeException(
//								"nInferencesInAllProofs differs "
//										+ nInferencesInAllProofs + " "
//										+ record.nInferencesInAllProofs);
//					}
					// @formatter:on
				}

				if (statNames == null) {
					statNames = new ArrayList<>(statistics.keySet());
					Collections.sort(statNames);
					for (final String name : statNames) {
						stats.print(",");
						stats.print(name);
					}
					stats.println();
					stats.flush();
				}

				stats.print("\"");
				stats.print(query);
				stats.print("\"");
				stats.print(",");
				stats.print(nAxiomsInAllProofs);
				stats.print(",");
				stats.print(nConclusionsInAllProofs);
				stats.print(",");
				stats.print(nInferencesInAllProofs);
				stats.print(",");
				Collections.sort(times);
				stats.print(median(times));
				for (final String name : statNames) {
					stats.print(",");
					final List<Double> values = new ArrayList<>();
					for (final Object value : statistics.get(name)) {
						values.add(Double.valueOf(value.toString()));
					}
					Collections.sort(values);
					stats.print(median(values));
				}
				stats.println();
				stats.flush();

			}

		} catch (final FileNotFoundException e) {
			LOG.error("File Not Found!", e);
			System.exit(2);
		} catch (final ExperimentException e) {
			LOG.error("Could not classify the ontology!", e);
			System.exit(2);
		} catch (final IOException e) {
			LOG.error("Error while reading the conclusion file!", e);
			System.exit(2);
		} finally {
			Utils.closeQuietly(ontologyIS);
			Utils.closeQuietly(stats);
		}

	}

	private static void run(final ElkProofProvider elkProofProvider,
			final String queryFileName, final int iteration,
			final RecordProducer producer)
			throws IOException, ExperimentException {

		BufferedReader queryReader = null;
		try {

			final Reasoner reasoner = elkProofProvider.getReasoner();
			final ElkObject.Factory factory = reasoner.getElkFactory();

			// Reset tracing state!
			reasoner.registerAxiomLoader(new AxiomLoader.Factory() {
				@Override
				public AxiomLoader getAxiomLoader(
						final InterruptMonitor interrupter) {
					return new AbstractAxiomLoader(interrupter) {

						private boolean isLoadingFinished_ = false;

						@Override
						public void load(final ElkAxiomProcessor axiomInserter,
								final ElkAxiomProcessor axiomDeleter)
								throws ElkLoadingException {

							final ElkAxiom axiom = factory.getSubClassOfAxiom(
									factory.getClass(
											new ElkFullIri("A" + iteration)),
									factory.getClass(
											new ElkFullIri("B" + iteration)));

							axiomInserter.visit(axiom);

							isLoadingFinished_ = true;
						}

						@Override
						public boolean isLoadingFinished() {
							return isLoadingFinished_;
						}

					};
				}
			});

			Stats.resetStats(reasoner);

			queryReader = new BufferedReader(new FileReader(queryFileName));

			String line;
			while ((line = queryReader.readLine()) != null) {

				final ElkSubClassOfAxiom query = CsvQueryDecoder.decode(line,
						new CsvQueryDecoder.Factory<ElkSubClassOfAxiom>() {

							@Override
							public ElkSubClassOfAxiom createQuery(
									final String subIri, final String supIri) {
								return factory.getSubClassOfAxiom(
										factory.getClass(
												new ElkFullIri(subIri)),
										factory.getClass(
												new ElkFullIri(supIri)));
							}

						});

				LOG.info("Collecting statistics for {} ...", query);

				final long startNanos = System.nanoTime();
				final JustificationCompleteProof<Object, Inference<Object>, ElkAxiom> proof = elkProofProvider
						.getProof(query);

				final Set<ElkAxiom> axioms = new HashSet<ElkAxiom>();
				final Set<Object> conclusions = new HashSet<Object>();
				final Set<Inference<Object>> inferences = new HashSet<Inference<Object>>();

				Utils.traverseProofs(proof,
						new Function<Inference<Object>, Void>() {
							@Override
							public Void apply(final Inference<Object> inf) {
								inferences.add(inf);
								return null;
							}
						}, new Function<Object, Void>() {
							@Override
							public Void apply(final Object expr) {
								conclusions.add(expr);
								return null;
							}
						}, new Function<ElkAxiom, Void>() {
							@Override
							public Void apply(final ElkAxiom axiom) {
								axioms.add(axiom);
								return null;
							}
						});

				final long runTimeNanos = System.nanoTime() - startNanos;
				LOG.info("... took {}s", runTimeNanos / 1000000000.0);

				// @formatter:off
				producer.produce(new Record(line, axioms.size(),
						conclusions.size(), inferences.size(),
						runTimeNanos / 1000000.0, Stats.copyIntoMap(reasoner)));
				// @formatter:on

			}
		} finally {
			Utils.closeQuietly(queryReader);
		}

	}

	private static class Record {

		public final String query;
		public final int nAxiomsInAllProofs;
		public final int nConclusionsInAllProofs;
		public final int nInferencesInAllProofs;
		public final double time;
		public final Map<String, Object> stats;

		public Record(final String query, final int nAxiomsInAllProofs,
				final int nConclusionsInAllProofs,
				final int nInferencesInAllProofs, final double time,
				final Map<String, Object> stats) {
			this.query = query;
			this.nAxiomsInAllProofs = nAxiomsInAllProofs;
			this.nConclusionsInAllProofs = nConclusionsInAllProofs;
			this.nInferencesInAllProofs = nInferencesInAllProofs;
			this.time = time;
			this.stats = stats;
		}

	}

	private static interface RecordProducer {

		void produce(Record record);

	}

	private static double median(final List<Double> numbers) {
		final int half = numbers.size() / 2;
		if (numbers.size() % 2 == 0) {
			return (numbers.get(half - 1) + numbers.get(half)) / 2.0;
		} else {
			return numbers.get(half);
		}
	}

}
