package edu.udel.cis.vsl.gmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A log for recording errors and corresponding traces encountered during a
 * model checking run.
 * 
 * @author siegel
 * 
 */
public class ErrorLog {

	// Instance fields...

	private DfsSearcher<?, ?, ?> searcher;

	/**
	 * Map used for storing the reported exceptions. The value is the length of
	 * the shortest trace encountered for that exception.
	 */
	private SortedMap<LogEntry, LogEntry> entryMap;

	/**
	 * Directory in which the log and trace files will be stored.
	 */
	private File directory;

	/**
	 * Names of the session: this name will be used to form the file name of all
	 * files created by this log.
	 */
	private String sessionName;

	/**
	 * Time and date at which this log was created.
	 */
	private Date date;

	/**
	 * The total number of errors reported to this log. This may be greater than
	 * the number stored by this log, because many of the errors reported may be
	 * equivalent, and only one from each equivalence class is stored.
	 */
	private int numErrors = 0;

	/**
	 * Total number of errors that can be reported before this log thrown an
	 * ExcessiveErrorException;
	 */
	private int errorBound = 10;

	private PrintStream out;

	/**
	 * Creates new ErrorLog with given comparator
	 * 
	 */
	public ErrorLog(File directory, String sessionName, PrintStream out) {
		this.out = out;
		if (!directory.exists()) {
			directory.mkdir();
		}
		if (!directory.isDirectory())
			throw new IllegalArgumentException("No directory named "
					+ directory);
		if (sessionName == null)
			throw new IllegalArgumentException("Session name is null");
		this.directory = directory;
		this.sessionName = sessionName;
		this.entryMap = new TreeMap<>();
		this.date = new Date();
	}

	// Helper methods...

	private String traceFileName(int i) {
		return sessionName + "_" + i + ".trace";
	}

	private File traceFile(int i) {
		return new File(directory, traceFileName(i));
	}

	// Public methods...

	public void setSearcher(DfsSearcher<?, ?, ?> searcher) {
		this.searcher = searcher;
	}

	public DfsSearcher<?, ?, ?> searcher() {
		return searcher;
	}

	public int errorBound() {
		return errorBound;
	}

	public void setErrorBound(int value) {
		this.errorBound = value;
	}

	public int numErrors() {
		return numErrors;
	}

	public int numEntries() {
		return entryMap.size();
	}

	public void print(PrintStream out) {
		out.println("Session name....... " + sessionName);
		out.println("Directory.......... " + directory);
		out.println("Date............... " + date);
		out.println("numErrors.......... " + numErrors);
		out.println("numDistinctErrors.. " + entryMap.size());
		out.println();
		for (LogEntry entry : entryMap.values()) {
			entry.print(out);
			out.println();
		}
	}

	// add another argument preamble which has a method "print"
	// to print a prefix to the file...

	public void report(LogEntry entry) throws FileNotFoundException {
		int length = searcher.stack().size();
		LogEntry oldEntry = entryMap.get(entry);

		out.println("Error " + numErrors + " encountered at depth " + length
				+ ":");
		entry.printBody(out);
		if (oldEntry != null) {
			int id = oldEntry.getId();
			File file = oldEntry.getTraceFile();
			int oldLength = oldEntry.getSize();

			out.println("New log entry is equivalent to previously encountered entry "
					+ id);
			if (length < oldLength) {
				out.println("Length of new trace (" + length
						+ ") is less than length of old (" + oldLength
						+ "): replacing old with new...");
				entry.setSize(length);
				entry.setTraceFile(file);
				entryMap.remove(entry);
				entryMap.put(entry, entry);
				file.delete();
				searcher.saveStack(file);
			} else {
				out.println("Length of new trace (" + length
						+ ") is greater than or equal to length of old ("
						+ oldLength + "): ignoring new trace.");
			}
		} else {
			int id = entryMap.size();
			File file = traceFile(id);

			out.println("Logging new entry " + id + ", writing trace to "
					+ file);
			entry.setTraceFile(file);
			entry.setId(id);
			entry.setSize(length);
			entryMap.put(entry, entry);
			searcher.saveStack(file);
		}
		numErrors++;
	}

}
