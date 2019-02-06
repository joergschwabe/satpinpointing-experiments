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
package org.satpinpointing;

import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Progress {

	public static final long DEFAULT_REPORT_INTERVAL_MILLIS = 1000l;

	private static final SimpleDateFormat READ_FORMAT_ = new SimpleDateFormat(
			"S");
	private static final SimpleDateFormat WRITE_FORMAT_ = new SimpleDateFormat(
			"HH:mm:ss");

	private final PrintStream output_;
	private final long reportIntervalMillis_;

	private int current_;
	private int total_;
	private long startTimeMillis_;
	private long nextReportAfterMillis_;

	public Progress(final PrintStream output, final long reportIntervalMillis,
			final int total) {
		this.output_ = output;
		this.reportIntervalMillis_ = reportIntervalMillis;
		restart(total);
	}

	public Progress(final PrintStream output, final int total) {
		this(output, DEFAULT_REPORT_INTERVAL_MILLIS, total);
	}

	public void restart(final int total) {
		if (total < 0) {
			throw new IllegalArgumentException(
					"Total must not be negative! total=" + total);
		}
		// else
		current_ = 0;
		total_ = total;
		startTimeMillis_ = System.currentTimeMillis();
		plainReport();
	}

	private void report() {
		output_.print("\r");
		plainReport();
	}

	private void plainReport() {
		// @formatter:off
		// current/total 100.99%  elapsed: 00:00:00  ETA: 00:00:00
		// @formatter:on
		final double percent = 100.0 * current_ / total_;

		final long currentTimeMillis = System.currentTimeMillis();
		final long runTimeMillis = currentTimeMillis - startTimeMillis_;
		final double speedMillis = current_ / (double) runTimeMillis;
		final long timeLeftMillis = (long) ((total_ - current_) / speedMillis);

		final String elapsed;
		final String eta;
		try {
			elapsed = WRITE_FORMAT_
					.format(READ_FORMAT_.parse("" + runTimeMillis));
			eta = WRITE_FORMAT_.format(READ_FORMAT_.parse("" + timeLeftMillis));
		} catch (final ParseException e) {
			throw new RuntimeException(e);
		}

		final String s = String.format(
				"%" + Utils.digitCount(total_)
						+ "d/%d %6.2f%%  elapsed: %s  ETA: %s",
				current_, total_, percent, elapsed, eta);
		output_.print(s);
		nextReportAfterMillis_ = currentTimeMillis + reportIntervalMillis_;
	}

	public void update(final int increment) {
		current_ += increment;
		if (System.currentTimeMillis() >= nextReportAfterMillis_) {
			report();
		}
	}

	public void update() {
		update(1);
	}

	public void stop() {
		report();
		output_.println();
	}

	public void finish() {
		current_ = total_;
		stop();
	}

}
