package makehandsons;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The MakeHandsOns program looks through the given
 * directories recursively, copying each file
 * to the output directory. If the file is a text file (ends in
 * .java or .xml or ...), do substitution on each line with a given
 * set of replacement patterns (patterns and their
 * replacements are loaded from a Properties file).
 * XXX This file may be too big!
 * @author Ian Darwin
 */
public class MakeHandsOns {
	
	private static final String EXCLUDE_FILES_FILENAME = "exclude-files.txt";
	private static final String VERBATIM_FILES_FILENAME = "verbatim-files.txt";
	private static final String PROPERTIES_FILENAME = "/makehandsons.properties";

	Properties sysProps = System.getProperties();
	
	/** The file extens that DO get replacements done */
	final static String[] SUB_TEXT_FILE_EXTENS = {
		".adoc",
		".c",
		".c#",
		".cpp",
		".gradle",
		".html",
		".java",
		".jsf",
		".jsp",
		".json",
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

	// Main patterns, but SEE ALSO src/main/resources/makehandsons.properties
	
	private final static Pattern CUTMODE_START = Pattern.compile("^\\s*//-\\s*$");
	private final static Pattern CUTMODE_END = Pattern.compile("^\\s*//\\+\\s*$");

	private final static Pattern COMMENTMODE_START = Pattern.compile("^\\s*//C\\+\\s*");
	private final static Pattern COMMENTMODE_END = Pattern.compile("^\\s*//C\\-\\s*");

	private final static Pattern REPLACE_TEXT = Pattern.compile("\\\\s*//R\\s*(.*)");
	
	private final static Pattern EXCHANGEMODE_START = Pattern.compile("\\s*//X\\+\\s*");
	private final static Pattern EXCHANGEMODE_END = Pattern.compile("\\s*//X\\-\\s*");	

	private final static Pattern IFDEF_START = Pattern.compile("^.if\\s+([a-z]+)");

	/** directories to ignore */
	final static String[] IGNORE_DIRS = { 
		"CVS", ".svn", ".git", ".metadata", "bin", "target", "build"
	};

	/** Map from a compiled regex Pattern to its replacement String */
	static Map<Pattern,String> pattMap;
	
	/** JUL logger */
	private static Logger log;

	/** The current solution folder and project folder */
	private static String currentSolution = "ex??solution", currentProject = "ex??";

	static {

		InputStream logStream = 
			MakeHandsOns.class.getClassLoader().getResourceAsStream("logging.properties");

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
		System.err.println("MakeHandsOns Processing started");
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
					currentSolution = fileArg.getName();
					currentProject = currentSolution.replace(REMOVE_FROM_PATH, "");
					prog.makeIgnoreList(fileArg);
					prog.makeVerbatimList(fileArg);
					prog.descendFileSystem(fileArg);
				}
		} catch (Exception e) {
			System.out.println("Catastrophe: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	static final String HELP_TEXT = """
		This program generates the exercises from the solutions
		//T -> // TODO
		//H -> // *HINT*
		//- -> enter cut mode, delete lines up to //+
		//R -> replacement text for what got cut (precedes //-)
		//C+ -> enter comment-out mode, up to //C-
		//X+ :::textToReplace:::replacementText-> enter exchange mode
		//X- -> leave exchange (text replacement) mode		
		#if project_name -> Like cpp #ifdef, end with #endif
		""";
	
	private static void doHelp() {
		System.out.println(HELP_TEXT);
	}

	MakeHandsOns() {
		log.info("MakeHandsOns.MakeHandsOns()");

		// Don't move this into static initializer as the filename will someday become parameterized.
		try (InputStream is = getClass().getResourceAsStream(PROPERTIES_FILENAME)) {
			if (is == null) {
				throw new RuntimeException("Could not load " + PROPERTIES_FILENAME + " from classpath.");
			}
			pattMap = loadPatterns(is);
		} catch (IOException ex) {
			throw new ExceptionInInitializerError("CANTHAPPEN, but did");
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
	
	List<String> verbatimFiles = new ArrayList<>();
	
	void makeVerbatimList(File dir) throws IOException {
		verbatimFiles.clear();
		final File verbatimeFilesFile = 
			new File(dir, VERBATIM_FILES_FILENAME);
		if (!verbatimeFilesFile.exists()) {
			return;
		}
		try (BufferedReader is = 
			new BufferedReader(new FileReader(verbatimeFilesFile))) {
			String line;
			while ((line = is.readLine()) != null) {
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				verbatimFiles.add(line);
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
	
	/** Load (and compile) the Pattern file, a Properties list
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
		p.keySet().forEach(k -> {
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
		});
		return pattMap;
	}

	/** 
	 * Work down through the starting directory and subdirs,
	 * mapping each to destDir. 
	 * Called recursively.
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

	static boolean isTextFile(String fileName) {
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
			EXCLUDE_FILES_FILENAME.equals(name) ||
			VERBATIM_FILES_FILENAME.equals(name)) {
			return;
		}
		// Verbatim files get copied unmolested
		if (verbatimFiles.contains(name)) {
			copyFile(file, newFile);
			return;
		}
		if (excludeFiles.contains(name)) {
			checkIgnoredFileForMarkup(file);
			if (newFile.exists()) {
				log.severe("Excluded file exists: " + newAbsPath + "; nuking it!");
				newFile.delete();
				if (newFile./*still*/exists()) {
					// Fail early and (not too) often.
					throw new IllegalArgumentException("Sob! Tried to delete " + newAbsPath + " but failed!");
				}
			}
			return;
		}
		log.fine(String.format("MakeHandsOn.processFile(%s->%s)%n", file, newAbsPath));		
		if (isTextFile(file.getName())) {
			processTextFile(file);
		} else {						// copy as binary
			copyFile(file, newFile);
		}		
	}
	
	private static int BLKSIZ = 8192;
	
	/** Copy a file in BINARY mode. 
	 * NO markup handling!
	 * @author Adapted from c.d.io.FileIO
	 */
	public void copyFile(File file, File target) throws IOException {
		if (!file.exists() || !file.isFile() || !(file.canRead())) {
			throw new IOException(file + " is not a readable file");
		}
		target.getParentFile().mkdirs();
		File dest = target;
		if (target.isDirectory()) {
			dest = new File(dest, file.getName());
		}
		Files.copy(file.toPath(), dest.toPath());
	}
	
	/** Copy one TEXT file, with substitutions. */
	private void processTextFile(File file) {
		TextModes modes = new TextModes();
		String path = file.getAbsolutePath().replace(REMOVE_FROM_PATH, "");
		new File(path).getParentFile().mkdirs();
		try (PrintWriter pw = new PrintWriter(path);) {
			List<String> lines = Files.readAllLines(file.toPath());
			List<String> outLines = processTextFileLines(lines, file, modes);
			for (String line : outLines) {
				pw.println(line);
			}
			pw.flush();
		} catch (IOException e) {
			System.err.printf("I/O Error on %s: %s%n", file, e);
		} finally {
			if (modes.fileChanged) {
				log.info(file + " had change(s)"); // XXX run diff, for hints?
			}
			if (modes.inCutMode) {
				System.err.println("WARNING: " + file + " file ends in cut mode!");
			}
			if (modes.inCommentMode) {
				System.err.println("WARNING: " + file + " file ends in commenting-out mode!");
			}
			if (modes.inCppMode) {
				System.err.println("WARNING: " + file + " file ends in #if mode!");
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
	 * @return The list of modified lines.
	 */
	public List<String> processTextFileLines(List<String> lines, File inputFile,
			TextModes modes) {
		Map<String,String> replaceMap = new HashMap<String,String>();
		List<String> output = new ArrayList<>();
		if (tld == null) {
			throw new IllegalStateException(
				"processTextFileLines(): no TLD set!");
		}
		String projectName = tld.getName();
		modes.inCppMode = false;
		for (String line : lines) {
			String oldLine = line;

			// Must do #if/#endif FIRST so it can control others
			if (line.startsWith("#endif")) {
				// Test for out of place endif here?
				modes.inCppMode = false;
				continue;
			}
			Matcher m = IFDEF_START.matcher(line);
			if (m.lookingAt()) {
				String ifdefName = m.group(1);
				modes.inCppMode = !ifdefName.equals(projectName);
				continue;
			}
			if (modes.inCppMode) {
				modes.fileChanged = true; // we cut this line
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
			// Start of replacements - do these first
			line = line.replaceAll("\\$\\{project.name\\}", currentProject);
			line = line.replaceAll("\\$\\{solution.name\\}", currentSolution);
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
				log.fine(String.format("Change in this line [%s] -->[%s]", oldLine, line));
				modes.fileChanged = true;
			}
		}
		return output;
	}

	/**
	 * Get the replace tokens from the REPLACEMODE_START line
	 * @param line The input string to be modified
	 * @author Mike Way
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
