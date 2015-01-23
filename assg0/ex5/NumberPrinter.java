import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class NumberPrinter extends TimerTask {
	
	private double n;
	
	public NumberPrinter(double toPrint){
		n = toPrint;
	}

	@Override
	public void run() {
		System.out.println(n);
	}
	
	public static void main(String[] args) {
		Timer timer = new Timer();
		timer.schedule(
				new NumberPrinter(Double.parseDouble(args[0])), 
				Integer.parseInt(args[1]) * 1000, 
				Integer.parseInt(args[2]) * 1000);
		
		Scanner scan = new Scanner(System.in);
		while (scan.hasNextLine()){
			if (scan.nextLine().equals("q")){
				timer.cancel();
				scan.close();
				System.exit(0);
			}
		}
	}

}
