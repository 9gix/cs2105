import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class CopierTestCase {

	private final String in_file = "mock_file";
	private final String out_file = "mock_file2";
	private File file;
	private File copied_file;
	@Before
	public void setUp() throws IOException {
		file = new File(in_file);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file));
			out.write("Mock Data 123\n\n");
		} finally {
			out.close();
		}
		copied_file = Paths.get(out_file).toFile();
	}

	@After
	public void tearDown() {
		file.delete();
		copied_file.delete();
	}

	@Test
	public void test() {
		String[] args = { in_file, out_file};
		try {
			Copier.main(args);
			assertEquals(file.getTotalSpace(), copied_file.getTotalSpace());
		} catch (IOException e) {
			fail();
		}
	}

}
