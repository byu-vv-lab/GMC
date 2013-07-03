package edu.udel.cis.vsl.gmc;

import java.io.PrintStream;

public interface StateManagerIF<STATE, TRANSITION> {

	STATE nextState(STATE state, TRANSITION transition);

	void setSeen(STATE state, boolean value);

	boolean seen(STATE state);

	void setOnStack(STATE state, boolean value);

	boolean onStack(STATE State);

	void printStateShort(PrintStream out, STATE state);

	void printStateLong(PrintStream out, STATE state);

	void printTransitionShort(PrintStream out, TRANSITION transition);

	void printTransitionLong(PrintStream out, TRANSITION transition);

	void printAllStatesShort(PrintStream out);

	void printAllStatesLong(PrintStream out);

}