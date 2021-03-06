package makehandsons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** The MakeHandsOns program looks through the given
 * directories recursively, copying each file
 * to the output directory. If the file is a text file (ends in
 * .java or .xml or ...), do substitution on each line with a given
 * set of replacement patterns (patterns and their
 * replacements are loaded from a Properties file).
 * XXX This file is too big!
 * @author Ian Darwin
 */
public class MakeHandsOns {
	
	private static final String EXCLUDE_FILES_FILENAME = "exclude-files.txt";

	private static final String PROPERTIES_FILENAME = "/makehandsons.properties";

	Properties sysProps = System.getProperties();
	
	/** The file extens that get replacements done */
	final static String[] SUB_TEXT_FILE_EXTENS = {
		".adoc",
		".gradle",
		".html",
		".java",
		".jsf",
		".jsp",
		".project",
		".properties",
		".txt",
		".xhtml",
		".xml",
	};
	
	/** tld == Top Level Directory */
	static File tld = null;
	
	/** The part of the directory name that gets removed. */
	final static String REMOVE_FROM_PATH = "solution";
	
	private final static Pattern CUTMODE_START = Pattern.compile("\\s*//-");
	private final static Pattern CUTMODE_END = Pattern.compile("\\s*//\\+");

	private final static Pattern COMMENTMODE_START = Pattern.compile("^\\s*//C\\+");
	private final static Pattern COMMENTMODE_END = Pattern.compile("^\\s*//C\\-");

	
	private final static Pattern EXCHANGEMODE_START = Pattern.compile("\\s*//X\\+");
	private final static Pattern EXCHANGEMODE_END = Pattern.compile("\\s*//X\\-");	
	//-
	/* This should not appear in the output */
	//+
	//R // This should appear, and is a test for the "cut mode" process in processText()

	/** directories to ignore */
	final static String[] IGNORE_DIRS = { 
		"CVS", ".svn", ".git", ".metadata", "bin", "target", "build"
	};

	/** Map from a compiled regex Pattern to its replacement String */
	static Map<Pattern,String> pattMap;
	
	/** JUL logger */
	private static Logger log;

	static {

		InputStream logStream = MakeHandsOns.class.getClassLoader().getResourceAsStream("logging.properties");

		if (logStream != null) {
			try {
				LogManager.getLogManager().readConfiguration(logStream);
			} catch (SecurityException | IOException e) {
				System.err.println("Error loading logging.properties: " + e + "; trying anyway.");
			}
		}
		log = Logger.getLogger(MakeHandsOns.class.getName());
	}

	public static void main(String[] args) {
		System.out.println("Processing started (message via sysout)");
		try {
			MakeHandsOns prog = new MakeHandsOns();
			if (args.length == 0) {
				System.err.printf("Usage: %s directory [...]%n", MakeHandsOns.class.getSimpleName());
			} else if (args[0].equals("-h")) {
				doHelp();
			} else
				for (String arg : args) {
					if (".".equals(arg)) {
						System.err.println(
							"Sorry, you can't use '.', use '*solution' or individual solution directory.");
						System.exit(42);
					}
					File fileArg = new File(arg);
					prog.makeIgnoreList(fileArg);
					prog.descendFileSystem(fileArg);
				}
		} catch (Exception e) {
			System.out.println("Catastrophe: " + e.getMessage());
		}
	}
	
	static final String[] HELP_TEXT = {
		"This program generates the exercises from the solutions",
		"//T -> // TODO ",
		"//H -> // *HINT*",
		"//- -> enter cut mode",
		"//+ -> leave cut mode",
		"//C+ -> enter comments mode",
		"//C- -> leave comments mode",
		"//X+ :::textToReplace:::replacementText-> enter exchange mode",
		"//X- -> leave comments mode"		
	};
	
	private static void doHelp() {
		for (String h : HELP_TEXT) {
			System.out.println(h);
		}
	}

	MakeHandsOns() {


		log.info("Program starting");

		try (InputStream is = getClass().getResourceAsStream(PROPERTIES_FILENAME)) {
			if (is == null) {
				throw new RuntimeException("Could not load " + PROPERTIES_FILENAME + " from classpath.");
			}
			pattMap = loadPatterns(is);
		} catch (IOException ex) {
			throw new ExceptionInInitializerError("CANTHAPPEN, did");
		}
	}
	
	List<String> excludeFiles = new ArrayList<>();
	
	void makeIgnoreList(File dir) throws IOException {
		excludeFiles.clear();
		final File ignoreFilesFile = new File(dir, EXCLUDE_FILES_FILENAME);
		if (!ignoreFilesFile.exists()) {
			return;
		}
		try (BufferedReader is = new BufferedReader(new FileReader(ignoreFilesFile))) {
			String line;
			while ((line = is.readLine()) != null) {
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				excludeFiles.add(line);
			}
		}
	}

	private void checkIgnoredFileForMarkup(File file) throws IOException {
		if (!file.exists()) {
			log.warning(String.format("Excluded file %s doesn't exist", file));
			return;
		}
		try (BufferedReader is = new BufferedReader(new FileReader(file))) {
			String line = null;
			while ((line = is.readLine()) != null) {
				if (line.contains("//-") ||
					line.contains("//+")) {
					log.warning("Excluded file " + file + " appears to contain markup");
					return;	// Only natter once per file
				}
			}
		}
	}
	
	/** Load (and compile) the Pattern file, a list
	 * of x=y, where x is a regex pattern and y
	 * is a replacement value.
	 */
	private Map<Pattern,String> loadPatterns(InputStream is) {
		Properties p = new Properties();
		try {
			p.load(is);
		} catch (IOException e) {
			throw new RuntimeException("Error loading patterns", e);
		}
		
		Map<Pattern,String> pattMap = new HashMap<Pattern,String>();
		for (Object k : p.keySet()) {
			String key = (String)k;
			String repl = p.getProperty(key);
			log.finer("load: " + key + "->" + repl);
			// If key begins with ${...}, substitute key now
			// ("now" == at program start-up).
			if (key.charAt(0) == '$') {
				String propName = key.substring(2, key.length() - 1);
				String newKey = sysProps.getProperty(propName);
				log.fine(String.format(
					"loadPatterns(): prop %s -> %s", propName, newKey));
				key = newKey;
			}
			log.fine(String.format("loadPatterns() key '%s' value '%s'", key, repl));
			Pattern pat = Pattern.compile(key);
			pattMap.put(pat, repl);
		}
		return pattMap;
	}

	/** Work through the starting directory, mapping it to destDir 
	 * @throws IOException When Java IO does
	 */
	void descendFileSystem(File startDir) throws IOException {
		log.fine(String.format("FileSub.searchFiles(%s)%n", startDir));
		boolean setTld = false;
		if (tld == null) {
			tld = startDir;
			setTld = true;
		}
		if (startDir.isDirectory()) {
			String name = startDir.getName();
			for (String dir : IGNORE_DIRS) {
				if (dir.equals(name)) {
					log.finer("IGNORING " + startDir);
					return;
				}
			}
			File[] list = startDir.listFiles();
			for (File f : list) {
				descendFileSystem(f);		// recurse
			}
		} else if (startDir.isFile()) {
			processFile(startDir);
		} else {
			System.err.printf("Warning: %s neither file nor directory, ignoring%n", startDir);
		}
		if (setTld) {
			tld = null;
		}
	}

	boolean isTextFile(String fileName) {
		for (String exten : SUB_TEXT_FILE_EXTENS) {
			if (fileName.endsWith(exten))
				return true;
		}
		return false;
	}
	
	private void processFile(File file) throws IOException {
		String name = file.getName();
		String absPath = file.getAbsolutePath();
		String newAbsPath = absPath.replace(REMOVE_FROM_PATH, "");
		File newFile = new File(newAbsPath);
		log.fine("NEW ABS PATH = " + newAbsPath);
		if (name == null || name.length() == 0 ||
			EXCLUDE_FILES_FILENAME.equals(name)) {
			return;
		}
		if (excludeFiles.contains(name)) {
			checkIgnoredFileForMarkup(file);
			if (newFile.exists()) {
				log.severe("Excluded file exists: " + newAbsPath + "; nuking it!");
				newFile.delete();
				if (newFile./*still*/exists()) {
					// Fail early and often.
					throw new IllegalArgumentException("Sob! Tried to delete " + newAbsPath + " but failed!");
				}
			}
			return;
		}
		newFile.getParentFile().mkdirs();
		log.fine(String.format("FileSub.processFile(%s->%s)%n", file, newAbsPath));		
		if (isTextFile(file.getName())) {
			processTextFile(file);
		} else {						// copy as binary
			try {
				copyFile(file, newFile);
			} catch (IOException e) {
				System.err.println("Failed to copy " + file + "; " + e);
			}
		}		
	}
	
	private static int BLKSIZ = 4096;
	
	/** Copy a BINARY file. Copied from c.d.io.FileIO */
	public static void copyFile(File file, File target) throws IOException {
		if (!file.exists() || !file.isFile() || !(file.canRead())) {
			throw new IOException(file + " is not a readable file");
		}
		File dest = target;
		if (target.isDirectory()) {
			dest = new File(dest, file.getName());
		}
		InputStream is = null;
		OutputStream os  = null;
		try {
			is = new FileInputStream(file);
			os = new FileOutputStream(dest);
			int count = 0;		// the byte count
			byte[] b = new byte[BLKSIZ];	// the bytes read from the file
			while ((count = is.read(b)) != -1) {
				os.write(b, 0, count);
			}
		} finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
		}
	}
	
	/** Copy one TEXT file, with substitutions. */
	private void processTextFile(File file) {
		TextModes modes = new TextModes();
		try (
			PrintWriter pw = new PrintWriter(file.getAbsolutePath().replace(REMOVE_FROM_PATH, ""));
			BufferedReader is = new BufferedReader(new FileReader(file));
			) {
			String aline;
			List<String> lines = new ArrayList<>();
			while ((aline = is.readLine()) != null) {
				lines.add(aline);
			}
			List<String> outLines = processTextFileLines(lines, file, modes);
			for (String modLine : outLines) {
				pw.println(modLine);
			}
		} catch (IOException e) {
			System.err.printf("I/O Error on %s: %s%n", file, e);
		} finally {
			if (modes.fileChanged) {
				log.info(file + " had change(s)"); // XXX run diff?
			}
			if (modes.inCutMode) {
				System.err.println("WARNING: " + file + " file ends in cut mode!");
			}
			if (modes.inCommentMode) {
				System.err.println("WARNING: " + file + " file ends in commenting-out mode!");
			}
		}
	}

	/**
	 * Do the actual work of massaging a text file's contents;
	 * extracted from processTextFile to make testing easier
	 * XXX change interface to this method to be a Stream (Java 8); still testable, less overhead
	 * @param lines The List of lines in the file
	 * @param inputFile The input File, only for printing errors
	 * @param modes The state flags wrapper
	 */
	public List<String> processTextFileLines(List<String> lines, File inputFile,
			TextModes modes) {
		Map<String,String> replaceMap = new HashMap<String,String>();
		List<String> output = new ArrayList<>();
		if (tld == null) {
			throw new RuntimeException("How you gat here?");
		}
		String defName = tld.getName();
		boolean ppEating = false;
		for (String line : lines) {
			String oldLine = line;
			if (line.startsWith("#endif")) {
				ppEating = false;
				continue;
			}
			if (line.startsWith("#if ")) {
				ppEating = !line.endsWith(defName);
				continue;
			}
			if (ppEating) {
				continue;
			}
			if (modes.inCutMode) {
				if (CUTMODE_START.matcher(line).find()) {
					System.err.println("WARNING: " + inputFile + " has nested CUT_START codes");
				}
				if (CUTMODE_END.matcher(line).find()) {
					modes.inCutMode = false;
				}
				modes.fileChanged = true; // we cut this line
				continue;
			}
			if (CUTMODE_START.matcher(line).find()) {
				modes.inCutMode = true;
				continue;
			}
			if (modes.inCommentMode) {
				if (COMMENTMODE_START.matcher(line).find()) {
					System.err.println("WARNING: " + inputFile + " has nested COMMENT_START codes");
				}
				if (COMMENTMODE_END.matcher(line).find()) {
					modes.inCommentMode = false;
					continue;
				}
			}
			if (COMMENTMODE_START.matcher(line).find()) {
				modes.inCommentMode = modes.fileChanged = true;
				continue;
			}
			if (modes.inExchangeMode) {
				if (EXCHANGEMODE_START.matcher(line).find()) {
					System.err.println("WARNING: " + inputFile + " has nested REPLACE_START codes");
					replaceMap.clear();
				}
				if (EXCHANGEMODE_END.matcher(line).find()) {
					modes.inExchangeMode = false;
					replaceMap.clear();
					continue;
				}
			}			
			if (EXCHANGEMODE_START.matcher(line).find()) {
				parseReplaceModeStart(replaceMap, line);
				modes.inExchangeMode = modes.fileChanged = true;
				continue;
			}			
			for (Pattern p : pattMap.keySet()) {
				line = p.matcher(line).replaceAll(pattMap.get(p));
				log.finest(String.format("Patt %s, line->%s", p, line));
			}
			for (String p : replaceMap.keySet()) {
				line = line.replace(p, replaceMap.get(p));
				log.finest(String.format("Patt %s, line->%s", p, line));
			}			
			output.add(modes.inCommentMode ? "//" + line : line);
			if (!line.equals(oldLine)) {
				log.fine(String.format("Match in this line [%s] -->[%s]", oldLine, line));
				modes.fileChanged = true;
			}
		}
		return output;
	}

	/**
	 * Get the replace tokens from the REPLACEMODE_START line
	 * @param line
	 */
	private void parseReplaceModeStart(Map<String,String> replaceMap, String line) {
			// Slightly fragile removal of an XML comment end if there is one!
		line = line.replace("-->", "");
		String[] tokens = line.split(":::");
		if(tokens.length < 3) {
				// Nasty hack to make sure there is a replace token
			tokens = new String[] {tokens[0], tokens[1], ""};
		}
		replaceMap.put(tokens[1], tokens[2]);
	}
}
