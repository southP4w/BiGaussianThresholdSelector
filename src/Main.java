import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;

public class Main
{
	public static void main(String[] args) {
		validateArguments(args);
		BGThresholdSelector bgs = new BGThresholdSelector(0, 0, 0, 0);
		try (BufferedWriter histFile = new BufferedWriter(new FileWriter(args[1]));
			 BufferedWriter gaussFile = new BufferedWriter(new FileWriter(args[2]));
			 BufferedWriter logFile = new BufferedWriter(new FileWriter(args[3]));
		) {
			bgs.loadHist(args[0]);

			histFile.write("\n** Below is the input histogram **:\n");
			bgs.printHist(histFile);

			histFile.write("\n** Below is the graphic display of the histogram **:\n");
			bgs.dispHist(gaussFile);

			double biGaussThrVal = bgs.biGaussian(logFile);
			gaussFile.write("\n** The selected threshold value is " + biGaussThrVal);

			gaussFile.write("\n** Below are the best-fitted Gaussians **:\n");
			bgs.printBestFitGauss(gaussFile);

			gaussFile.write("\n** Below is the graphic display of best-fitted Gaussians **:\n");
			bgs.plotGaussGraph(bgs.getBestFitGaussAry(), bgs.getGaussGraph(), logFile);
			bgs.dispGaussGraph(gaussFile);

			gaussFile.write("\n** Below displays the gaps between the histogram and best-fitted Gaussians **:\n");
			bgs.plotGapGraph(bgs.getHistAry(), bgs.getBestFitGaussAry(), logFile);
			bgs.dispGapGraph(gaussFile);
		} catch (IOException ioException) {
			System.err.println("\nError with input files, please check paths/destinations\n");
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