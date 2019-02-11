package com.github.joergschwabe.proofs.browser;

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

import javax.swing.tree.TreePath;

public interface TreeNodeLabelProvider {

	/**
	 * Returns label for the tree node <code>obj</code>.
	 * 
	 * @param obj
	 *            The tree node whose label should be returned.
	 * @param path
	 *            The path to the node if available, <code>null</code> if not.
	 * @return label for the tree node <code>obj</code>.
	 */
	String getLabel(Object obj, TreePath path);

}
