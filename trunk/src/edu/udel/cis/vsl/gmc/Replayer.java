package edu.udel.cis.vsl.gmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

/**
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
public class Replayer<STATE, TRANSITION, TRANSITIONSEQUENCE> {

	// Instance fields...

	/**
	 * The enabler: the object used to determine the set of enabled transitions
	 * from any given state.
	 */
	private EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> enabler;

	/**
	 * The state manager: the object used to determine the next state given a
	 * state and a transition.
	 */
	private StateManagerIF<STATE, TRANSITION> manager;

	/**
	 * The stream to which the human-readable output should be sent when
	 * replayin a trace.
	 */
	private PrintStream out;

	/**
	 * Print the states at each step in the trace? If this is false, only the
	 * initial and the final states will be printed.
	 */
	private boolean printAllStates = true;

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
	public Replayer(EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> enabler,
			StateManagerIF<STATE, TRANSITION> manager, PrintStream out) {
		this.enabler = enabler;
		this.manager = manager;
		this.out = out;
	}

	// Static methods....

	private static void err(String message) {
		throw new RuntimeException("Replay error: " + message);
	}

	/**
	 * Creates a guide by parsing from the given buffered reader. This interface
	 * is provided because the buffered reader may point to the middle of a
	 * file. This is provided because the first part of the file might contain
	 * some application-specific information (such as parameter values), and the
	 * part containing the sequence of integers may start in the middle. This
	 * will parse to the end of the file so expects to see a newline-separated
	 * sequence of integers from the given point on. Closes the reader at the
	 * end.
	 * 
	 * @param reader
	 *            a buffered reader containing a newline-separated sequence of
	 *            integers
	 * @return the sequence of integers
	 * @throws IOException
	 *             if an error occurs in reading from or closing the buffered
	 *             reader
	 */
	public static int[] makeGuide(BufferedReader reader) throws IOException {
		List<Integer> intList = new LinkedList<Integer>();
		int numInts, count;
		int[] guide;

		while (true) {
			String line = reader.readLine();

			if (line == null)
				throw new RuntimeException("Trace begin line not found");
			line = line.trim();
			if ("== Begin Trace==".equals(line))
				break;
		}

		while (true) {
			String line = reader.readLine();

			if (line == null)
				break; // end has been reached
			line = line.trim(); // remove white space
			if ("== End Trace==".equals(line))
				break;
			if (line.isEmpty())
				continue; // skip blank line
			try {
				int theInt = new Integer(line);

				if (theInt < 0) {
					err("Malformed trace file: transition index is negative: "
							+ theInt);
				}
				intList.add(new Integer(theInt));
			} catch (NumberFormatException e) {
				err("Expected integer, saws " + line);
			}
		}
		reader.close();
		numInts = intList.size();
		guide = new int[numInts];
		count = 0;
		for (Integer value : intList) {
			guide[count] = value;
			count++;
		}
		return guide;
	}

	/**
	 * Creates a guide by parsing a file which is a newline-separated list of
	 * integers.
	 * 
	 * @param file
	 *            a newline-separated list of integers
	 * @return the integers, as an array
	 * @throws IOException
	 *             if a problem occurs in opening, reading from, or closing the
	 *             file
	 */
	public static int[] makeGuide(File file) throws IOException {
		return makeGuide(new BufferedReader(new FileReader(file)));
	}

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
				out.println("State " + step + executionNames[i] + ":");
				manager.printStateLong(out, states[i]);
				out.println();
			}
		}
	}

	// Instance methods: public...

	public void setPrintAllStates(boolean value) {
		this.printAllStates = value;
	}

	public boolean getPrintAllStates() {
		return printAllStates;
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
	 *            which states should be printed. Array of length states.length.
	 * @param names
	 *            the names to use for the different executions. Array of length
	 *            states.length
	 * @param guide
	 *            sequence of integers used to guide execution when a state is
	 *            reached that has more than one enabled transition. The initial
	 *            state of index 0 is the one that will work with the guide
	 */
	public void play(STATE states[], boolean[] print, String[] names,
			int guide[]) {
		int numExecutions = states.length;
		int guideLength = guide.length;
		int guideIndex = 0;
		int step = 0;
		String[] executionNames = new String[numExecutions];

		for (int i = 0; i < numExecutions; i++) {
			String name = names[i];

			if (name == null)
				executionNames[i] = "";
			else
				executionNames[i] = " (" + names + ")";
		}
		printStates(step, numExecutions, executionNames, print, states);
		while (true) {
			TRANSITIONSEQUENCE sequence = enabler.enabledTransitions(states[0]);
			TRANSITION transition;

			if (!enabler.hasNext(sequence))
				break;
			transition = enabler.next(sequence);
			if (enabler.hasMultiple(sequence)) {
				int index;

				if (guideIndex >= guideLength) {
					out.println("Trail ends before execution terminates.");
					break;
				}
				index = guide[guideIndex];
				for (int i = 0; i < index; i++)
					transition = enabler.next(sequence);
				guideIndex++;
			}
			step++;
			out.print("Step " + step + ": ");
			manager.printTransitionLong(out, transition);
			out.println();
			for (int i = 0; i < numExecutions; i++)
				states[i] = manager.nextState(states[i], transition);
			// TODO: question: can the same transition be re-used?
			// this is not specified in the contract and in some cases
			// info is cached in the transition. Maybe duplicate the
			// transition, or clear it???
			if (printAllStates)
				printStates(step, numExecutions, executionNames, print, states);
		}
	}

	public void play(STATE initialStates[], boolean[] print, String[] names,
			File traceFile) throws IOException {
		play(initialStates, print, names, makeGuide(traceFile));
	}

	public void play(STATE initialState, boolean print, int[] guide)
			throws IOException {
		@SuppressWarnings("unchecked")
		STATE[] stateArray = (STATE[]) new Object[] { initialState };
		boolean[] printArray = new boolean[] { print };
		String[] names = new String[] { null };

		play(stateArray, printArray, names, guide);
	}

	public void play(STATE initialState, boolean print, File traceFile)
			throws IOException {
		play(initialState, print, makeGuide(traceFile));
	}

	public void play(STATE initialSymbolicState, STATE initialConcreteState,
			boolean printSymbolicStates, int[] guide) {
		@SuppressWarnings("unchecked")
		STATE[] stateArray = (STATE[]) new Object[] { initialSymbolicState,
				initialConcreteState };
		boolean[] printArray = new boolean[] { printSymbolicStates, true };
		String[] names = new String[] { "Symbolic", "Concrete" };

		play(stateArray, printArray, names, guide);
	}

	public void play(STATE initialSymbolicState, STATE initialConcreteState,
			boolean printSymbolicStates, File traceFile) throws IOException {
		play(initialSymbolicState, initialConcreteState, printSymbolicStates,
				makeGuide(traceFile));
	}
}
