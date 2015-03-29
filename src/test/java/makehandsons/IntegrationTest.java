package makehandsons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class IntegrationTest {
	
	final static String INPUT_DIR = "inttestsolution";
	final static String ACTUAL_DIR = "inttest";
	final static String EXPECTED_DIR = "inttestexpected";
	final static File ACTUAL_DIR_DIR = new File(ACTUAL_DIR);
	final static Runtime r = Runtime.getRuntime();

	/** Make sure it creates the directory if needed */
	@Test
	public void test() throws Exception {
		
		// Remove the output directory
		if (ACTUAL_DIR_DIR.exists()) {
			r.exec("rm -r " + ACTUAL_DIR).waitFor();
		}
		
		// Run the test subject!
		MakeHandsOns.main(new String[]{INPUT_DIR});
		
		// Make sure it re-created the directory
		assertTrue(ACTUAL_DIR_DIR.isDirectory());
		
	}
	
	@Test
	public void testDetails() throws Exception {
		
		// Run the test subject!
		MakeHandsOns.main(new String[]{INPUT_DIR});
		
		// Use Unix diff command to test (needs Cygwin on DOS)
        Process p = r.exec("diff -r " + EXPECTED_DIR + " " + ACTUAL_DIR);

        try {
            p.waitFor();    // wait for process to complete
        } catch (InterruptedException e) {
            System.err.println(e);  // "Can'tHappen"
            return;
        }
        assertEquals(0, p.exitValue());
	}
}
