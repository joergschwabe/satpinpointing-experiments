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
package com.github.joergschwabe.input;

import com.github.joergschwabe.ProofAndJustifierBuilder;

public abstract class SeveralCycles
		extends BaseEnumeratorTestInput<String, Integer> {

	private static ProofAndJustifierBuilder<String, Integer> getBuilder() {

		final ProofAndJustifierBuilder<String, Integer> builder = new ProofAndJustifierBuilder<String, Integer>();

		builder.conclusion("A").premise("B").axiom(1).add();
		builder.conclusion("A").premise("C").axiom(1).add();
		builder.conclusion("B").premise("C").axiom(3).add();
		builder.conclusion("C").premise("D").axiom(4).add();
		builder.conclusion("D").premise("E").axiom(5).add();
		builder.conclusion("E").premise("F").axiom(6).add();
		builder.conclusion("F").premise("C").axiom(7).add();
		builder.conclusion("F").premise("K").premise("L").premise("G").axiom(8).add();
		builder.conclusion("G").premise("E").axiom(9).add();
		builder.conclusion("B").premise("I").axiom(12).add();
		builder.conclusion("I").premise("J").premise("K").axiom(13).add();
		builder.conclusion("J").premise("B").axiom(13).add();
		builder.conclusion("D").premise("C").axiom(8).axiom(9).add();
		builder.conclusion("G").axiom(10).axiom(11).add();
		builder.conclusion("E").axiom(1).axiom(9).add();
		builder.conclusion("C").axiom(8).axiom(9).add();
		builder.conclusion("K").add();
		builder.conclusion("L").add();

		return builder;
	}

	public SeveralCycles() {
		super(getBuilder());
	}

	@Override
	public String getQuery() {
		return "A";
	}

}
