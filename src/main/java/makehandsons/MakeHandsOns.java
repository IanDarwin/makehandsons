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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** The MakeHandsOns program looks through the given
 * directories recursively, copying each file
 * to the output directory. If the file is a text file (ends in
 * .java or .xml o ...), do substitution on each line with a given
 * set of replacement patterns (patterns and their
 * replacements are loaded from a Properties file).
 * @author Ian Darwin
 */
public class MakeHandsOns {
	
	private static Logger log = Logger.getLogger("mytools.makehos");
	
	/** The file extens that get replacements done */
	final static String[] SUB_TEXT_FILE_EXTENS = {
		".java",
		".jsp",
		".html",
		".project",
		".properties",
		".xhtml",
		".xml",
	};
	
	/** The part of the directory name that gets removed. */
	final static String REMOVE_FROM_PATH = "solution";
	
	private final static Pattern CUTMODE_START = Pattern.compile("^\\s+//-");
	private final static Pattern CUTMODE_END = Pattern.compile("^\\s+//\\+");
	
	//-
	/* This should not appear in the output */
	//+
	//R // This should appear, and is a test for the "cut mode" process in processText()

	/** directories to ignore */
	final static String[] IGNORE_DIRS = { "CVS", ".metadata" };

	public static void main(String[] args) {
		MakeHandsOns f = new MakeHandsOns();
		f.loadPatterns();
		for (String arg : args) {
			f.searchFiles(new File(arg));
		}
	}
	
	/** Map from a compiled regex Pattern to its replacement String */
	Map<Pattern,String> pattMap = new HashMap<Pattern,String>();

	/** Load (and compile) the Pattern file, a list
	 * of x=y, where x is a regex pattern and y
	 * is a replacement value.
	 */
	void loadPatterns() {
		Properties p = new Properties();
		InputStream is = getClass().getResourceAsStream("/makehandsons.properties");
		try {
			p.load(is);
		} catch (IOException e) {
			throw new RuntimeException("Error loading patterns", e);
		}
		
		for (Object k : p.keySet()) {
			String key = (String)k;
			Pattern pat = Pattern.compile(key);
			String repl = p.getProperty(key);
			pattMap.put(pat, repl);
		}
	}

	/** Work through the starting directory, mapping it to destDir */
	void searchFiles(File startDir) {
		log.fine(String.format("FileSub.searchFiles(%s)%n", startDir));
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
				searchFiles(f);
			}
		} else if (startDir.isFile()) {
			processFile(startDir);
		} else {
			System.err.printf("Warning: %s neither file nor directory, ignoring", startDir);
		}
	}

	private boolean isTextFile(String fileName) {
		for (String exten : SUB_TEXT_FILE_EXTENS) {
			if (fileName.endsWith(exten))
				return true;
		}
		return false;
	}
	
	private void processFile(File file) {
		String absPath = file.getAbsolutePath();
		String newAbsPath = absPath.replace(REMOVE_FROM_PATH, "");
		log.fine("NEW ABS PATH = " + newAbsPath);
		File newFile = new File(newAbsPath);
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
	
	/** Copied from c.d.io.FileIO */
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
	private void processTextFile(File file) {
		BufferedReader is = null;
		PrintWriter pw = null;
		boolean inCutMode = false;
		boolean fileChanged = false;
		try {
			pw = new PrintWriter(file.getAbsolutePath().replace(REMOVE_FROM_PATH, ""));
			is = new BufferedReader(new FileReader(file));
			String line;
			while ((line = is.readLine()) != null) {
				String oldLine = line;
				if (inCutMode) {
					if (CUTMODE_START.matcher(line).matches()) {
						System.err.println("WARNING: " + file + " has nested CUT_START codes");
					}
					if (CUTMODE_END.matcher(line).matches()) {
						inCutMode = false;
					}
					continue;
				}
				if (CUTMODE_START.matcher(line).matches()) {
					inCutMode = true;
					continue;
				}
				for (Pattern p : pattMap.keySet()) {
					line = p.matcher(line).replaceAll(pattMap.get(p));
				}
				pw.println(line);
				if (line != oldLine) {
					fileChanged = true;
				}
			}
		} catch (IOException e) {
			System.err.printf("I/O Error on %s: %s%n", file, e);
		} finally {
			if (fileChanged) {
				System.out.println(file + " had change(s)"); // XXX run diff
			}
			if (inCutMode) {
				System.err.println("WARNING: " + file + " file ends in cut mode!");
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException annoyingLittleCheckedException) {
					annoyingLittleCheckedException.printStackTrace();
				}
			}
			if (pw != null) {
				pw.close();
			}
		}
	}
}