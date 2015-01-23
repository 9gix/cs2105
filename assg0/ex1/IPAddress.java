
import java.util.Scanner;

public class IPAddress {

	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		String binary_addr = scan.nextLine();
		String decimal_addr = IPAddress.binary_to_decimal(binary_addr);
		System.out.println(decimal_addr);
		scan.close();
	}

	public static String binary_to_decimal(String binary_addr) {
		StringBuilder sb = new StringBuilder(15);
		
		int index = 0;
		while (index < binary_addr.length()){
			String token = binary_addr.substring(index, Math.min(index + 8, binary_addr.length()));
			sb.append(Integer.parseInt(token, 2));
			index += 8;
			if (index < binary_addr.length()) 
				sb.append(".");
		}
		
		return sb.toString();
	}

}
