package edu.udel.cis.vsl.gmc;

import java.io.PrintWriter;

/**
 * An Enabler tells you which transitions should be explored from a given state.
 * It might need to know things about the state of the search (such as what
 * states are currently on the DFS stack). Such information can be provided at
 * creation.
 * 
 */
public interface EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> {

	/**
	 * Returns TransitionSequence of enabled transitions departing from given
	 * source state.
	 */
	TRANSITIONSEQUENCE enabledTransitions(STATE source);

	/**
	 * Returns source state.
	 */
	STATE source(TRANSITIONSEQUENCE sequence);

	/**
	 * Is there a next transition in the sequence?
	 * 
	 * @param sequence
	 * @return
	 */
	boolean hasNext(TRANSITIONSEQUENCE sequence);

	/**
	 * Moves to next transition in the sequence and returns that transition. If
	 * there is no next transition, an exception should be thrown.
	 * 
	 * @param sequence
	 * @return
	 */
	TRANSITION next(TRANSITIONSEQUENCE sequence);

	/**
	 * Returns the same value returned by next, but does not change the state of
	 * the sequence.
	 * 
	 * @param sequence
	 * @return
	 */
	TRANSITION peek(TRANSITIONSEQUENCE sequence);

	void print(PrintWriter out, TRANSITIONSEQUENCE sequence);

	/**
	 * Prints some representation of the transitions remaining after removing
	 * the first.
	 * 
	 * @param out
	 * @param sequence
	 */
	void printRemaining(PrintWriter out, TRANSITIONSEQUENCE sequence);

	void setDebugging(boolean value);

	boolean debugging();

	void setDebugOut(PrintWriter out);

	PrintWriter getDebugOut();

	/**
	 * Prints the current first transition in the sequence, preceded by the
	 * index of the transition within the sequence.
	 */
	void printFirstTransition(PrintWriter out, TRANSITIONSEQUENCE sequence);

}
