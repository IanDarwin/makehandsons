package makehandsons;

import org.junit.*;
import static org.junit.Assert.*;

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
}
