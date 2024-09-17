import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main
{
	public static void main(String[] args) {
		BGThresholdSelector bgs = new BGThresholdSelector(0, 0, 0, 0);
		bgs.loadHist(args[0]);
		bgs.printHist(args[1]);
		bgs.dispHist(args[1]);
	}
}
