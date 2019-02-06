package org.proofs.browser;

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
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.tree.TreePath;

import org.satpinpointing.BottomUpJustificationComputation;
import org.satpinpointing.MinimalSubsetCollector;
import org.satpinpointing.Utils;
import org.satpinpointing.experiments.ExperimentException;
import org.proofs.ElkProofProvider;
import org.proofs.JustificationCompleteProof;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.semanticweb.elk.owl.interfaces.ElkObject;
import org.semanticweb.elk.owl.interfaces.ElkSubClassOfAxiom;
import org.semanticweb.elk.owl.iris.ElkFullIri;
import org.semanticweb.elk.reasoner.tracing.Conclusion;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProofBrowser {

	private static final Logger LOG = LoggerFactory
			.getLogger(ProofBrowser.class);

	public static void main(final String[] args) {

		if (args.length < 3) {
			LOG.error("Insufficient arguments!");
			System.exit(1);
		}

		final int startingSizeLimit = Math.max(1, Integer.parseInt(args[0]));
		final String ontologyFileName = args[1];
		final String subFullIri = args[2];
		final String supFullIri = args[3];

		final OWLOntologyManager manager = OWLManager
				.createOWLOntologyManager();

		try {

			final ElkProofProvider elkProofProvider = new ElkProofProvider(
					new File(ontologyFileName), manager);
			final ElkObject.Factory factory = elkProofProvider.getReasoner()
					.getElkFactory();

			final ElkSubClassOfAxiom conclusion = factory.getSubClassOfAxiom(
					factory.getClass(new ElkFullIri(subFullIri)),
					factory.getClass(new ElkFullIri(supFullIri)));
			//
			final JustificationCompleteProof<Object, Inference<Object>, ElkAxiom> proof = elkProofProvider
					.getProof(conclusion);

			final MinimalSubsetCollector<Object, Inference<Object>, ElkAxiom> collector = new MinimalSubsetCollector<>(
					BottomUpJustificationComputation
							.<Object, Inference<Object>, ElkAxiom> getFactory(),
					proof.getProof(), proof.getJustifier());

			for (int size = startingSizeLimit; size <= Integer.MAX_VALUE; size++) {

				final int sizeLimit = size;

				final Collection<? extends Set<ElkAxiom>> justs = collector
						.collect(proof.getQuery(), sizeLimit);

				final TreeNodeLabelProvider decorator = new TreeNodeLabelProvider() {
					@Override
					public String getLabel(final Object obj,
							final TreePath path) {

						if (obj instanceof Conclusion) {
							final Conclusion c = (Conclusion) obj;
							final Collection<? extends Set<ElkAxiom>> js = collector
									.collect(c, sizeLimit);
							return "[" + js.size() + "] ";
						} else if (obj instanceof Inference) {
							final Inference<?> inf = (Inference<?>) obj;
							int product = 1;
							for (final Object premise : inf.getPremises()) {
								final Collection<? extends Set<ElkAxiom>> js = collector
										.collect((Conclusion) premise,
												sizeLimit);
								product *= js.size();
							}
							return "<" + product + "> ";
						}

						return "";
					}
				};

				final TreeNodeLabelProvider toolTipProvider = new TreeNodeLabelProvider() {
					@Override
					public String getLabel(final Object obj,
							final TreePath path) {

						if (path == null || path.getPathCount() < 2
								|| !(obj instanceof Conclusion)) {
							return null;
						}
						final Conclusion premise = (Conclusion) obj;
						final Object o = path
								.getPathComponent(path.getPathCount() - 2);
						if (!(o instanceof Inference)) {
							return null;
						}
						final Inference<?> inf = (Inference<?>) o;
						final Object c = inf.getConclusion();
						if (!(c instanceof Conclusion)) {
							return null;
						}
						final Conclusion concl = (Conclusion) c;

						final Collection<? extends Set<ElkAxiom>> premiseJs = collector
								.collect(premise, sizeLimit);
						final Collection<? extends Set<ElkAxiom>> conclJs = collector
								.collect(concl, sizeLimit);

						int countInf = 0;
						for (final Set<ElkAxiom> just : premiseJs) {
							if (Utils.isMinimal(just, conclJs)) {
								countInf++;
							}
						}

						int countGoal = 0;
						for (final Set<ElkAxiom> just : premiseJs) {
							if (Utils.isMinimal(just, justs)) {
								countGoal++;
							}
						}

						return "<html>minimal in inf conclusion: " + countInf
								+ "<br/>minimal in goal: " + countGoal
								+ "</html>";
					}
				};

				showProofBrowser(proof.getProof(), proof.getJustifier(),
						proof.getQuery(), "size " + sizeLimit, decorator,
						toolTipProvider);

				try {
					System.out.print("Press ENTER to continue: ");
					for (;;) {
						final int ch = System.in.read();
						if (ch == 10) {
							break;
						}
					}

				} catch (final IOException e) {
					LOG.error("Error during input!", e);
					break;
				}

			}

		} catch (final ExperimentException e) {
			LOG.error("Could not classify the ontology!", e);
			System.exit(2);
		}

	}

	public static <C, I extends Inference<? extends C>, A> void showProofBrowser(
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			final C conclusion) {
		showProofBrowser(proof, justifier, conclusion, null, null, null);
	}

	public static <C, I extends Inference<? extends C>, A> void showProofBrowser(
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			final C conclusion, final TreeNodeLabelProvider nodeDecorator,
			final TreeNodeLabelProvider toolTipProvider) {
		showProofBrowser(proof, justifier, conclusion, null, nodeDecorator,
				toolTipProvider);
	}

	public static <C, I extends Inference<? extends C>, A> void showProofBrowser(
			final Proof<? extends I> proof,
			final InferenceJustifier<? super I, ? extends Set<? extends A>> justifier,
			final C conclusion, final String title,
			final TreeNodeLabelProvider nodeDecorator,
			final TreeNodeLabelProvider toolTipProvider) {

		final StringBuilder message = new StringBuilder(
				"Change Look and Feel by adding one of the following properties:");
		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			message.append("\nswing.defaultlaf=").append(info.getClassName());
		}
		LOG.info(message.toString());

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final JFrame frame;
				if (title == null) {
					frame = new JFrame("Proof Browser - " + conclusion);
				} else {
					frame = new JFrame("Proof Browser - " + title);
				}

				final JScrollPane scrollPane = new JScrollPane(
						new ProofTreeComponent<C, I, A>(proof, justifier,
								conclusion, nodeDecorator, toolTipProvider));
				frame.getContentPane().add(scrollPane);

				frame.pack();
				// frame.setSize(500, 500);
				frame.setVisible(true);
			}
		});

	}

}
