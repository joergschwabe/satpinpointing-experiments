package org.satpinpointing.experiments;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator.Factory;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class OwlFactoryJustificationExperiment extends
		OwlJustificationExperiment<OwlFactoryJustificationExperiment.Options> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(OwlFactoryJustificationExperiment.class);

	public static final String OPT_FACTORY_CLASS = "class";

	public static class Options
			extends OwlJustificationExperiment.Options {
		@Arg(dest = OPT_FACTORY_CLASS)
		public String computationFactoryClassName;
	}

	private MinimalSubsetsFromProofs.Factory<OWLAxiom, Inference<OWLAxiom>, OWLAxiom> factory_;

	@Override
	protected Options newOptions() {
		return new Options();
	}

	@Override
	protected void addArguments(final ArgumentParser parser) {
		super.addArguments(parser);
		parser.description(
				"Experiment using provided Justification Computation and OWL API proofs from ELK.");
		parser.addArgument(OPT_FACTORY_CLASS)
				.help("class of the computation factory");
	}

	@Override
	protected void init(final Options options) throws ExperimentException {
		super.init(options);
		LOGGER_.info("computationFactoryClassName: {}",
				options.computationFactoryClassName);
		try {
			final Class<?> computationClass = Class
					.forName(options.computationFactoryClassName);
			final Method getFactory = computationClass.getMethod("getFactory");
			@SuppressWarnings("unchecked")
			final MinimalSubsetsFromProofs.Factory<OWLAxiom, Inference<OWLAxiom>, OWLAxiom> factory = (MinimalSubsetsFromProofs.Factory<OWLAxiom, Inference<OWLAxiom>, OWLAxiom>) getFactory
					.invoke(null);
			factory_ = factory;
		} catch (final ClassNotFoundException e) {
			throw new ExperimentException(e);
		} catch (final NoSuchMethodException e) {
			throw new ExperimentException(e);
		} catch (final SecurityException e) {
			throw new ExperimentException(e);
		} catch (final IllegalAccessException e) {
			throw new ExperimentException(e);
		} catch (final IllegalArgumentException e) {
			throw new ExperimentException(e);
		} catch (final InvocationTargetException e) {
			throw new ExperimentException(e);
		}
	}

	@Override
	protected Factory<OWLAxiom, OWLAxiom> newComputation(
			final Proof<? extends Inference<OWLAxiom>> proof,
			final InferenceJustifier<? super Inference<OWLAxiom>, ? extends Set<? extends OWLAxiom>> justifier,
			final InterruptMonitor monitor) throws ExperimentException {
		return factory_.create(proof, justifier, monitor);
	}

}
