package com.github.joergschwabe.experiments;

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

import org.liveontologies.puli.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.joergschwabe.proofs.ProofProvider;
import com.github.joergschwabe.proofs.SatProofProvider;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public abstract class SatJustificationExperiment<O extends SatJustificationExperiment.Options>
		extends
		BaseJustificationExperiment<O, Integer, Inference<Integer>, Integer> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(SatJustificationExperiment.class);

	public static final String INPUT_DIR_OPT = "input";

	public static class Options extends BaseJustificationExperiment.Options {
		@Arg(dest = INPUT_DIR_OPT)
		public File inputDir;
	}

	private File inputDir_;

	@Override
	protected void addArguments(final ArgumentParser parser) {
		parser.addArgument(INPUT_DIR_OPT)
				.type(Arguments.fileType().verifyExists().verifyIsDirectory())
				.help("directory with the input");
	}

	@Override
	protected void init(final O options) throws ExperimentException {
		LOGGER_.info("inputDir: {}", options.inputDir);
		this.inputDir_ = options.inputDir;
	}

	@Override
	protected ProofProvider<String, Integer, Inference<Integer>, Integer> newProofProvider()
			throws ExperimentException {
		return new SatProofProvider(inputDir_);
	}

}
