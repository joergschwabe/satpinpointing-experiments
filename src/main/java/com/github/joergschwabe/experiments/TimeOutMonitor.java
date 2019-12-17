package com.github.joergschwabe.experiments;

import org.liveontologies.puli.pinpointing.InterruptMonitor;

/**
	 * Interrupts when the global or local timeout expires. The global timeout
	 * is counted from the passed global start time and the local from the
	 * creation of this object.
	 * 
	 * @author Peter Skocovsky
	 */
	public class TimeOutMonitor
			implements InterruptMonitor, JustificationExperiment.Listener {

		private final long stopTimeMillis_;
		private final boolean onlyOneJustification_;
		private long satSolverTime_;
		private long justCompTime_;
		private long cycleCompTime_;

		private int count_ = 0;

		private volatile boolean cancelled = false;
		private long startSatSolverTime;

		public TimeOutMonitor(final long stopTimeMillis,
				final boolean onlyOneJustification) {
			this.stopTimeMillis_ = stopTimeMillis;
			this.onlyOneJustification_ = onlyOneJustification;
		}

		@Override
		public boolean isInterrupted() {
			if (stopTimeMillis_ < System.currentTimeMillis()) {
				cancelled = true;
			}
			return cancelled;
		}

		@Override
		public void newJustification() {
			count_++;
			if (onlyOneJustification_) {
				cancelled = true;
			}
		}

		public int getJustificationCount() {
			return count_;
		}

		public void startSatSolver() {
			startSatSolverTime = System.nanoTime();
		}

		public void stopSatSolver() {
			satSolverTime_ += System.nanoTime()-startSatSolverTime;
		}

		public long getSATSolverTime() {
			return satSolverTime_;
		}

		public void startJustComp() {
			startSatSolverTime = System.nanoTime();
		}

		public void stopJustComp() {
			justCompTime_ += System.nanoTime()-startSatSolverTime;
		}

		public long getJustCompTime() {
			return justCompTime_;
		}

		public void startCycleComp() {
			startSatSolverTime = System.nanoTime();
		}

		public void stopCycleComp() {
			cycleCompTime_ += System.nanoTime()-startSatSolverTime;
		}

		public long getCycleCompTime() {
			return cycleCompTime_;
		}

	}