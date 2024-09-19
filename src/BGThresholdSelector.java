import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BGThresholdSelector
{
	private int numRows, numCols, minVal, maxVal;    // image header
	private int biGaussThrVal;    // the auto-selected threshold value by the BG method
	private int histHeight;    // largest element hist[i] in the input histogram
	private int maxHeight;    // largest element hist[i] within a given range of the histogram. Init to 0
	private int[] histAry;   // 1D int[] (size maxVal+1) to store the hist (dynamically allocated at runtime, init to 0)
	private int[] gaussAry;    // 1D int[] (size maxVal+1) to store modified Gauss curve values. (DynAl, init 0)
	private int[] bestFitGaussAry;    // 1D int array to store the best Bi-Means Gaussian curves (init to 0)
	private char[][] gaussGraph;    // 2D character array (size maxVal+1 × histogramHeight+1, all init to blank)
	private char[][] gapGraph;    // 2D character array (size maxVal+1 × histogramHeight+1, all init to blank)

	public BGThresholdSelector(int numRows, int numCols, int minVal, int maxVal) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.minVal = minVal;
		this.maxVal = maxVal;

		maxHeight = 0;
		histAry = new int[maxVal + 1];
		gaussAry = new int[maxVal + 1];
		bestFitGaussAry = new int[maxVal + 1];
		gaussGraph = new char[maxVal + 1][histHeight + 1];
		gapGraph = new char[maxVal + 1][histHeight + 1];

		for (int i = 0; i < maxVal + 1; i++) {
			histAry[i] = gaussAry[i] = bestFitGaussAry[i] = 0;
			for (int j = 0; j < histHeight + 1; j++)
				gaussGraph[i][j] = gapGraph[i][j] = ' ';
		}
	}

	public int loadHist(String inFile) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new FileReader(inFile));
		initFromHeader(bufferedReader);

		int maxFrequency = 0;  // store most frequent value in histAry
		String line;    // read the histogram data line by line from inFile (using BufferedReader bufferedReader)
		while ((line = bufferedReader.readLine()) != null) {
			String[] tokens = line.split("\\s+");  // regex to split by any amount of whitespace
			int pixelValue = Integer.parseInt(tokens[0]);  // 1st column: pixel value
			int frequency = Integer.parseInt(tokens[1]);   // 2nd column: frequency
			maxFrequency = getMaxFrequency(pixelValue, frequency, maxFrequency);
		}

		return maxFrequency;    // return the max frequency found in the histogram
	}

	public void printHist(String histFile) throws IOException {
		BufferedWriter outFile = new BufferedWriter(new FileWriter(histFile));
		outFile.write('\n' + numRows + ' ' + numCols + ' ' + minVal + ' ' + maxVal + '\n');
		int width = Integer.toString(maxVal).length(), fieldWidth = width + 1;
		String formatString = "%-" + fieldWidth + "d%d";
		for (int i = 0; i <= maxVal; i++)
			outFile.write((String.format(formatString, i, histAry[i])) + '\n');
		outFile.close();
	}

	public void dispHist(String histFile) throws IOException {
		BufferedWriter outFile = new BufferedWriter(new FileWriter(histFile, true));    // set `append` boolean to true to not overwrite
		outFile.write('\n' + numRows + ' ' + numCols + ' ' + minVal + ' ' + maxVal + '\n');
		for (int i = 0; i <= maxVal; i++) {
			outFile.write(i + " (" + histAry[i] + "):");
			for (int j = 0; j < histAry[i]; j++)
				outFile.write('+');
			outFile.newLine();
		}
		outFile.close();
	}

	public void copyArys(int[] ary1, int[] ary2) throws IllegalArgumentException {
		if (ary1.length != ary2.length)
			throw new IllegalArgumentException("Ary1 and Ary2 must be the same length");

		for (int i = 0; i < ary1.length; i++)    // "deep" copy (but not really, since it's primitives) to ary2 from ary1
			ary2[i] = ary1[i];
	}

	public int[] setZero(int[] ary) {
		for (int i = 0; i < ary.length; i++)
			ary[i] = 0;

		return ary;
	}

	public char[][] setBlanks(char[][] graph) {
		for (int i = 0; i < graph.length; i++)
			for (int j = 0; j < graph[i].length; j++)
				graph[i][j] = ' ';

		return graph;
	}

	public int biGaussian(String logFile) throws IOException {
		BufferedWriter outFile = new BufferedWriter(new FileWriter(logFile));
		outFile.write("Entering biGaussian method");
		double sum1, sum2, total, minSumDiff;
		int offset = (maxVal - minVal)/10;
		int dividePt = offset;
		int bestThr = dividePt;
		minSumDiff = 99999.0;    // a large value
		setZero(gaussAry);

		return 0;
	}

	public double fitGauss(int leftIndex, int rightIndex, int[] histAry, int[] gaussAry, int maxHeight, char[][] graph, String logFile) throws IOException {
		BufferedWriter outFile = new BufferedWriter(new FileWriter(logFile));
		outFile.write("Entering fitGauss method");
		double sum = 0.0, mean, var, gVal;
		mean = computeMean(leftIndex, rightIndex, maxHeight, histAry, logFile);
		var = computeVar(leftIndex, rightIndex, mean, histAry, logFile);
		for (int i = leftIndex; i <= rightIndex; i++) {
//			gVal =
		}

		outFile.close();
		return sum;
	}

	public double computeMean(int leftIndex, int rightIndex, int maxHeight, int[] histAry, String logFile) throws IOException {
		BufferedWriter outFile = new BufferedWriter(new FileWriter(logFile));
		outFile.write("Entering computeMean method");
		maxHeight = 0;
		double sum = 0;
		int numPixels = 0;
		for (int i = leftIndex; i < rightIndex; i++) {
			sum += (histAry[i]*i);
			numPixels += histAry[i];
			if (histAry[i] > maxHeight)
				maxHeight = histAry[i];
		}
		outFile.write("Leaving computeMean method");

		outFile.close();
		return sum/numPixels;
	}

	public double computeVar(int leftIndex, int rightIndex, double mean, int[] histAry, String logFile) throws IOException {
		BufferedWriter outFile = new BufferedWriter(new FileWriter(logFile));
		outFile.write("Entering computeVar method");
		double sum = 0.0;
		int numPixels = 0;
		for (int i = leftIndex; i < rightIndex; i++) {
			sum += Math.pow((double) histAry[i]*((double) i - mean), 2);
			numPixels += histAry[i];
		}
		outFile.write("Leaving computeVar method");

		outFile.close();
		return sum/numPixels;
	}

	public double modifiedGauss(int x, double mean, double var, int maxHeight) {
		return maxHeight*Math.pow(-(x) - mean, 2)/(2*var);
	}

	private void initFromHeader(BufferedReader bufferedReader) throws IOException {
		String headerLine = bufferedReader.readLine();
		if (headerLine != null) {
			String[] headerTokens = headerLine.split("\\s+");  // split by whitespace, get header from file: numRows numCols minVal maxVal
			numRows = Integer.parseInt(headerTokens[0]);        // [] <-- numRows numCols minVal maxVal
			numCols = Integer.parseInt(headerTokens[1]);        // [numRows] <-- numCols minVal maxVal
			minVal = Integer.parseInt(headerTokens[2]);         // [numRows, numCols] <-- minVal maxVal
			maxVal = Integer.parseInt(headerTokens[3]);         // [numRows, numCols, minVal] <-- maxVal
			histAry = new int[maxVal + 1];    // reinit histAry based on maxVal
		}
	}

	private int getMaxFrequency(int pixelValue, int frequency, int maxFrequency) {
		if (pixelValue >= 0 && pixelValue <= maxVal) {
			histAry[pixelValue] = frequency;
			if (frequency > maxFrequency)
				maxFrequency = frequency;    // increment maxFrequency if current frequency is greater
		}

		return maxFrequency;
	}

	public int getNumRows() {return numRows;}
	public int getNumCols() {return numCols;}
	public int getMinVal() {return minVal;}
	public int getMaxVal() {return maxVal;}
	public int getBiGaussThrVal() {return biGaussThrVal;}
	public int getHistHeight() {return histHeight;}
	public int getMaxHeight() {return maxHeight;}
	public void setNumRows(int numRows) {this.numRows = numRows;}
	public void setNumCols(int numCols) {this.numCols = numCols;}
	public void setMinVal(int minVal) {this.minVal = minVal;}
	public void setMaxVal(int maxVal) {this.maxVal = maxVal;}
	public void setBiGaussThrVal(int biGaussThrVal) {this.biGaussThrVal = biGaussThrVal;}
	public void setHistHeight(int histHeight) {this.histHeight = histHeight;}
	public void setMaxHeight(int maxHeight) {this.maxHeight = maxHeight;}
}