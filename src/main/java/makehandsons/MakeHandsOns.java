package filesub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import com.darwinsys.io.FileIO;

/** The FileSub program looks through
 * directories recursively, copying each file
 * to the output directory. If the file
 * is .java or .xml, substitution on each line with a given
 * set of replacement patterns (patterns and their
 * replacements are loaded from a Properties file).
 * @author Ian Darwin
 */
public class FileSub {
	
	/** The file extens that get replacements done */
	final static String[] SUB_TEXT_FILE_EXTENS = {
		".java",
		".xml",
		".jsp",
		".html",
		".xhtml"
	};
	
	/** The part of the directory name that gets removed. */
	final static String REMOVE_FROM_PATH = "solution";
	
	/** directories to ignore */
	final static String[] IGNORE_DIRS = { "CVS", ".metadata" };

	public static void main(String[] args) {
		FileSub f = new FileSub();
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
		InputStream is = getClass().getResourceAsStream("/filesub.properties");
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
		System.out.printf("FileSub.searchFiles(%s)%n", startDir);
		if (startDir.isDirectory()) {
			String name = startDir.getName();
			for (String dir : IGNORE_DIRS) {
				if (dir.equals(name)) {
					System.out.println("IGNORING " + startDir);
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
		System.out.println("NEW ABS PATH = " + newAbsPath);
		File newFile = new File(newAbsPath);
		newFile.getParentFile().mkdirs();
		System.out.printf("FileSub.processFile(%s->%s)%n", file, newAbsPath);		
		if (isTextFile(file.getName())) {
			processTextFile(file);
		} else {						// copy as binary
			try {
				FileIO.copyFile(file, newFile);
			} catch (IOException e) {
				System.err.println("Failed to copy " + file + "; " + e);
			}
		}		
	}
	
	//-
	/* This should not appear in the output */
	//+
	//R this is a test for the "cut mode" process in processText()
	
	private void processTextFile(File file) {
		BufferedReader is = null;
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file.getAbsolutePath().replace(REMOVE_FROM_PATH, ""));
			is = new BufferedReader(new FileReader(file));
			String line;
			boolean inCutMode = false;
			while ((line = is.readLine()) != null) {
				if (inCutMode) {
					if (line.indexOf("//+") != -1) {
						inCutMode = false;
					}
					continue;
				}
				if (line.indexOf("//-") != -1) {
					inCutMode = true;
					continue;
				}
				for (Pattern p : pattMap.keySet()) {
					line = p.matcher(line).replaceAll(pattMap.get(p));
				}
				pw.println(line);
			}
		} catch (IOException e) {
			System.err.printf("I/O Error on %s: %s%n", file, e);
		} finally {
			if (pw != null)
				pw.close();
			if (is != null)
				try {
					is.close();
				} catch (IOException annoyingLittleCheckedException) {
					annoyingLittleCheckedException.printStackTrace();
				}
		}
	}
	
	FilenameFilter acceptFilter = new FilenameFilter() {

		public boolean accept(File dir, String name) {
			String[] okSuffixes = { ".xml", ".java" };
			for (String suffix : okSuffixes) {
				if (name.endsWith(suffix)) {
					return true;
				}
			}
			return false;
		}

	};
}