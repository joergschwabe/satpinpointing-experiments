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

import java.util.List;
import java.util.Map;

/**
 * The result of {@link StronglyConnectedComponentsComputation}. The list of all
 * components can be obtained by {@link #getComponents()}. The index of the
 * component in the list containing a given element can be obtained by
 * {@link #getComponentId(Object)}
 * 
 * @author Yevgeny Kazakov
 *
 * @param <C>
 *            the type of elements in the components
 */
public class StronglyConnectedComponents<C> {

	private final List<List<C>> components_;

	private final Map<C, Integer> componentIds_;

	StronglyConnectedComponents(List<List<C>> components,
			Map<C, Integer> componentIds) {
		this.components_ = components;
		this.componentIds_ = componentIds;
	}

	/**
	 * @return the list of all components; every component is represented by
	 *         list of its elements; the elements in each list do not repeat, so
	 *         the number of components and the corresponding size of the
	 *         component can be obtained by calling {@link List#size()}. The
	 *         components are listed in the topological order: children come
	 *         first.
	 */
	public List<List<C>> getComponents() {
		return components_;
	}

	/**
	 * @param element
	 * @return the index of the component containing the given element or
	 *         {@code null} if there is no such a component
	 */
	public Integer getComponentId(Object element) {
		return componentIds_.get(element);
	}

}
