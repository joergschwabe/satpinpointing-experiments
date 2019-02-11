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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectAxiomsFromImports {
	
	private static final Logger LOG = LoggerFactory.getLogger(
			CollectAxiomsFromImports.class);

	@SuppressWarnings("deprecation")
	public static void main(final String[] args) {
		
		if (args.length < 2) {
			LOG.error("Insufficient arguments!");
			System.exit(1);
		}

		final File inputFile = new File(args[0]);
		final File outputFile = new File(args[1]);
		if (outputFile.exists()) {
			Utils.recursiveDelete(outputFile);
		}
		
		final OWLOntologyManager manager =
				OWLManager.createOWLOntologyManager();
		
		try {
			
			LOG.info("Loading ontology ...");
			long start = System.currentTimeMillis();
			final OWLOntology ont =
					manager.loadOntologyFromOntologyDocument(inputFile);
			LOG.info("... took {}s",
					(System.currentTimeMillis() - start)/1000.0);
			LOG.info("Loaded ontology: {}", ont);
			
			final Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			for (final OWLAxiom axiom : ont.getAxioms(Imports.INCLUDED)) {
				axioms.add(axiom);
			}
			
			manager.removeOntology(ont);
			final OWLOntology outOnt =
					manager.createOntology(ont.getOntologyID());
			manager.addAxioms(outOnt, axioms);
			
			manager.saveOntology(outOnt,
					new FunctionalSyntaxDocumentFormat(),
					new FileOutputStream(outputFile));
			
			
			
		} catch (final OWLOntologyCreationException e) {
			LOG.error("Could not load the ontology!", e);
			System.exit(2);
		} catch (final FileNotFoundException e) {
			LOG.error("File not found!", e);
			System.exit(2);
		} catch (final OWLOntologyStorageException e) {
			LOG.error("Cannot save the output ontology!", e);
			System.exit(3);
		}
		
	}

}
