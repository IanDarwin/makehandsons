package filesub;

import java.io.*;
import java.util.Properties;

/** The FileSub program looks through
 * directories recursively looking for
 * *.xml or *.java files and, for each one,
 * if it contains one of a set of given
 * patterns, replace it with a given
 * replacement pattern (patterns and
 * replacements are loaded from a Properties
 * file).
 * @author Ian Darwin
 */
public class FileSub {

	public static void main(String[] args) {
		FileSub f = new FileSub();
		f.loadPatterns();
		f.searchFiles(new File("."));
	}

	private void loadPatterns() {
		Properties p = new Properties();
		InputStream is = getClass().getResourceAsStream("filesub.properties");
		try {
			p.load(is);
		} catch (IOException e) {
			throw new RuntimeException("Error loading patterns", e);
		}
	}

	private void searchFiles(File dir) {
		if (dir.isDirectory()) {
		File[] list = dir.listFiles(acceptFilter );
		for (File f : list) {
			searchFiles(f);
		}
		} else if (dir.isFile()) {
			processFile(dir);
		} else {
			System.err.printf("Warning: %s neither file nor directory, ignoring", dir);
		}
	}

	private void processFile(File file) {
		BufferedReader is;
		try {
			is = new BufferedReader(
				new FileReader(file));
			String line;
			while ((line = is.readLine()) != null) {
				if (lineMatchesAny(line)) {
					rewriteFileWithChanges(file);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void rewriteFileWithChanges(File f) {
		// TODO Auto-generated method stub
		
	}

	private boolean lineMatchesAny(String line) {
		// TODO Auto-generated method stub
		return false;
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