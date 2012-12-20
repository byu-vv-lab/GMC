package edu.udel.cis.vsl.gmc;

public interface StatePredicateIF<S> {

	boolean holdsAt(S state);

	/**
	 * Returns a human-readable explanation of why the predicate does or does
	 * not hold
	 */
	String explanation();

}
