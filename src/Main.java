import java.io.IOException;

public class Main
{
	public static void main(String[] args) {
		validateArguments(args);
		BGThresholdSelector bgs = new BGThresholdSelector(0, 0, 0, 0);
		try {
			bgs.loadHist(args[0]);
			bgs.printHist(args[1]);
			bgs.dispHist(args[1]);
		} catch (IOException ioException) {
			System.err.println("Error with input files, please check paths/destinations");
			ioException.printStackTrace();
		}
	}

	private static void validateArguments(String[] args) {
		if (args.length != 4) {
			System.err.println("You must have exactly 4 arguments: inFile histFile gaussFile logFile");
			System.exit(1);
		}
	}
}