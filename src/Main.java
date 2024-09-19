import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Main
{
	public static void main(String[] args) {
		validateArguments(args);
		BGThresholdSelector bgs = new BGThresholdSelector(0, 0, 0, 0);
		try (BufferedWriter histFile = new BufferedWriter(new FileWriter(args[1]));
			 BufferedWriter gaussFile = new BufferedWriter(new FileWriter(args[2]));
			 BufferedWriter logFile = new BufferedWriter(new FileWriter(args[3]))
		) {

			bgs.loadHist(args[0]);

			histFile.write("** Below is the input histogram **:\n");
			bgs.printHist(args[1]);

			histFile.write("\n** Below is the graphic display of the histogram **:\n");
			bgs.dispHist(args[1]);

			double biGaussThrVal = bgs.biGaussian(args[3]);
			gaussFile.write("** The selected threshold value is " + biGaussThrVal);

			gaussFile.write("\n** Below are the best-fitted Gaussians **:\n");
			bgs.printBestFitGauss(args[2]);

			gaussFile.write("\n** Below is the graphic display of best-fitted Gaussians **:\n");

			bgs.dispGaussGraph(args[2]);
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