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

import org.liveontologies.puli.pinpointing.InterruptMonitor;

/**
 * An justification enumeration experiment used by the experiment runners.
 * Implementing classes must have public constructor with no parameters. The
 * methods are called in the following order:
 * <ul>
 * <li>{@link #init(String[])} is called once after instantiation or
 * {@link #dispose()} and before any other method.
 * <li>{@link #before(String)} is called just before each call of
 * {@link #run(InterruptMonitor)} that should perform one experiment on the
 * query passed as a parameter to {@link #before(String)} called just before.
 * <li>{@link #run(InterruptMonitor)} performs experiments on the query passed
 * as a parameter to {@link #before(String)} called just before.
 * <li>{@link #after()} is called just after each call of
 * {@link #run(InterruptMonitor)}.
 * <li>{@link #dispose()} is called once after any other method, except
 * {@link #init(String[])} which may be called after {@link #dispose()}.
 * </ul>
 * More details are in the documentation of the methods.
 * <p>
 * {@link #run(InterruptMonitor)} may be called on a different thread than the
 * other methods, but it will not be called concurrently with other methods, so
 * implementing classes do not need to ensure thread safety.
 * 
 * @author Peter Skocovsky
 */
public interface JustificationExperiment {

	/**
	 * Called once after instantiation or {@link #dispose()} and before any
	 * other method.
	 * 
	 * @param args
	 *            The parameters of the experiment.
	 * @throws ExperimentException
	 */
	void init(String[] args) throws ExperimentException;

	/**
	 * Called just before each call of {@link #run(InterruptMonitor)} that
	 * should perform experiments on the supplied {@code query}.
	 * 
	 * @param query
	 *            The query for the following experiment.
	 * @throws ExperimentException
	 */
	void before(String query) throws ExperimentException;

	/**
	 * Performs one experiment on the query passed as a parameter to
	 * {@link #before(String)} called just before. Whenever a new result is
	 * found, the registered {@link Listener}s should be notified.
	 * <p>
	 * This method may be called on a different thread than the other methods,
	 * but it will not be called concurrently with other methods, so
	 * implementing classes do not need to ensure thread safety.
	 * 
	 * @param monitor
	 *            The experiment should be terminated when
	 *            {@link InterruptMonitor#isInterrupted()
	 *            monitor.isInterrupted()} returns {@code true}.
	 * @throws ExperimentException
	 */
	void run(InterruptMonitor monitor) throws ExperimentException;

	/**
	 * Called just after each call of {@link #run(InterruptMonitor)}.
	 * 
	 * @throws ExperimentException
	 */
	void after() throws ExperimentException;

	/**
	 * Called once after any other method, except {@link #init(String[])} which
	 * may be called after {@link #dispose()}.
	 */
	void dispose();

	void addJustificationListener(Listener listener);

	void removeJustificationListener(Listener listener);

	public static interface Listener {
		/**
		 * Called when a new result is found during a run of experiment.
		 */
		void newJustification();
	}

}
