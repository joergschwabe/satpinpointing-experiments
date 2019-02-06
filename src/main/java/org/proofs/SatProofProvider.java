package org.proofs;

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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.satpinpointing.DirectSatEncodingUsingElkCsvQuery;
import org.satpinpointing.Utils;
import org.satpinpointing.experiments.ExperimentException;
import org.proofs.adapters.DirectSatEncodingProofAdapter;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SatProofProvider
		implements ProofProvider<String, Integer, Inference<Integer>, Integer> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(SatProofProvider.class);

	private final File inputDir_;

	public SatProofProvider(final File inputDir) {
		this.inputDir_ = inputDir;
	}

	@Override
	public JustificationCompleteProof<Integer, Inference<Integer>, Integer> getProof(
			final String query) throws ExperimentException {

		LOGGER_.info("Decoding query {} ...", query);
		long start = System.currentTimeMillis();

		final File queryDir = new File(inputDir_, Utils.sha1hex(query));

		final File qFile = new File(queryDir,
				DirectSatEncodingUsingElkCsvQuery.FILE_NAME
						+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_Q);

		final String decoded;
		BufferedReader qReader = null;
		try {
			qReader = new BufferedReader(new FileReader(qFile));
			final String line = qReader.readLine();
			if (line == null) {
				throw new ExperimentException(
						"Could not read question file in: " + queryDir);
			}
			decoded = line.split("\\s+")[0];
		} catch (final IOException e) {
			throw new ExperimentException(e);
		} finally {
			Utils.closeQuietly(qReader);
		}
		final Integer goal = Integer.valueOf(decoded);

		final File cnfFile = new File(queryDir,
				DirectSatEncodingUsingElkCsvQuery.FILE_NAME
						+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_CNF);

		final File assumptionsFile = new File(queryDir,
				DirectSatEncodingUsingElkCsvQuery.FILE_NAME
						+ DirectSatEncodingUsingElkCsvQuery.SUFFIX_ASSUMPTIONS);

		LOGGER_.info("... took {}s",
				(System.currentTimeMillis() - start) / 1000.0);

		InputStream cnf = null;
		InputStream assumptions = null;
		try {

			cnf = new FileInputStream(cnfFile);
			assumptions = new FileInputStream(assumptionsFile);

			LOGGER_.info("Loading proof ...");
			start = System.currentTimeMillis();
			final Proof<Inference<Integer>> proof = DirectSatEncodingProofAdapter
					.load(assumptions, cnf);
			LOGGER_.info("... took {}s",
					(System.currentTimeMillis() - start) / 1000.0);

			return new BaseJustificationCompleteProof<>(goal, proof,
					DirectSatEncodingProofAdapter.JUSTIFIER);

		} catch (final IOException e) {
			throw new ExperimentException(e);
		} finally {
			Utils.closeQuietly(cnf);
			Utils.closeQuietly(assumptions);
		}

	}

	@Override
	public void dispose() {
		// Empty.
	}

}
