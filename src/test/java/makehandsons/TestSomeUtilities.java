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
	private MakeHandsOns target;

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
}
