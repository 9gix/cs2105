import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class Copier {

	public static void main(String[] args) throws IOException {
		String src = args[0];
		String dest = args[1];
		FileInputStream file_in = null;
		FileOutputStream file_out = null;
		try {
			file_in = new FileInputStream(src);
			file_out = new FileOutputStream(dest);
			int data;
			while((data = file_in.read()) != -1){
				file_out.write(data);	
			}
		} finally {		
			file_in.close();
			file_out.close();
		}
		
	}

}
