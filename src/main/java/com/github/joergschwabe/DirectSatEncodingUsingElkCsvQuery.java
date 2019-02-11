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
package com.github.joergschwabe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.output.NullOutputStream;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkClassAxiom;
import org.semanticweb.elk.owl.interfaces.ElkObject;
import org.semanticweb.elk.owl.iris.ElkFullIri;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.joergschwabe.ConvertToElSatKrssInput.ElSatPrinterVisitor;
import com.github.joergschwabe.experiments.CsvQueryDecoder;
import com.github.joergschwabe.experiments.ExperimentException;
import com.github.joergschwabe.proofs.CsvQueryProofProvider;
import com.github.joergschwabe.proofs.ElkProofProvider;
import com.github.joergschwabe.proofs.JustificationCompleteProof;
import com.github.joergschwabe.proofs.ProofProvider;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

/**
 * Exports proofs to CNF files as produced by EL+SAT.
 * <p>
 * Proof of each query from the query file is exported into its query directory.
 * This query directory will be placed inside of the output directory and its
 * name will be derived from the query by {@link Utils#sha1hex(String)}.
 * <p>
 * Each conclusion and axiom occurring in a proof of a query is given a unique
 * positive integer that is called its atom. The files placed into the directory
 * of this query are:
 * <ul>
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_H} - header of the CNF file.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_CNF} - inferences encoded as clauses
 * where the premises are negated atom and the conclusion is positive atom.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_Q} - atom of the goal conclusion.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_QUESTION} - negated atom of the goal
 * conclusion followed by 0.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_QUERY} - query as read from the query
 * file.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_PPP} - atoms of axioms.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_PPP_G_U} - sorted atoms of axioms.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_ASSUMPTIONS} - atoms of axioms
 * separated by " " and followed by 0.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_ZZZ} - conclusions with their atoms.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_ZZZ_GCI} - GCI axioms with their
 * atoms.
 * <li>{@value #FILE_NAME}+{@value #SUFFIX_ZZZ_RI} - RI axioms with their atoms.
 * </ul>
 * 
 * @author Peter Skocovsky
 */
public class DirectSatEncodingUsingElkCsvQuery {

	public static final String FILE_NAME = "encoding";
	public static final String SUFFIX_H = ".h";
	public static final String SUFFIX_CNF = ".cnf";
	public static final String SUFFIX_Q = ".q";
	public static final String SUFFIX_QUESTION = ".question";
	public static final String SUFFIX_QUERY = ".query";
	public static final String SUFFIX_PPP = ".ppp";
	public static final String SUFFIX_PPP_G_U = ".ppp.g.u";
	public static final String SUFFIX_ASSUMPTIONS = ".assumptions";
	public static final String SUFFIX_ZZZ = ".zzz";
	public static final String SUFFIX_ZZZ_GCI = ".zzz.gci";
	public static final String SUFFIX_ZZZ_RI = ".zzz.ri";

	private static final Logger LOG_ = LoggerFactory
			.getLogger(DirectSatEncodingUsingElkCsvQuery.class);

	public static final String OPT_ONTOLOGY = "ontology";
	public static final String OPT_QUERIES = "queries";
	public static final String OPT_OUTDIR = "outdir";
	public static final String OPT_MINIMAL = "minimal";
	public static final String OPT_PROGRESS = "progress";

	public static class Options {
		@Arg(dest = OPT_ONTOLOGY)
		public File ontologyFile;
		@Arg(dest = OPT_QUERIES)
		public File queriesFile;
		@Arg(dest = OPT_OUTDIR)
		public File outDir;
		@Arg(dest = OPT_MINIMAL)
		public boolean minimal;
		@Arg(dest = OPT_PROGRESS)
		public boolean progress;
	}

	public static void main(final String[] args) {

		final ArgumentParser parser = ArgumentParsers
				.newArgumentParser(
						DirectSatEncodingUsingElkCsvQuery.class.getSimpleName())
				.description(
						"Export proofs into CNF files as produced by EL+SAT.");
		parser.addArgument(OPT_ONTOLOGY)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("ontology file");
		parser.addArgument(OPT_QUERIES)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("query file");
		parser.addArgument(OPT_OUTDIR).type(File.class)
				.help("output directory");
		parser.addArgument("--" + OPT_MINIMAL).action(Arguments.storeTrue())
				.help("generate only necessary files");
		parser.addArgument("--" + OPT_PROGRESS).action(Arguments.storeTrue())
				.help("print progress to stdout");

		final OWLOntologyManager manager = OWLManager
				.createOWLOntologyManager();

		BufferedReader queryReader = null;

		try {

			final Options opt = new Options();
			parser.parseArgs(args, opt);

			if (!Utils.cleanDir(opt.outDir)) {
				LOG_.error("Could not prepare the output directory!");
				System.exit(2);
			}

			final ElkProofProvider elkProofProvider = new ElkProofProvider(
					opt.ontologyFile, manager);
			final ElkObject.Factory factory = elkProofProvider.getReasoner()
					.getElkFactory();

			final CsvQueryDecoder.Factory<ElkAxiom> decoder = new CsvQueryDecoder.Factory<ElkAxiom>() {

				@Override
				public ElkAxiom createQuery(final String subIri,
						final String supIri) {
					return factory.getSubClassOfAxiom(
							factory.getClass(new ElkFullIri(subIri)),
							factory.getClass(new ElkFullIri(supIri)));
				}

			};
			final ProofProvider<String, Object, Inference<Object>, ElkAxiom> proofProvider = new CsvQueryProofProvider<>(
					decoder, elkProofProvider);

			queryReader = new BufferedReader(new FileReader(opt.queriesFile));
			int queryCount = 0;
			String line;
			while ((line = queryReader.readLine()) != null) {
				queryCount++;
			}
			queryReader.close();

			final Progress progress;
			if (opt.progress) {
				progress = new Progress(System.out, queryCount);
			} else {
				progress = new Progress(new PrintStream(new NullOutputStream()),
						queryCount);
			}

			queryReader = new BufferedReader(new FileReader(opt.queriesFile));

			int queryIndex = 0;
			while ((line = queryReader.readLine()) != null) {

				LOG_.debug("Encoding {} of {}: {}", queryIndex, queryCount,
						line);

				encode(line, proofProvider, opt.outDir, opt.minimal, queryCount,
						queryIndex++);

				progress.update();
			}

			progress.finish();

		} catch (final FileNotFoundException e) {
			LOG_.error("File Not Found!", e);
			System.exit(2);
		} catch (final ExperimentException e) {
			LOG_.error("Could not classify the ontology!", e);
			System.exit(2);
		} catch (final IOException e) {
			LOG_.error("I/O error!", e);
			System.exit(2);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		} finally {
			Utils.closeQuietly(queryReader);
		}

	}

	private static <C, I extends Inference<? extends C>, A> void encode(
			final String line,
			final ProofProvider<String, C, I, A> proofProvider,
			final File outputDirectory, final boolean minimal,
			final int queryCount, final int queryIndex)
			throws IOException, ExperimentException {

		final String queryName = Utils.sha1hex(line);
		// @formatter:off
//		final String queryName = Utils.toFileName(line);
//		final String queryName = String.format(
//				"%0" + Integer.toString(queryCount).length() + "d", queryIndex);
		// @formatter:on
		final File outDir = new File(outputDirectory, queryName);
		final File hFile = new File(outDir, FILE_NAME + SUFFIX_H);
		final File cnfFile = new File(outDir, FILE_NAME + SUFFIX_CNF);
		final File qFile = new File(outDir, FILE_NAME + SUFFIX_Q);
		final File questionFile = new File(outDir, FILE_NAME + SUFFIX_QUESTION);
		final File queryFile = new File(outDir, FILE_NAME + SUFFIX_QUERY);
		final File pppFile = new File(outDir, FILE_NAME + SUFFIX_PPP);
		final File pppguFile = new File(outDir, FILE_NAME + SUFFIX_PPP_G_U);
		final File assumptionsFile = new File(outDir,
				FILE_NAME + SUFFIX_ASSUMPTIONS);
		final File zzzFile = new File(outDir, FILE_NAME + SUFFIX_ZZZ);
		final File zzzgciFile = new File(outDir, FILE_NAME + SUFFIX_ZZZ_GCI);
		final File zzzriFile = new File(outDir, FILE_NAME + SUFFIX_ZZZ_RI);
		outDir.mkdirs();

		PrintWriter cnfWriter = null;
		PrintWriter hWriter = null;

		try {

			cnfWriter = new PrintWriter(cnfFile);
			hWriter = new PrintWriter(hFile);
			final PrintWriter cnf = cnfWriter;

			final JustificationCompleteProof<C, I, A> proof = proofProvider
					.getProof(line);
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier = proof
					.getJustifier();

			final Set<A> axioms = new HashSet<A>();
			final Set<C> conclusions = new HashSet<C>();

			Utils.traverseProofs(proof.getQuery(), proof.getProof(), justifier,
					Functions.<I> identity(), new Function<C, Void>() {
						@Override
						public Void apply(final C expr) {
							conclusions.add(expr);
							return null;
						}
					}, new Function<A, Void>() {
						@Override
						public Void apply(final A axiom) {
							axioms.add(axiom);
							return null;
						}
					});

			final Utils.Counter literalCounter = new Utils.Counter(1);
			final Utils.Counter clauseCounter = new Utils.Counter();

			final Map<A, Integer> axiomIndex = new HashMap<A, Integer>();
			for (final A axiom : axioms) {
				axiomIndex.put(axiom, literalCounter.next());
			}
			final Map<C, Integer> conclusionIndex = new HashMap<C, Integer>();
			for (final C conclusion : conclusions) {
				conclusionIndex.put(conclusion, literalCounter.next());
			}

			// cnf
			Utils.traverseProofs(proof.getQuery(), proof.getProof(), justifier,
					new Function<I, Void>() {
						@Override
						public Void apply(final I inf) {

							LOG_.trace("processing {}", inf);

							for (final A axiom : justifier
									.getJustification(inf)) {
								cnf.print(-axiomIndex.get(axiom));
								cnf.print(" ");
							}

							for (final C premise : inf.getPremises()) {
								cnf.print(-conclusionIndex.get(premise));
								cnf.print(" ");
							}

							cnf.print(conclusionIndex.get(inf.getConclusion()));
							cnf.println(" 0");
							clauseCounter.next();

							return null;
						}
					}, Functions.<C> identity(), Functions.<A> identity());

			final int lastLiteral = literalCounter.next();

			// h
			hWriter.println(
					"p cnf " + (lastLiteral - 1) + " " + clauseCounter.next());

			// ppp
			if (!minimal) {
				writeLines(axiomIndex.values(), pppFile);
			}

			// ppp.g.u
			final List<Integer> orderedAxioms = new ArrayList<Integer>(
					axiomIndex.values());
			Collections.sort(orderedAxioms);
			writeLines(orderedAxioms, pppguFile);

			// assumptions
			writeSpaceSeparated0Terminated(orderedAxioms, assumptionsFile);

			// q
			writeLines(Collections
					.singleton(conclusionIndex.get(proof.getQuery())), qFile);

			// question
			writeSpaceSeparated0Terminated(
					Collections
							.singleton(-conclusionIndex.get(proof.getQuery())),
					questionFile);

			// query
			if (!minimal) {
				writeLines(Collections.singleton(line), queryFile);
			}

			// zzz
			if (!minimal) {
				final SortedMap<Integer, A> gcis = new TreeMap<Integer, A>();
				final SortedMap<Integer, A> ris = new TreeMap<Integer, A>();
				for (final Map.Entry<A, Integer> entry : axiomIndex
						.entrySet()) {
					final A expr = entry.getKey();
					final int lit = entry.getValue();
					if (expr instanceof ElkClassAxiom) {
						gcis.put(lit, expr);
					} else {
						ris.put(lit, expr);
					}
				}
				final SortedMap<Integer, C> lemmas = new TreeMap<Integer, C>();
				for (final Map.Entry<C, Integer> entry : conclusionIndex
						.entrySet()) {
					lemmas.put(entry.getValue(), entry.getKey());
				}

				final Function<Map.Entry<Integer, A>, String> print = new Function<Map.Entry<Integer, A>, String>() {

					@Override
					public String apply(final Map.Entry<Integer, A> entry) {
						final StringBuilder result = new StringBuilder();
						result.append(entry.getKey()).append(" ");
						final A axiom = entry.getValue();
						if (axiom instanceof ElkAxiom) {
							((ElkAxiom) axiom)
									.accept(new ElSatPrinterVisitor(result));
							// Remove the last line end.
							result.setLength(result.length() - 1);
						} else {
							result.append(axiom);
						}
						return result.toString();
					}

				};
				writeLines(Iterables.transform(gcis.entrySet(), print),
						zzzgciFile);
				writeLines(Iterables.transform(ris.entrySet(), print),
						zzzriFile);
				writeLines(Iterables.transform(lemmas.entrySet(),
						new Function<Map.Entry<Integer, C>, String>() {
							@Override
							public String apply(
									final Map.Entry<Integer, C> entry) {
								final StringBuilder result = new StringBuilder();
								result.append(entry.getKey()).append(" ")
										.append(entry.getValue());
								return result.toString();
							}
						}), zzzFile);
			}

		} finally {
			Utils.closeQuietly(cnfWriter);
			Utils.closeQuietly(hWriter);
		}

	}

	private static void writeLines(final Iterable<?> lines, final File file)
			throws FileNotFoundException {

		PrintWriter writer = null;

		try {
			writer = new PrintWriter(file);

			for (final Object line : lines) {
				writer.println(line);
			}

		} finally {
			if (writer != null) {
				writer.close();
			}
		}

	}

	private static void writeSpaceSeparated0Terminated(
			final Iterable<?> iterable, final File file)
			throws FileNotFoundException {

		PrintWriter writer = null;

		try {
			writer = new PrintWriter(file);

			for (final Object object : iterable) {
				writer.print(object);
				writer.print(" ");
			}
			writer.print("0");

		} finally {
			if (writer != null) {
				writer.close();
			}
		}

	}

}
