package edu.udel.cis.vsl.gmc;

import java.io.File;
import java.io.PrintStream;

/**
 * An entry in an error log. Each entry reports one error encountered during a
 * model checking session. The application must extend this abstract class to a
 * concrete class by implementing several methods.
 * 
 * @author siegel
 * 
 */
public abstract class LogEntry implements Comparable<LogEntry> {

	/**
	 * The ID number of this log entry, unique among entries in the log.
	 */
	private int id;

	/**
	 * The length of the trace corresponding to this log entry
	 */
	private int size;

	/**
	 * The file containing the trace for this log entry
	 */
	private File traceFile;

	/**
	 * Default construtor: does nothing.
	 */
	public LogEntry() {
	}

	/**
	 * Prints an explanation of the error. Make this as precise as possible,
	 * showing relevant variable values, line numbers, and other source
	 * information.
	 * 
	 * @param out
	 *            the stream to which to print
	 */
	public abstract void printBody(PrintStream out);

	/**
	 * Compares this with another log entry in the natural order. The order
	 * should be defined so that higher priority errors come first. Should be
	 * compatible with equals.
	 */
	@Override
	public abstract int compareTo(LogEntry that);

	/**
	 * Determines whether this log entry is equal to the given object. The given
	 * object must be a log entry. Two entries may be considered equal if they
	 * represent "essentially" the same error. For example, two violations of
	 * the same assertion may be considered equal. The exact choice depends on
	 * how many errors you want to report. The log will only record one error
	 * from each equivalence class under the equivalence relation defined by
	 * this equals method.
	 */
	@Override
	public abstract boolean equals(Object obj);

	/**
	 * Retunrs the ID number of this log entry, which should be unique within
	 * the log to which it belongs.
	 * 
	 * @return the ID number of this log entry
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the ID number of this entry. This is used by the error log, it
	 * should not be done by the application, hence it is package private.
	 * 
	 * @param id
	 *            ID number for this entry
	 */
	void setId(int id) {
		this.id = id;
	}

	public File getTraceFile() {
		return traceFile;
	}

	/**
	 * Sets the file containing the trace corresponding to this entry. Used by
	 * the error log.
	 * 
	 * @param file
	 *            the file containing the trace
	 */
	void setTraceFile(File file) {
		this.traceFile = file;
	}

	/**
	 * Returns the length of the trace.
	 * 
	 * @return the length of the trace
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Sets the length of the traace.
	 * 
	 * @param size
	 *            length of trace
	 */
	void setSize(int size) {
		this.size = size;
	}

	/**
	 * Prints this log entry to the given stream.
	 * 
	 * @param out
	 *            stream to which to print
	 */
	public void print(PrintStream out) {
		out.println("Error " + id + "[length=" + size + ", file=" + traceFile
				+ "]:");
		printBody(out);
		out.flush();
	}

}
