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

import org.satpinpointing.experiments.ExperimentException;
import org.proofs.ProofProvider;
import org.proofs.SatProofProvider;
import org.liveontologies.puli.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class CollectStatisticsUsingDirectSat extends
		StatisticsCollector<CollectStatisticsUsingDirectSat.Options, Integer, Inference<Integer>, Integer> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(CollectStatisticsUsingDirectSat.class);

	public static final String INPUT_DIR_OPT = "input";

	public static class Options extends StatisticsCollector.Options {
		@Arg(dest = INPUT_DIR_OPT)
		public File inputDir;
	}

	@Override
	protected Options newOptions() {
		return new Options();
	}

	@Override
	protected void addArguments(final ArgumentParser parser) {
		parser.addArgument(INPUT_DIR_OPT)
				.type(Arguments.fileType().verifyExists().verifyIsDirectory())
				.help("directory with the input");
	}

	@Override
	protected ProofProvider<String, Integer, Inference<Integer>, Integer> init(
			final Options options) throws ExperimentException {
		LOGGER_.info("inputDir: {}", options.inputDir);
		return new SatProofProvider(options.inputDir);
	}

	public static void main(final String[] args) {
		new CollectStatisticsUsingDirectSat().collectStatistics(args);
	}

}
