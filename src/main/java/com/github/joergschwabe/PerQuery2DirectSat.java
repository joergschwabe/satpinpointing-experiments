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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

public class PerQuery2DirectSat {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(PerQuery2DirectSat.class);

	public static final String OPT_INPUT_DIR = "input";
	public static final String OPT_OUTPU_DIR = "output";

	public static class Options {
		@Arg(dest = OPT_INPUT_DIR)
		public File inDir;
		@Arg(dest = OPT_OUTPU_DIR)
		public File outDir;
	}

	public static void main(final String[] args) {

		final ArgumentParser parser = ArgumentParsers
				.newArgumentParser(PerQuery2DirectSat.class.getSimpleName())
				.description("Convert *.perQuery to direct SAT encoding.");
		parser.addArgument(OPT_INPUT_DIR)
				.type(Arguments.fileType().verifyExists().verifyIsDirectory())
				.help("input directory with *.perQuery encoding");
		parser.addArgument(OPT_OUTPU_DIR).type(File.class)
				.help("output directory");

		try {

			final Options opt = new Options();
			parser.parseArgs(args, opt);

			if (!Utils.cleanDir(opt.outDir)) {
				LOGGER_.error("Could not prepare the output directory!");
				System.exit(2);
			}

			final String[] perQueryDirs = opt.inDir.list();
			Arrays.sort(perQueryDirs);
			for (final String perQueryDir : perQueryDirs) {

				try {
					transform(new File(opt.inDir, perQueryDir), opt.outDir);
				} catch (final IOException e) {
					LOGGER_.error("I/O error!", e);
				} catch (final NumberFormatException e) {
					LOGGER_.error("I/O error!", e);
				}

			}

		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		}

	}

	private static void transform(final File perQueryDir, final File outDir)
			throws IOException {

		BufferedReader queryReader = null;
		BufferedReader questionReader = null;
		BufferedReader assumptionsReader = null;
		BufferedReader cnfReader = null;

		PrintWriter qWriter = null;
		PrintWriter pppguWriter = null;
		PrintWriter hWriter = null;

		try {

			// Get the query

			final File queryFile = getFirstFileWithSuffix(perQueryDir,
					DirectSatEncodingUsingElkCsvQuery.SUFFIX_QUERY);

			queryReader = new BufferedReader(new FileReader(queryFile));
			final String query = queryReader.readLine();

			LOGGER_.info("transforming query: {}", query);

			// Create output directory

			final File queryDir = new File(outDir, Utils.sha1hex(query));
			queryDir.mkdirs();

			// Copy *.cnf file

			final File cnfFile = getFirstFileWithSuffix(perQueryDir,
					DirectSatEncodingUsingElkCsvQuery.SUFFIX_CNF);

			Files.copy(cnfFile.toPath(), new File(queryDir,
					DirectSatEncodingUsingElkCsvQuery.FILE_NAME
							+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_CNF)
									.toPath(),
					StandardCopyOption.REPLACE_EXISTING);

			// Copy *.question file

			final File questionFile = getFirstFileWithSuffix(perQueryDir,
					DirectSatEncodingUsingElkCsvQuery.SUFFIX_QUESTION);

			Files.copy(questionFile.toPath(), new File(queryDir,
					DirectSatEncodingUsingElkCsvQuery.FILE_NAME
							+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_QUESTION)
									.toPath(),
					StandardCopyOption.REPLACE_EXISTING);

			// Copy *.assumptions file

			final File assumptionsFile = getFirstFileWithSuffix(perQueryDir,
					DirectSatEncodingUsingElkCsvQuery.SUFFIX_ASSUMPTIONS);

			Files.copy(assumptionsFile.toPath(), new File(queryDir,
					DirectSatEncodingUsingElkCsvQuery.FILE_NAME
							+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_ASSUMPTIONS)
									.toPath(),
					StandardCopyOption.REPLACE_EXISTING);

			// Write *.q file

			questionReader = new BufferedReader(new FileReader(questionFile));
			String line = questionReader.readLine();
			final int q = -Integer.parseInt(line.split(" +", 0)[0]);

			qWriter = new PrintWriter(new File(queryDir,
					DirectSatEncodingUsingElkCsvQuery.FILE_NAME
							+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_Q));
			qWriter.println(q);

			// Write *.ppp.g.u file

			pppguWriter = new PrintWriter(new File(queryDir,
					DirectSatEncodingUsingElkCsvQuery.FILE_NAME
							+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_PPP_G_U));

			assumptionsReader = new BufferedReader(
					new FileReader(assumptionsFile));
			line = assumptionsReader.readLine();
			for (final String a : line.split(" +", 0)) {
				final int assumption = Integer.parseInt(a);
				if (assumption > 0) {
					pppguWriter.println(assumption);
				}
			}

			// Write *.h file

			int maxAtom = 0;
			int clauseCount = 0;

			cnfReader = new BufferedReader(new FileReader(cnfFile));
			String clause;
			while ((clause = cnfReader.readLine()) != null) {
				boolean hasLiterals = false;
				for (final String l : clause.split(" +", 0)) {
					final int literal = Integer.parseInt(l);
					hasLiterals = true;
					final int atom = Math.abs(literal);
					if (atom > maxAtom) {
						maxAtom = atom;
					}
				}
				if (hasLiterals) {
					clauseCount++;
				}
			}

			hWriter = new PrintWriter(new File(queryDir,
					DirectSatEncodingUsingElkCsvQuery.FILE_NAME
							+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_H));
			hWriter.println(String.format("p cnf %d %d", maxAtom, clauseCount));

		} finally {
			Utils.closeQuietly(queryReader);
			Utils.closeQuietly(questionReader);
			Utils.closeQuietly(assumptionsReader);
			Utils.closeQuietly(cnfReader);
			Utils.closeQuietly(qWriter);
			Utils.closeQuietly(pppguWriter);
			Utils.closeQuietly(hWriter);
		}

	}

	private static File getFirstFileWithSuffix(final File dir,
			final String suffix) throws FileNotFoundException {
		final File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File directory, final String name) {
				return name.endsWith(suffix);
			}
		});
		if (files.length < 1) {
			throw new FileNotFoundException(
					"No *" + suffix + " file in " + dir);
		}
		return files[0];
	}

}
