package edu.udel.cis.vsl.gmc;

import java.io.PrintStream;
import java.util.Stack;

/**
 * A DfsSearcher performs a simple depth-first search of the state space of a
 * transition system, stopping immediately if it finds a state satisfying the
 * given predicate. A DfsSearcher is instantiated with a given enabler (an
 * object which tells what transitions to explore from a given state), a state
 * manager, a predicate, and a state from which to start the search.
 */
public class DfsSearcher<STATE, TRANSITION, TRANSITIONSEQUENCE> {

	private EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> enabler;

	private StateManagerIF<STATE, TRANSITION> manager;

	private StatePredicateIF<STATE> predicate;

	private Stack<TRANSITIONSEQUENCE> stack;

	private boolean reportCycleAsViolation = false;

	private boolean cycleFound = false;

	private int numTransitions = 0;

	private int numStatesMatched = 0;

	private int numStatesSeen = 1;

	private PrintStream debugOut;

	private boolean debugging = false;

	private String name = null;

	private int summaryCutOff = 5;

	public DfsSearcher(
			EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> enabler,
			StateManagerIF<STATE, TRANSITION> manager,
			StatePredicateIF<STATE> predicate, PrintStream debugOut) {

		if (enabler == null) {
			throw new NullPointerException("null enabler");
		}
		if (manager == null) {
			throw new NullPointerException("null manager");
		}
		this.enabler = enabler;
		this.manager = manager;
		this.predicate = predicate;
		this.debugOut = debugOut;
		if (debugOut != null) {
			this.debugging = true;
		}
		stack = new Stack<TRANSITIONSEQUENCE>();
	}

	public StatePredicateIF<STATE> predicate() {
		return predicate;
	}

	public DfsSearcher(
			EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> enabler,
			StateManagerIF<STATE, TRANSITION> manager,
			StatePredicateIF<STATE> predicate) {
		this(enabler, manager, predicate, null);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public boolean reportCycleAsViolation() {
		return this.reportCycleAsViolation;
	}

	/**
	 * If you want to check for cycles in the state space, and report the
	 * existence of a cycle as a violation, this flag should be set to true.
	 * Else set it to false. By default, it is false.
	 */
	public void setReportCycleAsViolation(boolean value) {
		this.reportCycleAsViolation = value;
	}

	/**
	 * If reportCycleAsViolation is true, and the search terminates with a
	 * "true" value, then this method can be called to determine whether the
	 * predicate holds (indicating a standard property violation) or a cycle has
	 * been found.
	 */
	public boolean cycleFound() {
		return cycleFound;
	}

	/** Returns the state at the top of the stack, without modifying the stack. */
	public STATE currentState() {
		if (stack.isEmpty()) {
			return null;
		} else {
			return enabler.source(stack.peek());
		}
	}

	/** Returns the stack used to perform the depth first search */
	public Stack<TRANSITIONSEQUENCE> stack() {
		return stack;
	}

	/**
	 * Performs a depth-first search starting from the given state. Essentially,
	 * this pushes the given state onto the stack, making it the current state,
	 * and then invokes search().
	 */
	public boolean search(STATE initialState) {
		stack.push(enabler.enabledTransitions(initialState));
		manager.setSeen(initialState, true);
		manager.setOnStack(initialState, true);
		if (debugging) {
			debugOut.println("Pushed initial state onto stack " + name + ":\n");
			manager.printStateLong(debugOut, initialState);
			debugOut.println();
			debugOut.flush();
		}
		return search();
	}

	/**
	 * Returns true iff predicate holds at some state reachable (by a legal
	 * trace) from the current state, including the current state. If this is
	 * the case, this will return true when first state satisfying predicate is
	 * found in search. Once true is returned you may call print trace to get
	 * the trace. If false is returned, then this has gone through all legal
	 * traces.
	 * 
	 * If reportCycleAsViolation is true, this will also terminate and return
	 * true if a cycle in the state space has been found. The final state in the
	 * trace will also be the one which occurs earlier in the trace, forming a
	 * cycle.
	 */
	public boolean search() {
		while (!predicate.holdsAt(currentState())) {
			debug("Predicate does not hold at current state of " + name + ".");
			if (!proceedToNewState()) {
				if (cycleFound) {
					debug("Cycle found in state space.");
					return true;
				}
				debug("Search complete: predicate " + predicate
						+ " does not hold at " + "any reachable state of "
						+ name + ".\n");
				return false;
			}
		}
		debug("Predicate " + predicate + " holds at current state of " + name
				+ ": terminating search.\n");
		return true;
	}

	/**
	 * Proceeds with the search until we arrive at a state that has not been
	 * seen before (assuming there is one). In this case it marks the new state
	 * as seen, pushes it on the stack, and marks it as on the stack, and then
	 * returns true. If it finishes searching without finding a new state, it
	 * returns false.
	 * <p>
	 * 
	 * The search proceeds in the depth-first manner. The last transition
	 * sequence is obtained from the stack; these are the enabled transitions
	 * departing from the current state. The first transition in this sequence
	 * is applied to the current state. If the resulting state has not been seen
	 * before, we are done. Otherwise, the next transition is tried, and so on.
	 * If all these transitions are exhausted we proceed as follows: if the
	 * stack is empty, the search has completed and false is returned.
	 * Otherwise, the stack is popped, and the list of remaining transitions to
	 * be explored for the new current state is used, and we proceed as before.
	 * If this is again exhausted, we pop and repeat.
	 */
	public boolean proceedToNewState() {
		/*
		 * was the last stack operation a "pop"? Not necessary for the
		 * algorithm, but used to report on nondeterminism.
		 */
		// boolean popped = false;

		while (!stack.isEmpty()) {
			TRANSITIONSEQUENCE sequence = stack.peek();
			STATE currentState = enabler.source(sequence);

			while (enabler.hasNext(sequence)) {
				TRANSITION transition = enabler.peek(sequence);
				STATE newState = manager.nextState(currentState, transition);

				numTransitions++;
				// if (debugging && popped) {
				// // TODO: think of better way to summarize this
				// // information and print at end in report:
				// debug("** Another transition from " + currentState
				// + " has been found:\n" + transition);
				// }

				// if (popped) {
				// System.out.println("** Another transition from "
				// + currentState + " has been found:\n" + transition);
				// System.out.flush();
				// }
				if (!manager.seen(newState)) {
					assert !manager.onStack(newState);
					if (debugging) {
						debugOut.println("New state of " + name + " is "
								+ newState + ":");
						debugOut.println();
						manager.printStateLong(debugOut, newState);
						debugOut.println();
						debugOut.flush();
					}
					stack.push(enabler.enabledTransitions(newState));
					manager.setSeen(newState, true);
					manager.setOnStack(newState, true);
					numStatesSeen++;
					debugPrintStack("Pushed " + newState + " onto the stack "
							+ name + ". ", false);
					// popped = false;
					return true;
				}
				debug(newState + " seen before!  Moving to next transition.");
				if (reportCycleAsViolation && manager.onStack(newState)) {
					cycleFound = true;
					return false;
				}
				numStatesMatched++;
				enabler.next(sequence);
			}
			manager.setOnStack(enabler.source(stack.pop()), false);
			// popped = true;
			if (!stack.isEmpty())
				enabler.next(stack.peek());
			debugPrintStack("Popped stack.", false);
		}
		return false;
	}

	public void setDebugging(boolean value) {
		debugging = value;
	}

	boolean debugging() {
		return debugging;
	}

	public void setDebugOut(PrintStream out) {
		if (out == null) {
			throw new NullPointerException("null out");
		}
		debugOut = out;
	}

	public PrintStream getDebugOut() {
		return debugOut;
	}

	protected void debug(String s) {
		if (debugging) {
			debugOut.println(s);
			debugOut.flush();
		}
	}

	public void printStack(PrintStream out, boolean longFormat,
			boolean summarize) {
		int size = stack.size();

		if (size == 0) {
			out.println("  <EMPTY>");
		}
		for (int i = 0; i < size; i++) {
			TRANSITIONSEQUENCE sequence = stack.elementAt(i);
			STATE state = enabler.source(sequence);

			if (!summarize || i <= 1 || size - i < summaryCutOff - 1) {
				if (i > 0) {
					out.print(" -> ");
					manager.printStateShort(out, state);
					out.println();
				}
				if (longFormat) {
					out.println();
					manager.printStateLong(out, state);
					out.println();
				}
			}
			if (summarize && size - i == summaryCutOff - 1) {
				for (int j = 0; j < 3; j++)
					out.println("     .");
			}
			if (!summarize || i <= 0 || size - i < summaryCutOff) {
				out.print("Step " + (i + 1) + ": ");
				manager.printStateShort(out, state);
				if (enabler.hasNext(sequence)) {
					out.print(" --");
					enabler.printFirstTransition(out, sequence);
				}
				out.flush();
			}
		}
		out.println();
		out.flush();
	}

	public void printStack(PrintStream out) {
		if (name != null)
			out.print(name + " ");
		out.println("Trace summary:\n");
		printStack(out, false, false);
		out.println();
		if (name != null)
			out.print(name + " ");
		out.println("Trace details:");
		printStack(out, true, false);
	}

	void debugPrintStack(String s, boolean longFormat) {
		if (debugging) {
			debugOut.println(s + "  New stack for " + name + ":\n");
			printStack(debugOut, longFormat, true);
			debugOut.println();
		}
	}

	void debugStates(String s) {
		if (debugging) {
			debugOut.println(s + "All states for " + name + ":\n");
			manager.printAllStatesLong(debugOut);
			debugOut.println();
			printSummary(debugOut);
		} else {
			// debugOut.println(s);
			// printSummary(debugOut);
		}
	}

	public int numStatesSeen() {
		return numStatesSeen;
	}

	public int numTransitions() {
		return numTransitions;
	}

	public int numStatesMatched() {
		return numStatesMatched;
	}

	public void printSummary(PrintStream out) {
		out.println("Number of states seen:    " + numStatesSeen);
		out.println("Number of transitions:   " + numTransitions);
		out.println("Number of states matched: " + numStatesMatched + "\n");
		out.flush();
	}
}
