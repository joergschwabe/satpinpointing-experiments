package com.github.joergschwabe;

import java.util.HashSet;
import java.util.Set;

import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceDerivabilityChecker;

/**
 * 
 * @author Jörg Schwabe
 *
 * @param <Object> the type of axioms used by the inferences
 */
public class SatClauseHandler<I extends Inference<?>, A> {

	private IdProvider<A, I> idProvider;
	private InferenceDerivabilityChecker<Object, Inference<?>> infDeriv;
	private int queryId;

	public SatClauseHandler(IdProvider<A, I> idProvider, InferenceDerivabilityChecker<Object, Inference<?>> infDeriv, Integer queryId) {
		this.idProvider = idProvider;
		this.infDeriv = infDeriv;
		this.queryId = queryId;
	}

	Set<A> translateToAxioms(Set<Integer> axiomSet) {
		Set<A> translated_axiomSet = new HashSet<>();
		for (Integer axiomId : axiomSet) {
			translated_axiomSet.add(idProvider.getAxiomFromId(axiomId));
		}
		return translated_axiomSet;
	}

	Set<Integer> computeMinimalRepair(Set<Integer> repair) {
		Set<Integer> minRepair = new HashSet<Integer>();

		for (Integer axiomId : repair) {
			infDeriv.block(axiomId);
		}

		for (Integer axiomId : repair) {
			infDeriv.unblock(axiomId);

			if (infDeriv.isDerivable(queryId)) {
				minRepair.add(axiomId);
				infDeriv.block(axiomId);
			}
		}

		for (Integer axiomId : minRepair) {
			infDeriv.unblock(axiomId);
		}

		return minRepair;
	}

	public boolean isQueryDerivable(Set<Integer> axiomSet) {
		Set<Integer> axiomSetAll = new HashSet<Integer>(idProvider.getAxiomIds());
		axiomSetAll.removeAll(axiomSet);
		for (Integer axiomId : axiomSetAll) {
			infDeriv.block(axiomId);
		}
		boolean isDerivable = false;
		if (infDeriv.isDerivable(queryId)) {
			isDerivable = true;
		}
		for (Integer axiomId : axiomSetAll) {
			infDeriv.unblock(axiomId);
		}
		return isDerivable;
	}

	Set<Integer> computeJustification(Set<Integer> axiomSet) {
		Set<Integer> justification = new HashSet<Integer>();
		Set<Integer> axiomSetAll = new HashSet<Integer>(idProvider.getAxiomIds());
		axiomSetAll.removeAll(axiomSet);
		
		for (Integer axiomId : axiomSetAll) {
			infDeriv.block(axiomId);
		}
		
		for (Integer axiomId : axiomSet) {
			infDeriv.block(axiomId);
			
			if (!infDeriv.isDerivable(queryId)) {
				justification.add(axiomId);
				infDeriv.unblock(axiomId);
			}
		}
		
		axiomSetAll = new HashSet<Integer>(idProvider.getAxiomIds());
		for (Integer axiomId : axiomSetAll) {
			infDeriv.unblock(axiomId);
		}		
		
		return justification;
	}
}