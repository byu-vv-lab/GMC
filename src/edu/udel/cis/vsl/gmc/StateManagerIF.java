package edu.udel.cis.vsl.gmc;

import java.io.PrintWriter;

public interface StateManagerIF<STATE, TRANSITION> {

	STATE nextState(STATE state, TRANSITION transition);

	void setSeen(STATE state, boolean value);

	boolean seen(STATE state);

	void setOnStack(STATE state, boolean value);

	boolean onStack(STATE State);

	void printStateShort(PrintWriter out, STATE state);

	void printStateLong(PrintWriter out, STATE state);

	void printTransitionShort(PrintWriter out, TRANSITION transition);

	void printTransitionLong(PrintWriter out, TRANSITION transition);

	void printAllStatesShort(PrintWriter out);

	void printAllStatesLong(PrintWriter out);

}