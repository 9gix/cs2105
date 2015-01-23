
public class Calculator {

	public static void main(String[] args) {
		try {
			int operand1 = Integer.parseInt(args[0]);			
			int operand2 = Integer.parseInt(args[2]);
			String operator = args[1];
			int result = 0;
			switch (operator) {
				case "+": result = operand1 + operand2; break;
				case "-": result = operand1 - operand2; break;
				case "*": result = operand1 * operand2; break;
				case "/": result = operand1 / operand2; break;
				default: throw new ArithmeticException();
			}
			System.out.print(operand1 + " " + operator + " " + operand2 + " = " + result);			
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException | ArithmeticException e){
			System.out.print("Error in expression");
		}
	}

}
