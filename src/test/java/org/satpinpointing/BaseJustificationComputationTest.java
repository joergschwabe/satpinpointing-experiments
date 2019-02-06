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
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.proofs.JustificationCompleteProof;
import org.proofs.ProofProvider;
import org.satpinpointing.BinarizedJustificationComputation;
import org.satpinpointing.BottomUpJustificationComputation;
import org.satpinpointing.MinPremisesBottomUp;
import org.satpinpointing.MinimalSubsetCollector;
import org.satpinpointing.TopDownJustificationComputation;
import org.satpinpointing.Utils;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.liveontologies.puli.pinpointing.ResolutionJustificationComputation;
import org.liveontologies.puli.pinpointing.TopDownRepairComputation;

@RunWith(Parameterized.class)
public abstract class BaseJustificationComputationTest<Q, C, I extends Inference<? extends C>, A> {

	public static List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> getJustificationComputationFactories() {
		final List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> computations = new ArrayList<MinimalSubsetsFromProofs.Factory<?, ?, ?>>();
		computations.add(BottomUpJustificationComputation.getFactory());
		computations.add(BinarizedJustificationComputation
				.getFactory(BottomUpJustificationComputation
						.<List<Object>, Inference<List<Object>>, Object> getFactory()));
		computations.add(MinPremisesBottomUp.getFactory());
		computations.add(TopDownJustificationComputation.getFactory());
		computations.add(ResolutionJustificationComputation.getFactory());
		return computations;
	}

	public static List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> getRepairComputationFactories() {
		final List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> computations = new ArrayList<MinimalSubsetsFromProofs.Factory<?, ?, ?>>();
		computations.add(TopDownRepairComputation.getFactory());
		return computations;
	}

	public static Collection<Object[]> getParameters(
			final List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> computationFactories,
			final String testInputDir,
			final String expectedOutputForEntailmentDirName)
			throws URISyntaxException {

		final Collection<Object[]> inputFiles = collectJustificationTestInputFiles(
				testInputDir, BaseJustificationComputationTest.class,
				expectedOutputForEntailmentDirName);

		final List<Object[]> result = new ArrayList<Object[]>();
		for (final MinimalSubsetsFromProofs.Factory<?, ?, ?> c : computationFactories) {
			for (final Object[] files : inputFiles) {
				final Object[] r = new Object[files.length + 1];
				r[0] = c;
				System.arraycopy(files, 0, r, 1, files.length);
				result.add(r);
			}
		}

		return result;
	}

	private final ProofProvider<Q, C, I, A> proofProvider_;
	private final MinimalSubsetsFromProofs.Factory<C, I, A> factory_;
	private final File ontoFile_;
	private final Map<File, File[]> entailFilesPerJustFile_;

	public BaseJustificationComputationTest(
			final ProofProvider<Q, C, I, A> proofProvider,
			final MinimalSubsetsFromProofs.Factory<C, I, A> factory,
			final File ontoFile,
			final Map<File, File[]> entailFilesPerJustFile) {
		this.proofProvider_ = proofProvider;
		this.factory_ = factory;
		this.ontoFile_ = ontoFile;
		this.entailFilesPerJustFile_ = entailFilesPerJustFile;
	}

	public MinimalSubsetsFromProofs.Factory<C, I, A> getFactory() {
		return factory_;
	}

	public File getOntoFile() {
		return ontoFile_;
	}

	public Map<File, File[]> getJustFilePerEntailFiles() {
		return entailFilesPerJustFile_;
	}

	protected void setUp() {
		// Empty default.
	}

	protected abstract Q getQuery(final File entailFile) throws Exception;

	private Set<? extends Set<? extends A>> getActualJustifications(
			final File entailFile) throws Exception {

		final JustificationCompleteProof<C, I, A> proof = proofProvider_
				.getProof(getQuery(entailFile));

		final MinimalSubsetCollector<C, I, A> collector = new MinimalSubsetCollector<>(
				getFactory(), proof.getProof(), proof.getJustifier());

		return new HashSet<>(collector.collect(proof.getQuery()));
	}

	protected abstract Set<? extends Set<? extends A>> getExpectedJustifications(
			final File[] justFiles) throws Exception;

	protected void dispose() {
		// Empty default.
	}

	@Before
	public void before() {
		setUp();
	}

	@Test
	public void test() throws Exception {

		// @formatter:off
		Assume.assumeFalse(
				"No expected output.\n" + "computation: " + factory_.getClass()
						+ "\n" + "ontology: " + ontoFile_,
				entailFilesPerJustFile_.isEmpty());
		// @formatter:on

		for (final Map.Entry<File, File[]> entry : entailFilesPerJustFile_
				.entrySet()) {

			final Set<? extends Set<? extends A>> justifications = getActualJustifications(
					entry.getKey());

			final Set<? extends Set<? extends A>> expected = getExpectedJustifications(
					entry.getValue());

			if (!expected.equals(justifications)) {

				final HashSet<Set<? extends A>> expectedMinusActual = new HashSet<>(
						expected);
				expectedMinusActual.removeAll(justifications);

				final HashSet<Set<? extends A>> actualMinusExpected = new HashSet<>(
						justifications);
				actualMinusExpected.removeAll(expected);

				// @formatter:off
				final String inputsMessage = "computation: "
						+ factory_.getClass() + "\n" + "ontology: " + ontoFile_
						+ "\n" + "entailment: " + entry.getKey() + "\n"
						+ "expected \\ actual: " + expectedMinusActual + "\n"
						+ "actual \\ expected: " + actualMinusExpected;
				// @formatter:on

				Assert.fail(inputsMessage);

			}

		}

	}

	@After
	public void after() {
		proofProvider_.dispose();
		dispose();
	}

	public static final String OWL_EXTENSION = ".owl";
	public static final String ENTAILMENT_EXTENSION = ".entailment";
	public static final String JUSTIFICATION_DIR_NAME = "justifications";
	public static final String REPAIRS_DIR_NAME = "repairs";

	public static Collection<Object[]> collectJustificationTestInputFiles(
			final String testInputDir, final Class<?> srcClass,
			final String expectedOutputForEntailmentDirName)
			throws URISyntaxException {

		final List<Object[]> result = new ArrayList<>();

		final URI inputDirURI = srcClass.getClassLoader()
				.getResource(testInputDir).toURI();

		// Assume it's not in JAR :-P
		final File rootDir = new File(inputDirURI);

		// For every ontology
		final File[] ontoFiles = rootDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(OWL_EXTENSION);
			}

		});
		if (ontoFiles == null) {
			throw new RuntimeException("Cannot list files in " + rootDir);
		}
		for (final File ontoFile : ontoFiles) {

			// For every entailment
			final String baseName = Utils.dropExtension(ontoFile.getName());
			final File[] entailFiles = rootDir.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(final File dir, final String name) {
					return name.startsWith(baseName)
							&& name.endsWith(ENTAILMENT_EXTENSION);
				}

			});
			if (entailFiles == null) {
				throw new RuntimeException(
						"Cannot list files in " + entailFiles);
			}
			final Map<File, File[]> entailFilesPerJustFile = new HashMap<>();
			for (final File entailDir : entailFiles) {

				if (!entailDir.isDirectory()) {
					throw new RuntimeException("Not a directory: " + entailDir);
				}

				final File entailFile = new File(entailDir,
						baseName + ENTAILMENT_EXTENSION);
				if (!entailFile.exists()) {
					throw new RuntimeException(
							"No entailment file: " + entailFile);
				}

				// Collect justification files
				final File justDir = new File(entailDir,
						expectedOutputForEntailmentDirName);
				if (!justDir.exists()) {
					// Ignore!
					continue;
				}
				if (!justDir.isDirectory()) {
					throw new RuntimeException("Not a directory: " + justDir);
				}
				final File[] justFiles = justDir.listFiles();
				if (justFiles == null) {
					throw new RuntimeException(
							"Cannot list files in " + justFiles);
				}

				entailFilesPerJustFile.put(entailFile, justFiles);
			}

			result.add(new Object[] { ontoFile, entailFilesPerJustFile });
		}

		return result;
	}

}
