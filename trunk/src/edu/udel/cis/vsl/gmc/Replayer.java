package edu.udel.cis.vsl.gmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

//TODO: Need the ability to track two executions at once when
//replaying a trace.  One will start with symbolic state
//u0.  Other will start with concrete state v0.  The 
//symbolic enabler will be used to select the next
//transition at each step.   That one transition will
//execute in both spaces.   Print just the concrete one,
//or both.

/**
 * A Replayer is used to replay an execution trace of a transition system. The
 * trace is typically stored in a file created by method
 * {@link DfsSearcher#saveStack(File)}.
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

	private EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> enabler;

	private StateManagerIF<STATE, TRANSITION> manager;

	private int[] guide;

	private STATE initialState;

	private PrintStream out;

	/** Print the states at each step in the trace? */
	private boolean printStates = true;

	/**
	 * 
	 * @param enabler
	 *            enabler used to determine the set of enabled transitions at a
	 *            given state
	 * @param manager
	 *            state manager; used to compute the next state given a state
	 *            and transition
	 * @param guide
	 *            sequence of integers used to guide execution when a state is
	 *            reached that has more than one enabled transition.
	 * @param initialState
	 *            the state from which the execution should start
	 * @param out
	 *            stream to which the trace should be written in human-readable
	 *            form
	 */
	public Replayer(EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> enabler,
			StateManagerIF<STATE, TRANSITION> manager, int[] guide,
			STATE initialState, PrintStream out) {
		this.enabler = enabler;
		this.manager = manager;
		this.guide = guide;
		this.initialState = initialState;
		this.out = out;
	}

	public Replayer(EnablerIF<STATE, TRANSITION, TRANSITIONSEQUENCE> enabler,
			StateManagerIF<STATE, TRANSITION> manager, File trace,
			STATE initialState, PrintStream out) throws IOException {
		// convert file to guide...
		this(enabler, manager, makeGuide(trace), initialState, out);
	}

	public void setPrintStates(boolean value) {
		this.printStates = value;
	}

	public boolean getPrintStates() {
		return printStates;
	}

	private static void err(String message) {
		throw new RuntimeException("Replay error: " + message);
	}

	private static int[] makeGuide(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		List<Integer> intList = new LinkedList<Integer>();
		int numInts, count;
		int[] guide;

		while (true) {
			String line = reader.readLine();

			if (line == null)
				break; // end has been reached
			line = line.trim(); // remove white space
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
	 * Plays the trace.
	 */
	public void play() {
		int guideLength = guide.length;
		int guideIndex = 0;
		int step = 0;
		STATE state = initialState;

		out.println("State " + step + ":");
		manager.printStateLong(out, state);
		out.println();
		while (true) {
			TRANSITIONSEQUENCE sequence = enabler.enabledTransitions(state);
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
			state = manager.nextState(state, transition);
			if (printStates) {
				out.println("State " + step + ":");
				manager.printStateLong(out, state);
				out.println();
			}
		}
	}
}
