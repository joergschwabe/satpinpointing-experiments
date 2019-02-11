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

import java.util.Set;

/**
 * The set interface enhanced with methods for handling justifications
 * 
 * @author Yevgeny Kazakov
 *
 * @param <C>
 *            the type of the conclusion for which the justification is computed
 * @param <A>
 *            the type of axioms in the justification
 */
public interface Justification<C, A> extends Set<A> {

	/**
	 * @return the conclusion for which this justification is computed
	 */
	C getConclusion();

	/**
	 * Copies the justification to another conclusion
	 * 
	 * @param conclusion
	 * @return the justification containing the same axioms as this
	 *         justification but for the given conclusion
	 */
	public Justification<C, A> copyTo(C conclusion);

	/**
	 * @param added
	 * @return a justification obtained from this one by adding all elements in
	 *         the given set; this justification is not modified
	 */
	public Justification<C, A> addElements(Set<? extends A> added);

	/**
	 * @param removed
	 * @return a justification obtained from this one by removing all elements
	 *         in the given set; this justification is not modified
	 */
	public Justification<C, A> removeElements(Set<? extends A> removed);

	/**
	 * The visitor pattern for instances
	 * 
	 * @author Yevgeny Kazakov
	 *
	 * @param <C>
	 *            the type of the conclusion for which the justification is
	 *            computed
	 * @param <A>
	 *            the type of axioms in the justification
	 *
	 * @param <O>
	 *            the type of the output
	 */
	public interface Visitor<C, A, O> {

		O visit(Justification<C, A> just);

	}

}
