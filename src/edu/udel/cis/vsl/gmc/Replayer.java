package edu.udel.cis.vsl.gmc;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * CHANGE THE NAME. This is now more general. It is used to execute the system
 * using any chooser.
 * 
 * A Replayer is used to replay an execution trace of a transition system. The
 * trace is typically stored in a file created by method
 * {@link DfsSearcher#writeStack(File)}.
 * 
 * @author siegel
 * 
 * @param <STATE>
 *            the type for the states in the transition system
 * @param <TRANSITION>
 *            the type for the transitions in the transition system
 * @param <TRANSITIONSEQUENCE>
 *            the type for a sequence of transitions emanating from a single
 *            state
 */
public class Replayer<STATE, TRANSITION> {

	// Instance fields...

	/**
	 * The state manager: the object used to determine the next state given a
	 * state and a transition.
	 */
	private StateManagerIF<STATE, TRANSITION> manager;

	/**
	 * The stream to which the human-readable output should be sent when
	 * replaying a trace.
	 */
	private PrintStream out;

	/**
	 * Print the states at each step in the trace? If this is false, only the
	 * initial and the final states will be printed.
	 */
	private boolean printAllStates = true;

	private StatePredicateIF<STATE> predicate = null;

	private ErrorLog log = null;

	/**
	 * How long the execution should be, if known. If unknown, it is -1.
	 */
	private int length = -1;

	// Constructors...

	/**
	 * 
	 * @param enabler
	 *            enabler used to determine the set of enabled transitions at a
	 *            given state
	 * @param manager
	 *            state manager; used to compute the next state given a state
	 *            and transition
	 * @param out
	 *            stream to which the trace should be written in human-readable
	 *            form
	 */
	public Replayer(StateManagerIF<STATE, TRANSITION> manager, PrintStream out) {
		this.manager = manager;
		this.out = out;
	}

	// Static methods....

	// Instance methods: helpers...

	/**
	 * Prints out those states which should be printed. A utility method used by
	 * play method.
	 * 
	 * @param step
	 *            the step number to use in the printout
	 * @param numStates
	 *            the number of states in the array states
	 * @param executionNames
	 *            the names to use for each state; array of length numStates
	 * @param print
	 *            which states should be printed; array of boolean of length
	 *            numStates
	 * @param states
	 *            the states; array of STATE of length numStates
	 */
	private void printStates(int step, int numStates, String[] executionNames,
			boolean[] print, STATE[] states) {
		for (int i = 0; i < numStates; i++) {
			if (print[i]) {
				// out.println("State " + step + executionNames[i] + ":");
				out.println();
				manager.printStateLong(out, states[i]);
				out.println();
			}
		}
	}

	// Instance methods: public...

	public void setLength(int length) {
		this.length = length;
	}

	public int getLength() {
		return length;
	}

	public void setPredicate(StatePredicateIF<STATE> predicate) {
		this.predicate = predicate;
	}

	public StatePredicateIF<STATE> getPredicate() {
		return predicate;
	}

	public void setPrintAllStates(boolean value) {
		this.printAllStates = value;
	}

	public boolean getPrintAllStates() {
		return printAllStates;
	}

	public void setLog(ErrorLog log) {
		this.log = log;
	}

	public ErrorLog getLog() {
		return log;
	}

	/**
	 * Plays the trace. This method accepts an array of initial states, and will
	 * create executions in parallel, one for each initial state. All of the
	 * executions will use the same sequence of transitions, but may start from
	 * different initial states. The common use case has two initial states, the
	 * first one a symbolic state and the second a concrete state obtained by
	 * solving the path condition.
	 * 
	 * @param states
	 *            the states from which the execution should start. The first
	 *            state in the initial state (index 0) will be the one assumed
	 *            to execute according to the guide. This method will modify
	 *            this array so that upon returning the array will hold the
	 *            final states.
	 * @param print
	 *            which states should be printed at a point when states will be
	 *            printed. Array of length states.length.
	 * @param names
	 *            the names to use for the different executions. Array of length
	 *            states.length
	 * @param chooser
	 *            the object used to decide which transition to choose when more
	 *            than one is enabled at a state
	 * @throws MisguidedExecutionException
	 *             if the chooser does
	 */
	public boolean play(STATE states[], boolean[] print, String[] names,
			TransitionChooser<STATE, TRANSITION> chooser)
			throws MisguidedExecutionException {
		int numExecutions = states.length;
		int step = 0;
		String[] executionNames = new String[numExecutions];
		boolean violation = false;
		TRANSITION transition;

		for (int i = 0; i < numExecutions; i++) {
			String name = names[i];

			if (name == null)
				executionNames[i] = "";
			else
				executionNames[i] = " (" + names + ")";
		}
		out.println("\nInitial state:");
		printStates(step, numExecutions, executionNames, print, states);
		while (true) {
			if (predicate != null) {
				for (int i = 0; i < numExecutions; i++) {
					STATE state = states[i];

					if (predicate.holdsAt(state)) {
						if (!printAllStates) {
							out.println();
							manager.printStateLong(out, state);
						}
						out.println();
						out.println("Violation of " + predicate + " found in "
								+ state + ":");
						out.println(predicate.explanation());
						out.println();
						violation = true;
					}
				}
			}
			// at this point, step is the number of steps that have executed.
			// We want to play the last transition (represented by top
			// element in stack) because that could be where the error
			// happens...
			if (length >= 0 && step >= length)
				break;
			transition = chooser.chooseEnabledTransition(states[0]);
			if (transition == null)
				break;
			step++;
			out.print("\nTransition " + step + ": ");
			// manager.printTransitionLong(out, transition);
			// out.println();
			for (int i = 0; i < numExecutions; i++)
				states[i] = manager.nextState(states[i], transition);
			// TODO: question: can the same transition be re-used?
			// this is not specified in the contract and in some cases
			// info is cached in the transition. Maybe duplicate the
			// transition, or clear it???
			if (printAllStates)
				printStates(step, numExecutions, executionNames, print, states);
		}
		// always print the last state:
		// out.println("\nFinal state:"); commented out to avoid duplicated
		// printing of the final states
		// if (!printAllStates)
		// printStates(step, numExecutions, executionNames, print, states);
		out.println("Trace ends after " + step + " transitions.");
		return violation;
	}

	public boolean play(STATE initialState,
			TransitionChooser<STATE, TRANSITION> chooser)
			throws MisguidedExecutionException {
		@SuppressWarnings("unchecked")
		STATE[] stateArray = (STATE[]) new Object[] { initialState };
		boolean[] printArray = new boolean[] { true };
		String[] names = new String[] { null };

		return play(stateArray, printArray, names, chooser);
	}

	@SuppressWarnings("unchecked")
	public boolean replayForGui(STATE initialState,
			TransitionChooser<STATE, TRANSITION> chooser,
			ArrayList<STATE> states, ArrayList<TRANSITION> transitions)
			throws MisguidedExecutionException {
		boolean[] print = new boolean[] { true };
		int step = 0;
		String[] executionNames = new String[1];
		TRANSITION transition;
		boolean violation = false;
		STATE current = initialState;
		Object[] results;

		executionNames[0] = "";
		out.println("\nInitial state:");
		printStates(step, 1, executionNames, print,
				(STATE[]) new Object[] { initialState });
		while (true) {
			if (predicate != null) {
				if (predicate.holdsAt(current)) {
					if (!printAllStates) {
						out.println();
						manager.printStateLong(out, current);
					}
					out.println();
					out.println("Violation of " + predicate + " found in "
							+ current + ":");
					out.println(predicate.explanation());
					out.println();
					violation = true;
				}
			}
			// at this point, step is the number of steps that have executed.
			// We want to play the last transition (represented by top
			// element in stack) because that could be where the error
			// happens...
			if (length >= 0 && step >= length)
				break;
			transition = chooser.chooseEnabledTransition(current);
			if (transition == null)
				break;
			step++;
			out.print("\nTransition " + step + ": ");
			results = manager.nextStateForGui(current, transition);
			current = (STATE) results[0];
			transition = (TRANSITION) results[1];
			states.add(current);
			transitions.add(transition);
			// TODO: question: can the same transition be re-used?
			// this is not specified in the contract and in some cases
			// info is cached in the transition. Maybe duplicate the
			// transition, or clear it???
			if (printAllStates)
				printStates(step, 1, executionNames, print,
						(STATE[]) new Object[] { current });
		}
		out.println("Trace ends after " + step + " transitions.");

		return violation;
	}

	public boolean play(STATE initialSymbolicState, STATE initialConcreteState,
			boolean printSymbolicStates,
			TransitionChooser<STATE, TRANSITION> chooser)
			throws MisguidedExecutionException {
		@SuppressWarnings("unchecked")
		STATE[] stateArray = (STATE[]) new Object[] { initialSymbolicState,
				initialConcreteState };
		boolean[] printArray = new boolean[] { printSymbolicStates, true };
		String[] names = new String[] { "Symbolic", "Concrete" };

		return play(stateArray, printArray, names, chooser);
	}

}
