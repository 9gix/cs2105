import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class TCPEchoClient {
	
	private String ip = "127.0.0.1";
	private int port = 5678;
	
	public TCPEchoClient(){
		try {
			Socket socket = new Socket(ip, port);
			Scanner scan = new Scanner(System.in);
			String message = scan.nextLine();
			PrintWriter outbox;
			outbox = new PrintWriter(socket.getOutputStream(), true);
			outbox.println(message);
			Scanner sc = new Scanner(socket.getInputStream());
			String msg = sc.nextLine();
			System.out.println("Echo from server " + msg);
			socket.close();
			scan.close();
			sc.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new TCPEchoClient();
	}

}
