package makehandsons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TestSomeUtilities {
	MakeHandsOns target;

	@Before
	public void mehtUp() {
		target = new MakeHandsOns();
	}

	@Test
	public void testIsTextFileName() {
		assertTrue(target.isTextFile("foo/bar.java"));
		assertTrue(target.isTextFile("foo/abc.txt"));
		assertTrue(target.isTextFile("foo/Readme.adoc"));
		assertFalse(target.isTextFile("Llewelynn.class"));
	}
	
	final static File inputFile = new File("test file");
	
	@Test
	public void testProcessTextFileLinesIdemPotent() {
		List<String> input = Arrays.asList("One","Two");
		TextModes modes = new TextModes();
		List<String> output = target.processTextFileLines(input, inputFile, modes);
		for (int i = 0; i < output.size(); i++) {
			assertEquals(input.get(i), output.get(i));
		}
	}
}
