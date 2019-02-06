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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects records and prints then into the provided {@link PrintWriter} when
 * flushed.
 * <p>
 * A record is a set of name-value pairs. Calling {@link #newRecord()} starts
 * entering a new record and finishes entering previous record if there was one.
 * The new record can be populated using the returned {@link RecordBuilder}.
 * When any method of a {@link RecordBuilder} is called after a new one is
 * obtained with another call to {@link #newRecord()}, behavior is undefined.
 * <p>
 * Calling {@link #flush()} writes the entered records that were not printed yet
 * into the {@link PrintWriter} provided to the constructor. The records are
 * written in the CSV format (delimiter is "," and quote character is "\""). The
 * first line are the names of the values in the records, so if a new name is
 * introduces after the first call to {@link #flush()}, behavior is undefined.
 * Also the record that is just being entered is written, so if its values are
 * entered in a different order as for the previous records, behavior is
 * undefined.
 * 
 * @author Peter Skocovsky
 */
public class Recorder {

	public static interface RecordBuilder {
		Object put(final String name, final Object value);
	}

	private final RecordBuilder recordBuilder_ = new RecordBuilder() {

		@Override
		public Object put(final String name, final Object value) {

			LOGGER_.info("{}: {}", name, value);

			names_.add(name);
			return currentRecord_.put(name, value);
		}

	};

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(Recorder.class);

	private final PrintWriter output_;

	private final Set<String> names_ = new LinkedHashSet<>();
	private final List<List<Object>> records_ = new ArrayList<>();

	private final Map<String, Object> currentRecord_ = new HashMap<>();

	private int recordIndex_ = 0;
	private int valueIndex_ = 0;

	public Recorder(final PrintWriter output) {
		this.output_ = output;
	}

	public RecordBuilder newRecord() {
		if (currentRecord_.isEmpty()) {
			return recordBuilder_;
		}
		// else
		final List<Object> record = new ArrayList<>(currentRecord_.size());
		for (final String name : names_) {
			final Object value = currentRecord_.get(name);
			record.add(value);
		}
		records_.add(record);
		currentRecord_.clear();
		return recordBuilder_;
	}

	public void flush() {
		if (output_ == null) {
			return;
		}
		// else

		if (recordIndex_ == 0 && valueIndex_ == 0) {
			// Write header
			final Iterator<String> iter = names_.iterator();
			if (iter.hasNext()) {
				output_.print(iter.next());
				while (iter.hasNext()) {
					output_.print(",");
					output_.print(iter.next());
				}
			}
			output_.println();
		}

		for (; recordIndex_ < records_.size(); recordIndex_++) {
			final List<Object> record = records_.get(recordIndex_);
			for (; valueIndex_ < record.size(); valueIndex_++) {
				if (valueIndex_ != 0) {
					output_.print(",");
				}
				output_.print(valueToString(record.get(valueIndex_)));
			}
			valueIndex_ = 0;
			output_.println();
		}

		final List<Object> record = new ArrayList<>(currentRecord_.size());
		for (final String name : names_) {
			final Object value = currentRecord_.get(name);
			record.add(value);
		}
		final ListIterator<Object> iter = record.listIterator(record.size());
		while (iter.hasPrevious()) {
			final Object value = iter.previous();
			if (value == null) {
				iter.remove();
			} else {
				break;
			}
		}
		for (; valueIndex_ < record.size(); valueIndex_++) {
			if (valueIndex_ != 0) {
				output_.print(",");
			}
			output_.print(valueToString(record.get(valueIndex_)));
		}

		output_.flush();
	}

	private String valueToString(final Object value) {
		if (value == null) {
			return "" + value;
		}
		// else
		if (value instanceof String) {
			final String string = (String) value;
			return "\"" + string.replace("\"", "") + "\"";
		}
		// else
		if (value instanceof Boolean) {
			return (Boolean) value ? "TRUE" : "FALSE";
		}
		// else
		return "" + value;
	}

}
