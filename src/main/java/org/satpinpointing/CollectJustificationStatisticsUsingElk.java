package org.satpinpointing;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.satpinpointing.experiments.CsvQueryDecoder;
import org.satpinpointing.experiments.ExperimentException;
import org.proofs.CsvQueryProofProvider;
import org.proofs.ElkProofProvider;
import org.proofs.JustificationCompleteProof;
import org.proofs.ProofProvider;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkObject;
import org.semanticweb.elk.owl.iris.ElkFullIri;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

public class CollectJustificationStatisticsUsingElk {

	private static final Logger LOG = LoggerFactory
			.getLogger(CollectJustificationStatisticsUsingElk.class);

	public static final long WARMUP_TIMEOUT = 20000l;

	public static void main(final String[] args) {

		if (args.length < 3) {
			LOG.error("Insufficient arguments!");
			System.exit(1);
		}

		final File recordFile = new File(args[0]);
		if (recordFile.exists()) {
			Utils.recursiveDelete(recordFile);
		}
		final long timeOut = Long.parseLong(args[1]);
		final long globalTimeOut = Long.parseLong(args[2]);
		final int justificationSizeLimit = Integer.parseInt(args[3]);
		final String ontologyFileName = args[4];
		final String conclusionsFileName = args[5];

		final OWLOntologyManager manager = OWLManager
				.createOWLOntologyManager();

		final long timeOutCheckInterval = Math.min(timeOut / 4, 1000);

		BufferedReader conclusionReader = null;
		PrintWriter record = null;

		try {

			record = new PrintWriter(recordFile);
			record.println(
					"conclusion,didTimeOut,sumOfProductsOfPremiseJustNum,sumOfProductsOfInferenceJustNum,sumOfInferenceJustNum");

			final TimeOutMonitor monitor = new TimeOutMonitor();

			conclusionReader = new BufferedReader(
					new FileReader(conclusionsFileName));

			final BufferedReader conclReader = conclusionReader;
			final PrintWriter rec = record;
			final Thread worker = new Thread() {
				@Override
				public void run() {
					final long globalStartTime = System.currentTimeMillis();

					try {

						final ElkProofProvider elkProofProvider = new ElkProofProvider(
								new File(ontologyFileName), manager);
						final ElkObject.Factory factory = elkProofProvider
								.getReasoner().getElkFactory();

						final CsvQueryDecoder.Factory<ElkAxiom> decoder = new CsvQueryDecoder.Factory<ElkAxiom>() {

							@Override
							public ElkAxiom createQuery(final String subIri,
									final String supIri) {
								return factory.getSubClassOfAxiom(
										factory.getClass(
												new ElkFullIri(subIri)),
										factory.getClass(
												new ElkFullIri(supIri)));
							}

						};
						final ProofProvider<String, Object, Inference<Object>, ElkAxiom> proofProvider = new CsvQueryProofProvider<>(
								decoder, elkProofProvider);

						String line;
						while ((line = conclReader.readLine()) != null) {
							final long currentRunTime = System
									.currentTimeMillis() - globalStartTime;
							if (currentRunTime > globalTimeOut
									&& globalTimeOut != 0) {
								break;
							}

							System.gc();

							LOG.info("Collecting statistics for {}", line);
							if (globalTimeOut != 0) {
								LOG.info("{}s left until global timeout",
										(globalTimeOut - currentRunTime)
												/ 1000d);
							}

							monitor.cancelled = false;
							monitor.startTime.set(System.currentTimeMillis());

							final long startTime = System.nanoTime();
							final JustificationCompleteProof<Object, Inference<Object>, ElkAxiom> proof = proofProvider
									.getProof(line);

							final MinimalSubsetCollector<Object, Inference<Object>, ElkAxiom> collector = new MinimalSubsetCollector<>(
									BottomUpJustificationComputation
											.<Object, Inference<Object>, ElkAxiom> getFactory(),
									proof.getProof(), proof.getJustifier());

							final int sizeLimit = justificationSizeLimit <= 0
									? Integer.MAX_VALUE
									: justificationSizeLimit;

							final List<Long> productSum = Arrays.asList(0l);
							final List<Long> minProductSum = Arrays.asList(0l);
							final List<Long> minSum = Arrays.asList(0l);
							Utils.traverseProofs(proof,
									new Function<Inference<Object>, Void>() {
										@Override
										public Void apply(
												final Inference<Object> inf) {
											if (monitor.isInterrupted()) {
												return null;
											}

											final Collection<? extends Set<ElkAxiom>> conclJs = collector
													.collect(
															inf.getConclusion(),
															sizeLimit);

											long product = 1;
											long minProduct = 1;
											long sum = 0;
											for (final Object premise : inf
													.getPremises()) {

												final Collection<? extends Set<ElkAxiom>> js = collector
														.collect(premise,
																sizeLimit);

												product *= js.size();

												long count = 0;
												for (final Set<ElkAxiom> just : js) {
													if (Utils.isMinimal(
															new BloomSet<>(inf
																	.getConclusion(),
																	just,
																	proof.getJustifier()
																			.getJustification(
																					inf)),
															conclJs)) {
														count++;
													}
												}
												minProduct *= count;
												sum += count;

											}
											productSum.set(0, productSum.get(0)
													+ product);
											minProductSum.set(0,
													minProductSum.get(0)
															+ minProduct);
											minSum.set(0, minSum.get(0) + sum);

											return null;
										}
									}, Functions.<Object> identity(),
									Functions.<ElkAxiom> identity());

							final double time = (System.nanoTime() - startTime)
									/ 1000000.0;

							final boolean didTimeOut = (time > timeOut
									&& timeOut != 0);

							if (didTimeOut) {
								LOG.info("... timeout {}s", time / 1000.0);
							} else {
								LOG.info("... took {}s", time / 1000.0);
							}

							rec.print("\"");
							rec.print(line);
							rec.print("\",");
							rec.flush();
							rec.print(didTimeOut ? "TRUE" : "FALSE");
							rec.print(",");
							rec.print(productSum.get(0));
							rec.print(",");
							rec.print(minProductSum.get(0));
							rec.print(",");
							rec.println(minSum.get(0));
							rec.flush();

						}

					} catch (final IOException e) {
						throw new RuntimeException(e);
					} catch (final ExperimentException e) {
						throw new RuntimeException(e);
					}

				}
			};

			worker.start();
			while (worker.isAlive()) {
				try {
					Thread.sleep(timeOutCheckInterval);
				} catch (final InterruptedException e) {
					LOG.warn("Waiting for the worker thread interruptet!", e);
				}
				final long runTime = System.currentTimeMillis()
						- monitor.startTime.get();
				if (runTime > timeOut && timeOut != 0) {
					monitor.cancelled = true;
				}
			}

		} catch (final FileNotFoundException e) {
			LOG.error("File not found!", e);
			System.exit(2);
		} finally {
			Utils.closeQuietly(conclusionReader);
			Utils.closeQuietly(record);
		}

	}

	private static class TimeOutMonitor implements InterruptMonitor {

		public volatile boolean cancelled = false;
		public final AtomicLong startTime = new AtomicLong();

		@Override
		public boolean isInterrupted() {
			return cancelled;
		}

	}

}
