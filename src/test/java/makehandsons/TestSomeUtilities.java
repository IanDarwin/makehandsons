package makehandsons;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TestSomeUtilities {

	@Test
	public void testIsTextFileName() {
		assertTrue(MakeHandsOns.isTextFile("foo/bar.java"));
		assertTrue(MakeHandsOns.isTextFile("foo/abc.txt"));
		assertTrue(MakeHandsOns.isTextFile("foo/Readme.adoc"));
		assertTrue(MakeHandsOns.isTextFile("foo/reboot.cpp"));
		assertFalse(MakeHandsOns.isTextFile("Llewelynn.class"));
	}
}
