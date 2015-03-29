package makehandsons;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class IntegrationTest {

	@Test
	public void test() throws Exception {
		MakeHandsOns.main(new String[]{"inttestsolution/"});
		assertTrue(new File("inttest").isDirectory());
	}

}
