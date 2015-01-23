import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CalculatorTestCase {
	private final String expression_error_msg = "Error in expression";
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
	public void testSubstraction() {
		String[] args = { "30", "-", "7" };
		Calculator.main(args);
		assertEquals("30 - 7 = 23", bytes.toString());
	}

	@Test
	public void testDivision() {
		String[] args = { "30", "/", "7" };
		Calculator.main(args);
		assertEquals("30 / 7 = 4", bytes.toString());
	}

	@Test
	public void testInsufficientOperand() {
		String[] args = { "-10", "*" };
		Calculator.main(args);
		assertEquals(expression_error_msg, bytes.toString());
	}

	@Test
	public void testDivisionByZero() {
		String[] args = { "333", "/", "0" };
		Calculator.main(args);
		assertEquals(expression_error_msg, bytes.toString());
	}

	@Test
	public void testInvalidOperandType() {
		String[] args = { "30.5", "/", "7" };
		Calculator.main(args);
		assertEquals(expression_error_msg, bytes.toString());
	}
}
