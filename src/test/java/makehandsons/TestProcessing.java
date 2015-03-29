package makehandsons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TestProcessing {
	TextModes modes;
	MakeHandsOns target;
	
	final static File inputFile = new File("test file");

	@Before
	public void mehtUp() {
		target = new MakeHandsOns();
		modes = new TextModes();
	}

	@Test
	public void testProcessTextFileLinesIdemPotent() {
		List<String> input = Arrays.asList("One","Two");
		
		List<String> output = target.processTextFileLines(input, inputFile, modes);
		for (int i = 0; i < output.size(); i++) {
			assertEquals(input.get(i), output.get(i));
		}
	}
	
	@Test
	public void testCutMode() {
		List<String> input = Arrays.asList(
			"//-",
			"/* This should not appear in the output */",
			"//+",
			"int i = 0;" // This is the online line that should appear
			);
		List<String> output = target.processTextFileLines(input, inputFile, modes);
		assertEquals(1, output.size());
		assertFalse(output.toString().contains("should not appear"));
	}
}
