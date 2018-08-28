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
		assertNotChanged(modes);
		assertFinished(modes);
	}

	@Test
	public void testCutModeJava() {
		List<String> input = Arrays.asList(
			"//-",
			"/* This should not appear in the output */",
			"//+",
			"int i = 0;" // This is the online line that should appear
			);
		List<String> output = target.processTextFileLines(input, inputFile, modes);
		assertEquals(1, output.size());
		assertChanged(modes);
		assertFinished(modes);
		assertFalse(output.toString().contains("should not appear"));
	}

	@Test
	public void testCutModeXhtml() {
		List<String> input = Arrays.asList(
			"<!-- //- -->",
			"This should not appear in the output",
			"<!-- //+ -->",
			"int i = 0;" // This is the online line that should appear
			);
		List<String> output = target.processTextFileLines(input, inputFile, modes);
		assertEquals(1, output.size());
		assertChanged(modes);
		assertFinished(modes);
		assertFalse(output.toString().contains("should not appear"));
		assertEquals(input.get(3), output.get(0));
	}

	@Test
	public void testCommentMode() {
		List<String> input = Arrays.asList(
			"//C+",
			"int i = 0;",
			"//C-"
			);
		List<String> output = target.processTextFileLines(input, inputFile, modes);
		assertEquals(1, output.size());
		assertEquals("//"+input.get(1), output.get(0));
		assertChanged(modes);
		assertFinished(modes);
		assertFalse(output.toString().contains("should not appear"));
	}

	private void assertFinished(TextModes modes) {
		assertFalse(modes.inCutMode);
		assertFalse(modes.inCommentMode);
	}

	private void assertChanged(TextModes modes) {
		assertTrue(modes.fileChanged);
	}
	private void assertNotChanged(TextModes modes) {
		assertFalse(modes.fileChanged);
	}

	@Test
	public void testReplacementMode() {
		List<String> input = Arrays.asList(
			"//R This should appear",
			"//-",
			"int i = 0; // this should not appear",
			"//+"
			);

		List<String> output = target.processTextFileLines(input, inputFile, modes);
		assertEquals(1, output.size());
		assertTrue(output.get(0).contains("This should appear"));
	}
	
	@Test
	public void testExchangetMode() {
		List<String> input = Arrays.asList(
			"//X+ :::Replace This\\.:::With This",
			"int i = 0; // Replace This.",
			"//X-",
			"int i = 1; // Replace This"
			
			);

		List<String> output = target.processTextFileLines(input, inputFile, modes);
		assertEquals(2, output.size());
		assertTrue(output.get(0).contains("int i = 0;"));
		assertTrue(output.get(0).contains("With This"));
		assertTrue(output.get(1).contains("int i = 1;"));
		assertTrue(output.get(1).contains("Replace This"));		
	}
	
	@Test
	public void testExchangeModeWithXML() {
		List<String> input = Arrays.asList("<?xml version=\"1.0\" encoding=\"utf-8\"?>",
		"<!-- //X+ :::android:id=\"@\\+id/expLst_layout\":::   -->",		
		"<android.support.constraint.ConstraintLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"",
		    "xmlns:app=\"http://schemas.android.com/apk/res-auto\"",
		    "xmlns:tools=\"http://schemas.android.com/tools\"",
		    "              ",
		    "android:id=\"@+id/expLst_layout\"\\>");		


		List<String> output = target.processTextFileLines(input, inputFile, modes);
		assertEquals(6, output.size());
		System.out.println(output.get(3)+ "===>" + "xmlns:tools=\"http://schemas.android.com/tools\"");
		assertTrue(output.get(3).contains("xmlns:tools=\"http://schemas.android.com/tools\""));
		assertTrue(output.get(5).contains("\\>"));
	}	
}
