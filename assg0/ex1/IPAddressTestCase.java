

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IPAddressTestCase {

	@Test
	public void testBinaryToDecimal() {
		String binary_addr = "00000011100000001111111111111110";
		String decimal_addr = IPAddress.binary_to_decimal(binary_addr);
		assertEquals("3.128.255.254", decimal_addr);
		
		String binary_addr2 = "11001011100001001110010110000000";
		String decimal_addr2 = IPAddress.binary_to_decimal(binary_addr2);
		assertEquals("203.132.229.128", decimal_addr2);
	}

}
