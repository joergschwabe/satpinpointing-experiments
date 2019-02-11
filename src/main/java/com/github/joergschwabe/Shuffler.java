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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Shuffler {

	public static void main(final String[] args) {
		
		if (args.length < 1) {
			System.err.println("Insufficient arguments!");
			System.exit(1);
		}
		final Random random = new Random(Long.valueOf(args[0]));
		
		FileInputStream fin = null;
		
		try {
			final InputStream in;
			if (args.length > 1) {
				fin = new FileInputStream(args[1]);
				in = fin;
			} else {
				in = System.in;
			}
			
			final List<String> lines = new ArrayList<String>();
			
			final BufferedReader reader =
					new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			
			Collections.shuffle(lines, random);
			
			for (final String l : lines) {
				System.out.println(l);
			}
			
		} catch (final FileNotFoundException e) {
			System.err.println("The input file cannot be found!");
			e.printStackTrace();
			System.exit(2);
		} catch (final IOException e) {
			System.err.println("Problem during reading the imput file!");
			e.printStackTrace();
			System.exit(2);
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (final IOException e) {}
			}
		}
		
	}

}
