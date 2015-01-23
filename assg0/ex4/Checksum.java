import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.CRC32;


public class Checksum {

	public static void main(String[] args) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
		CRC32 crc = new CRC32();
		crc.update(bytes);
		System.out.print(crc.getValue());
	}
	
}
