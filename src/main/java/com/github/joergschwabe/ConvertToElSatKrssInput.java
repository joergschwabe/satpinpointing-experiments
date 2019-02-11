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
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.elk.owl.interfaces.ElkObject;
import org.semanticweb.elk.owl.interfaces.ElkObjectPropertyDomainAxiom;
import org.semanticweb.elk.owl.printers.KrssSyntaxPrinterVisitor;
import org.semanticweb.elk.owlapi.wrapper.OwlConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertToElSatKrssInput {
	
	private static final Logger LOG = LoggerFactory.getLogger(
			ConvertToElSatKrssInput.class);

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
		
		PrintWriter output = null;
		
		try {
			
			LOG.info("Loading ontology ...");
			long start = System.currentTimeMillis();
			final OWLOntology ont =
					manager.loadOntologyFromOntologyDocument(inputFile);
			LOG.info("... took {}s",
					(System.currentTimeMillis() - start)/1000.0);
			LOG.info("Loaded ontology: {}", ont);
			
			final OwlConverter converter = OwlConverter.getInstance();
			
			output = new PrintWriter(outputFile);
			
			final ElSatPrinterVisitor printer = new ElSatPrinterVisitor(output);
			
			for (final OWLAxiom axiom
					: ont.getLogicalAxioms(Imports.INCLUDED)) {
				converter.convert(axiom).accept(printer);
			}
			
		} catch (final OWLOntologyCreationException e) {
			LOG.error("Could not load the ontology!", e);
			System.exit(2);
		} catch (final FileNotFoundException e) {
			LOG.error("File not found!", e);
			System.exit(2);
		} finally {
			if (output != null) {
				output.close();
			}
		}
		
	}
	
	static class ElSatPrinterVisitor extends KrssSyntaxPrinterVisitor {

		Set<Object> ignored;
		
		public ElSatPrinterVisitor(final Appendable writer) {
			super(writer);
			this.ignored = new HashSet<Object>();
		}
		
		@Override
		protected Void defaultVisit(final ElkObject elkObject) {
			if (ignored.add(elkObject.getClass())) {
				LOG.warn("Unsupported expression type: {}", elkObject.getClass());
			}
			return null;
		}

		@Override
		public Void visit(
				final ElkObjectPropertyDomainAxiom elkObjectPropertyDomainAxiom) {
			write("(implies (some ");
			write(elkObjectPropertyDomainAxiom.getProperty());
			write(" top) ");
			write(elkObjectPropertyDomainAxiom.getDomain());
			write(")\n");
			return null;
		}
		
	}

}
