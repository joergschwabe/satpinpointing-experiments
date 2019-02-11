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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

/**
 * Collects all subsumptions between atomic concepts entailed by the provided
 * ontology and saves then into a file. The subsumptions may be restricted to
 * the direct ones, obvious tautologies or subsumptions stated in the ontology
 * may be filtered out. The order in which the subsumptions are collected may be
 * controlled, or they may be sorted. Call {@link #main(String[])} with argument
 * "-h" to see usage.
 * 
 * @author Peter Skocovsky
 */
public class ExtractSubsumptions {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ExtractSubsumptions.class);

	public static final String ONTOLOGY_OPT = "ontology";
	public static final String OUTPUT_OPT = "output";
	public static final String TRAVERSAL_OPT = "traversal";
	public static final String COLLECTION_OPT = "collection";
	public static final String SORT_OPT = "sort";
	public static final String DIRECT_OPT = "direct";
	public static final String UNTOLD_OPT = "untold";
	public static final String TAUTOLOGIES_OPT = "taut";
	public static final String NO_BOTTOM_OPT = "nobottom";

	public static class Options {
		@Arg(dest = ONTOLOGY_OPT)
		public File ontologyFile;
		@Arg(dest = OUTPUT_OPT)
		public File outputFile;
		@Arg(dest = TRAVERSAL_OPT)
		public TraversalDirection traversalDirection;
		@Arg(dest = COLLECTION_OPT)
		public CollectionDirection collectionDirection;
		@Arg(dest = SORT_OPT)
		public boolean doSort;
		@Arg(dest = DIRECT_OPT)
		public boolean onlyDirect;
		@Arg(dest = UNTOLD_OPT)
		public boolean onlyUntold;
		@Arg(dest = TAUTOLOGIES_OPT)
		public boolean includeTautologies;
		@Arg(dest = NO_BOTTOM_OPT)
		public boolean avoidBottomNode;
	}

	public static void main(final String[] args) {

		final ArgumentParser parser = ArgumentParsers
				.newArgumentParser(ExtractSubsumptions.class.getSimpleName())
				.description("Extract subsumptions.");
		parser.addArgument(ONTOLOGY_OPT)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("ontology file");
		parser.addArgument(OUTPUT_OPT).type(File.class).help("output file");
		parser.addArgument("--" + TRAVERSAL_OPT).type(TraversalDirection.class)
				.setDefault(TraversalDirection.TOP_DOWN)
				.help("in which direction should the taxonomy be traversed (default: "
						+ TraversalDirection.TOP_DOWN + ")");
		parser.addArgument("--" + COLLECTION_OPT)
				.type(CollectionDirection.class)
				.setDefault(CollectionDirection.SUPER_TO_SUB)
				.help("in which order should the subsumptions be collected (default: "
						+ CollectionDirection.SUPER_TO_SUB + ")");
		parser.addArgument("--" + SORT_OPT).action(Arguments.storeTrue())
				.help("sort the subsumptions");
		parser.addArgument("--" + DIRECT_OPT).action(Arguments.storeTrue())
				.help("collect only direct subsumptions");
		parser.addArgument("--" + UNTOLD_OPT).action(Arguments.storeTrue())
				.help("collect only subsumptions that are not asserted in the ontology");
		parser.addArgument("--" + TAUTOLOGIES_OPT).action(Arguments.storeTrue())
				.help("collect also obviously tautological subsumptions");
		parser.addArgument("--" + NO_BOTTOM_OPT).action(Arguments.storeTrue())
				.help("do not collect subsumptions involving inconsistent classes");

		final OWLOntologyManager manager = OWLManager
				.createOWLOntologyManager();

		PrintWriter output = null;

		try {

			final Options opt = new Options();
			parser.parseArgs(args, opt);

			LOGGER_.info("ontologyFile: {}", opt.ontologyFile);
			if (opt.outputFile.exists()) {
				Utils.recursiveDelete(opt.outputFile);
			}
			LOGGER_.info("outputFile: {}", opt.outputFile);
			LOGGER_.info("traversalDirection: {}", opt.traversalDirection);
			LOGGER_.info("collectionDirection: {}", opt.collectionDirection);
			LOGGER_.info("doSort: {}", opt.doSort);
			LOGGER_.info("onlyDirect: {}", opt.onlyDirect);
			LOGGER_.info("onlyUntold: {}", opt.onlyUntold);
			LOGGER_.info("includeTautologies: {}", opt.includeTautologies);
			LOGGER_.info("avoidBottomNode: {}", opt.avoidBottomNode);

			LOGGER_.info("Loading ontology ...");
			long start = System.currentTimeMillis();
			final OWLOntology ont = manager
					.loadOntologyFromOntologyDocument(opt.ontologyFile);
			LOGGER_.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);
			LOGGER_.info("Loaded ontology: {}", ont.getOntologyID());

			final OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
			final OWLReasoner reasoner = reasonerFactory.createReasoner(ont);

			LOGGER_.info("Classifying ...");
			start = System.currentTimeMillis();
			reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			LOGGER_.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);

			LOGGER_.info("Extracting subsumptions ...");
			start = System.currentTimeMillis();
			final List<OWLSubClassOfAxiom> subsumptions = extractSubsumptions(
					reasoner, opt.traversalDirection, opt.collectionDirection,
					opt.onlyDirect, opt.onlyUntold, opt.includeTautologies,
					opt.avoidBottomNode);
			LOGGER_.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);
			LOGGER_.info("Number of direct subsumptions: {}",
					subsumptions.size());

			if (opt.doSort) {
				LOGGER_.info("Sorting direct subsumptions ...");
				start = System.currentTimeMillis();
				Collections.sort(subsumptions);
				LOGGER_.info("... took {}s",
						(System.currentTimeMillis() - start) / 1000.0);
			}

			LOGGER_.info("Printing direct subsumptions ...");
			start = System.currentTimeMillis();
			output = new PrintWriter(opt.outputFile);
			for (final OWLSubClassOfAxiom subsumption : subsumptions) {
				output.print(subsumption.getSubClass().asOWLClass().getIRI());
				output.print(" ");
				output.print(subsumption.getSuperClass().asOWLClass().getIRI());
				output.println();
			}
			LOGGER_.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);

		} catch (final OWLOntologyCreationException e) {
			LOGGER_.error("Could not load the ontology!", e);
			System.exit(2);
		} catch (final FileNotFoundException e) {
			LOGGER_.error("File Not Found!", e);
			System.exit(2);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		} finally {
			if (output != null) {
				output.close();
			}
		}

	}

	private static List<OWLSubClassOfAxiom> extractSubsumptions(
			final OWLReasoner reasoner,
			final TraversalDirection traversalDirection,
			final CollectionDirection collectionDirection,
			final boolean onlyDirect, final boolean onlyUntold,
			final boolean includeTautologies, final boolean avoidBottomNode) {

		final Set<Node<OWLClass>> done = new HashSet<Node<OWLClass>>();
		final Queue<Node<OWLClass>> toDo = new LinkedList<Node<OWLClass>>();
		final List<OWLSubClassOfAxiom> result = new ArrayList<OWLSubClassOfAxiom>();

		final Node<OWLClass> first = traversalDirection.getFirst(reasoner);
		toDo.add(first);
		done.add(first);

		Node<OWLClass> node;
		while ((node = toDo.poll()) != null) {

			collectionDirection.collect(node, onlyDirect, onlyUntold,
					includeTautologies, avoidBottomNode, reasoner, result);

			// Queue up the next.
			for (final Node<OWLClass> subNode : traversalDirection
					.getNext(reasoner, node)) {
				if (done.add(subNode)) {
					toDo.add(subNode);
				}
			}

		}

		return result;
	}

	@SuppressWarnings("deprecation")
	private static boolean isAsserted(final OWLSubClassOfAxiom axiom,
			final OWLOntology ontology) {
		final Set<OWLSubClassOfAxiom> axioms = ontology
				.getSubClassAxiomsForSubClass(axiom.getSubClass().asOWLClass());
		if (axioms == null || axioms.isEmpty()) {
			return false;
		} else {
			return axioms.contains(axiom);
		}
	}

	public static enum TraversalDirection {

		TOP_DOWN {
			@Override
			public Node<OWLClass> getFirst(final OWLReasoner reasoner) {
				return reasoner.getTopClassNode();
			}

			@Override
			public NodeSet<OWLClass> getNext(final OWLReasoner reasoner,
					final Node<OWLClass> current) {
				return reasoner.getSubClasses(
						current.getRepresentativeElement(), true);
			}
		},

		BOTTOM_UP {
			@Override
			public Node<OWLClass> getFirst(final OWLReasoner reasoner) {
				return reasoner.getBottomClassNode();
			}

			@Override
			public NodeSet<OWLClass> getNext(final OWLReasoner reasoner,
					final Node<OWLClass> current) {
				return reasoner.getSuperClasses(
						current.getRepresentativeElement(), true);
			}
		};

		public abstract Node<OWLClass> getFirst(OWLReasoner reasoner);

		public abstract NodeSet<OWLClass> getNext(OWLReasoner reasoner,
				Node<OWLClass> current);

	}

	public static enum CollectionDirection {

		SUPER_TO_SUB {
			@Override
			public void collect(final Node<OWLClass> current,
					final boolean onlyDirect, final boolean onlyUntold,
					final boolean includeTautologies,
					final boolean avoidBottomNode, final OWLReasoner reasoner,
					final Collection<OWLSubClassOfAxiom> collection) {

				if (avoidBottomNode && current.isBottomNode()) {
					// Avoid bottom node
					return;
				}
				// else

				final OWLOntology ontology = reasoner.getRootOntology();
				final OWLDataFactory factory = ontology.getOWLOntologyManager()
						.getOWLDataFactory();

				// Classes in current are superclasses.
				for (final OWLClass sup : current) {

					if (!includeTautologies && sup.isTopEntity()) {
						// No tautologies.
						continue;
					}
					// else

					// Equivalent
					for (final OWLClass sub : current) {

						if (!includeTautologies
								&& (sub.isBottomEntity() || sup.equals(sub))) {
							// No tautologies.
							continue;
						}
						// else

						final OWLSubClassOfAxiom axiom = factory
								.getOWLSubClassOfAxiom(sub, sup);
						if (!onlyUntold || !isAsserted(axiom, ontology)) {
							collection.add(axiom);
						}

					}

					// Subclasses
					for (final Node<OWLClass> subNode : reasoner
							.getSubClasses(sup, onlyDirect)) {
						if (avoidBottomNode && subNode.isBottomNode()) {
							// Avoid bottom node
							continue;
						}
						// else
						for (final OWLClass sub : subNode) {

							if (!includeTautologies && sub.isBottomEntity()) {
								// No tautologies.
								continue;
							}
							// else

							final OWLSubClassOfAxiom axiom = factory
									.getOWLSubClassOfAxiom(sub, sup);
							if (!onlyUntold || !isAsserted(axiom, ontology)) {
								collection.add(axiom);
							}

						}
					}

				}

			}
		},

		SUB_TO_SUPER {
			@Override
			public void collect(final Node<OWLClass> current,
					final boolean onlyDirect, final boolean onlyUntold,
					final boolean includeTautologies,
					final boolean avoidBottomNode, final OWLReasoner reasoner,
					final Collection<OWLSubClassOfAxiom> collection) {

				if (avoidBottomNode && current.isBottomNode()) {
					// Avoid bottom node
					return;
				}
				// else

				final OWLOntology ontology = reasoner.getRootOntology();
				final OWLDataFactory factory = ontology.getOWLOntologyManager()
						.getOWLDataFactory();

				// Classes in current are subclasses.
				for (final OWLClass sub : current) {

					if (!includeTautologies && sub.isBottomEntity()) {
						// No tautologies.
						continue;
					}
					// else

					// Equivalent
					for (final OWLClass sup : current) {

						if (!includeTautologies
								&& (sup.isTopEntity() || sub.equals(sup))) {
							// No tautologies.
							continue;
						}
						// else

						final OWLSubClassOfAxiom axiom = factory
								.getOWLSubClassOfAxiom(sub, sup);
						if (!onlyUntold || !isAsserted(axiom, ontology)) {
							collection.add(axiom);
						}

					}

					// Superclasses
					for (final Node<OWLClass> superNode : reasoner
							.getSuperClasses(sub, onlyDirect)) {
						for (final OWLClass sup : superNode) {

							if (!includeTautologies && sup.isTopEntity()) {
								// No tautologies.
								continue;
							}
							// else

							final OWLSubClassOfAxiom axiom = factory
									.getOWLSubClassOfAxiom(sub, sup);
							if (!onlyUntold || !isAsserted(axiom, ontology)) {
								collection.add(axiom);
							}

						}
					}

				}

			}
		};

		public abstract void collect(Node<OWLClass> current, boolean onlyDirect,
				boolean onlyUntold, boolean includeTautologies,
				boolean avoidBottomNode, OWLReasoner reasoner,
				Collection<OWLSubClassOfAxiom> collection);

	}

}
