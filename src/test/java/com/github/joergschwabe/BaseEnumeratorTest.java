/*-
 * #%L
 * Proof Utility Library
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Live Ontologies Project
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
package com.github.joergschwabe;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetCollector;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.MinimalSubsetsFromProofs;

@RunWith(Parameterized.class)
public abstract class BaseEnumeratorTest<C, I extends Inference<? extends C>, A> {

	@Parameter(0)
	public String name;

	@Parameter(1)
	public EnumeratorTestInput<C, I, A> input;

	@Parameter(2)
	public MinimalSubsetsFromProofs.Factory<C, I, A> factory;

	@Test
	public void testRepairs() {

		final MinimalSubsetEnumerator.Factory<C, A> computation = factory
				.create(input.getProof(), input.getJustifier(),
						InterruptMonitor.DUMMY);

		final Set<Set<? extends A>> actualResult = new HashSet<Set<? extends A>>();
		computation.newEnumerator(input.getQuery())
				.enumerate(new MinimalSubsetCollector<A>(actualResult));

		Assert.assertEquals(input.getExpectedResult(), actualResult);
	}

	public static Iterable<Object[]> getParameters(
			final List<MinimalSubsetsFromProofs.Factory<?, ?, ?>> factories,
			final String testInputSubpkg) throws Exception {

		final List<EnumeratorTestInput<?, ?, ?>> inputs = getEnumeratorTestInputs(
				testInputSubpkg);

		final List<Object[]> parameters = new ArrayList<Object[]>();
		for (final MinimalSubsetsFromProofs.Factory<?, ?, ?> factory : factories) {
			for (final EnumeratorTestInput<?, ?, ?> input : inputs) {
				final String name = input.getClass().getSimpleName() + ", "
						+ factory.getClass().getName();
				parameters.add(new Object[] { name, input, factory });
			}
		}

		return parameters;
	}

	public static final String CLASS_FILE_EXT = ".class";

	public static List<EnumeratorTestInput<?, ?, ?>> getEnumeratorTestInputs(
			final String testInputSubpkg)
			throws URISyntaxException, ClassNotFoundException,
			InstantiationException, IllegalAccessException {

		final List<EnumeratorTestInput<?, ?, ?>> inputs = new ArrayList<EnumeratorTestInput<?, ?, ?>>();

		final String pkgName = BaseEnumeratorTest.class.getPackage().getName();
		final String inputsLocation = pkgName.replace('.', '/') + "/"
				+ testInputSubpkg.replace('.', '/');
		final URI inputsUri = BaseEnumeratorTest.class.getClassLoader()
				.getResource(inputsLocation).toURI();
		final File inputsDir = new File(inputsUri);
		String[] fileNames = inputsDir.list(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(CLASS_FILE_EXT);
			}
		});
		if (fileNames == null) {
			throw new RuntimeException("Cannot find test files");
		}
		for (final String filename : fileNames) {
			final String inputClassName = pkgName + "." + testInputSubpkg + "."
					+ filename.substring(0,
							filename.length() - CLASS_FILE_EXT.length());
			final Class<?> inputClass = Class.forName(inputClassName);
			if (!EnumeratorTestInput.class.isAssignableFrom(inputClass)) {
				throw new IllegalArgumentException(inputClass
						+ " is not a subclass of " + EnumeratorTestInput.class);
			}
			// else
			inputs.add((EnumeratorTestInput<?, ?, ?>) inputClass.newInstance());
		}

		return inputs;
	}

}
