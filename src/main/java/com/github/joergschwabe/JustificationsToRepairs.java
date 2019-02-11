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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

/**
 * Obtains all minimal hitting sets of a collection of sets. This is done using
 * {@link Utils#merge(Set, java.util.Collection)}. The input collection are
 * files in input directory that are OWL ontologies. The output collection will
 * be written into files in output directory (each file is one minimal hitting
 * set). Call {@link #main(String[])} with argument "-h" to see usage.
 * 
 * @author Peter Skocovsky
 */
public class JustificationsToRepairs {

	public static final String INPUT_OPT = "inputdir";
	public static final String OUTPUT_OPT = "outputdir";

	public static class Options {
		@Arg(dest = INPUT_OPT)
		public File inputDir;
		@Arg(dest = OUTPUT_OPT)
		public File outputDir;
	}

	@SuppressWarnings("deprecation")
	public static void main(final String[] args)
			throws OWLOntologyCreationException, IOException,
			OWLOntologyStorageException {

		final ArgumentParser parser = ArgumentParsers
				.newArgumentParser(
						JustificationsToRepairs.class.getSimpleName())
				.description(
						"Obtains all minimal hitting sets of a collection of sets.");
		parser.addArgument(INPUT_OPT)
				.type(Arguments.fileType().verifyExists().verifyCanRead()
						.verifyIsDirectory())
				.help("input directory. Each file must be an OWL ontology. Its axioms are one of the input sets.");
		parser.addArgument(OUTPUT_OPT).type(File.class).help(
				"output directory. Each minimal hitting set will be written into one file as an OWL ontology.");

		final Options opt = new Options();
		try {
			parser.parseArgs(args, opt);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		}
		// else

		Utils.cleanDir(opt.outputDir);

		final OWLOntologyManager manager = OWLManager
				.createOWLOntologyManager();

		// Load justifications.
		final Set<Set<? extends OWLAxiom>> justs = new HashSet<>();
		final File[] justFiles = opt.inputDir.listFiles();
		if (justFiles == null) {
			throw new RuntimeException("Cannot list files in " + opt.inputDir);
		}
		String name = "DeFaUlT";
		for (final File justFile : justFiles) {
			final OWLOntology justOnto = manager
					.loadOntologyFromOntologyDocument(justFile);
			justs.add(justOnto.getLogicalAxioms());

			final String fileName = justFile.getName();
			final int dotIndex = fileName.indexOf('.');
			name = dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
		}

		// Make product and minimize.
		Set<Set<OWLAxiom>> repairs = new HashSet<>();
		repairs.add(Collections.<OWLAxiom> emptySet());
		for (final Set<? extends OWLAxiom> just : justs) {
			// Join with repairs so far.
			final Set<Set<OWLAxiom>> newRepairs = new HashSet<>();
			for (final Set<OWLAxiom> repair : repairs) {
				for (final OWLAxiom axiom : just) {
					final Set<OWLAxiom> newRepair = new HashSet<>();
					newRepair.addAll(repair);
					newRepair.add(axiom);
					// Minimize.
					Utils.merge(newRepair, newRepairs);
				}
			}
			repairs = newRepairs;
		}

		// Save the repairs.
		final int maxIndex = repairs.size() <= 1 ? repairs.size()
				: repairs.size() - 1;
		int index = 0;
		for (final Set<OWLAxiom> repair : repairs) {
			final OWLOntology repairOnt = manager.createOntology(repair);
			final File repairFile = new File(opt.outputDir,
					String.format(
							"%s.%0" + Utils.digitCount(maxIndex) + "d.repair",
							name, index));
			OutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream(repairFile);
				manager.saveOntology(repairOnt,
						new FunctionalSyntaxDocumentFormat(), outputStream);
			} finally {
				if (outputStream != null) {
					outputStream.close();
				}
			}
			index++;
		}

	}

}
