import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class FileTransferTestCase {

	private final String in_file = "mock_file";
	private final String out_file = "mock_file2";
	private File file;
	private File transfered_file;
	
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random rnd = new Random();

	String randomString( int len ) 
	{
	   StringBuilder sb = new StringBuilder( len );
	   for( int i = 0; i < len; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
	   return sb.toString();
	}
	
	@Before
	public void setUp() throws IOException {
		file = new File(in_file);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file));
			out.write(randomString(1025));
		} finally {
			out.close();
		}
		transfered_file = Paths.get(out_file).toFile();
	}

	@After
	public void tearDown() {
		file.delete();
		transfered_file.delete();
	}

	@Test
	public void test() {
		String[] receiver_args = { "9000" };
		
	    Thread server_thread = new Thread(){
	        public void run(){
	            try {
                    FileReceiver.main(receiver_args);
                } catch (IOException e) {
                    fail(e.getMessage());
                }		            
	        }
	    };
	    server_thread.setPriority(Thread.MAX_PRIORITY);
	    server_thread.start();
		
		String[] sender_args = { in_file, "localhost" , "9000", out_file };
		Thread client_thread = new Thread(){
		    public void run(){
		        try {
		            FileSender.main(sender_args);
		        } catch (IOException | InterruptedException e) {
		            // TODO Auto-generated catch block
		            fail(e.getMessage());
		        }              
		    }
		};
		client_thread.setPriority(Thread.MIN_PRIORITY);
		client_thread.start();
		
		try {
            client_thread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            fail(e.getMessage());
        } finally {            
            server_thread.interrupt();
        }
		
		byte[] data = null;
        byte[] transferred_data = null; 
		try {
            data = Files.readAllBytes(file.toPath());
            transferred_data = Files.readAllBytes(transfered_file.toPath());
        } catch (IOException e) {
            fail(e.getMessage());
        }
		
		assertNotNull(transferred_data);
		assertArrayEquals(data, transferred_data);
	}

}
