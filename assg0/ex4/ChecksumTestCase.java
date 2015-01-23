import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ChecksumTestCase {

	private ByteArrayOutputStream bytes;
	private PrintStream console;

	@Before
	public void setUp() {
		bytes = new ByteArrayOutputStream();
		console = System.out;
		System.setOut(new PrintStream(bytes));
	}

	@After
	public void tearDown() {
		System.setOut(console);
	}

	@Test
	public void test() {
		String[] args = {"./data/doge.jpg"};
		try {
			Checksum.main(args);
			assertEquals("4052859698", bytes.toString());
		} catch (IOException e) {
			fail();
		}
	}

}
