import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


public class TCPEchoServer {
	private int port = 5678;
	
	public TCPEchoServer(){
		ServerSocket server;
		try {
			server = new ServerSocket(port);

			while (true){
				Socket socket = server.accept();
				System.out.println("Connected to a client...");
				Scanner scan = new Scanner(socket.getInputStream());
				String inbox = scan.nextLine();
				PrintWriter outbox = new PrintWriter(socket.getOutputStream(), true);
				outbox.println(inbox);
				scan.close();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	public static void main(String[] args) {
		new TCPEchoServer();
	}

}
