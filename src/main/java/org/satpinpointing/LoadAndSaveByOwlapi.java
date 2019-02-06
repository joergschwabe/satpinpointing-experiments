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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

/**
 * Loads an ontology with OWL API and saves it in functional syntax. This may
 * change the ontology, because OWL API removes duplicated axioms and duplicates
 * in some expressions (line conjunctions). Call {@link #main(String[])} with
 * argument "-h" to see usage.
 * 
 * @author Peter Skocovsky
 */
public class LoadAndSaveByOwlapi {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(LoadAndSaveByOwlapi.class);

	public static final String INPUT_OPT = "i";
	public static final String OUTPUT_OPT = "o";

	public static class Options {
		@Arg(dest = INPUT_OPT)
		public File inputFile;
		@Arg(dest = OUTPUT_OPT)
		public File outputFile;
	}

	public static void main(final String[] args) {

		final ArgumentParser parser = ArgumentParsers
				.newArgumentParser(LoadAndSaveByOwlapi.class.getSimpleName())
				.description(
						"Loads an ontology with OWL API and saves it in functional syntax.");
		parser.addArgument("-" + INPUT_OPT)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("input file (default: stdin)");
		parser.addArgument("-" + OUTPUT_OPT).type(File.class)
				.help("output file (default: stdout)");

		final OWLOntologyManager manager = OWLManager
				.createOWLOntologyManager();

		InputStream input = null;
		OutputStream output = null;

		try {

			final Options opt = new Options();
			parser.parseArgs(args, opt);

			if (opt.inputFile == null) {
				input = System.in;
			} else {
				input = new FileInputStream(opt.inputFile);
			}
			if (opt.outputFile == null) {
				output = System.out;
			} else {
				if (opt.outputFile.exists()) {
					Utils.recursiveDelete(opt.outputFile);
				}
				output = new FileOutputStream(opt.outputFile);
			}

			final OWLOntology ont = manager
					.loadOntologyFromOntologyDocument(input);

			manager.saveOntology(ont, new FunctionalSyntaxDocumentFormat(),
					output);

		} catch (final OWLOntologyCreationException e) {
			LOGGER_.error("Could not load the ontology!", e);
			System.exit(2);
		} catch (final FileNotFoundException e) {
			LOGGER_.error("File not found!", e);
			System.exit(2);
		} catch (final OWLOntologyStorageException e) {
			LOGGER_.error("Could not save the ontology!", e);
			System.exit(2);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		} finally {
			if (input != null && input != System.in) {
				Utils.closeQuietly(input);
			}
			if (output != null && output != System.out) {
				Utils.closeQuietly(output);
			}
		}

	}

}
