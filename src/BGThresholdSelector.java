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
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		initFromHeader(reader);
		int maxFrequency = 0;
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty()) continue;
			String[] tokens = line.split("\\s+");
			if (tokens.length >= 2) {
				int pixelValue = Integer.parseInt(tokens[0]);
				int frequency = Integer.parseInt(tokens[1]);
				maxFrequency = getMaxFrequency(pixelValue, frequency, maxFrequency);
			}
		}
		reader.close();

		histHeight = maxFrequency;
		maxHeight = maxFrequency;

		gaussGraph = new char[maxVal + 1][histHeight + 1];
		gapGraph = new char[maxVal + 1][histHeight + 1];
		setBlanks(gaussGraph);
		setBlanks(gapGraph);

		return maxFrequency;
	}

	public void printHist(BufferedWriter outFile) throws IOException {
		outFile.write('\n' + numRows + ' ' + numCols + ' ' + minVal + ' ' + maxVal + '\n');

		int width = Integer.toString(maxVal).length(), fieldWidth = width + 1;
		String formatString = "%-" + fieldWidth + "d%d";

		for (int i = 0; i <= maxVal; i++)
			outFile.write((String.format(formatString, i, histAry[i])) + '\n');
	}


	public void dispHist(BufferedWriter outFile) throws IOException {
		outFile.write('\n' + numRows + ' ' + numCols + ' ' + minVal + ' ' + maxVal + '\n');

		for (int i = 0; i <= maxVal; i++) {
			outFile.write(i + " (" + histAry[i] + "):");
			for (int j = 0; j < histAry[i]; j++)
				outFile.write('+');
			outFile.newLine();
		}
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

	public int biGaussian(BufferedWriter logFile) throws IOException {
		logFile.write("Entering biGaussian method\n");

		double sum1, sum2, total, minSumDiff;
		int offset = (maxVal - minVal) / 10;
		int dividePt = offset;
		int bestThr = dividePt;
		minSumDiff = Double.MAX_VALUE;

		while (dividePt < (maxVal - offset)) {
			setZero(gaussAry);
			sum1 = fitGauss(0, dividePt, histAry, gaussAry, maxHeight, gaussGraph, logFile);
			sum2 = fitGauss(dividePt, maxVal, histAry, gaussAry, maxHeight, gaussGraph, logFile);
			total = sum1 + sum2;
			if (total < minSumDiff) {
				minSumDiff = total;
				bestThr = dividePt;
				copyArys(gaussAry, bestFitGaussAry);
			}
			logFile.write("\nIn biGaussian(): dividePt=" + dividePt + ", sum1=" + sum1 + ", sum2=" + sum2 + ", total=" + total + ", minSumDiff=" + minSumDiff + ", bestThr=" + bestThr);
			dividePt++;
		}

		logFile.write("\nLeaving biGaussian method\n");
		return bestThr;
	}


	public double fitGauss(int leftIndex, int rightIndex, int[] histAry, int[] gaussAry, int maxHeight, char[][] graph, BufferedWriter logFile) throws IOException {
		logFile.write("Entering fitGauss method\n");

		double sum = 0.0, mean, var, gVal;
		mean = computeMean(leftIndex, rightIndex, maxHeight, histAry, logFile);
		var = computeVar(leftIndex, rightIndex, mean, histAry, logFile);

		for (int i = leftIndex; i <= rightIndex; i++) {
			gVal = modifiedGauss(i, mean, var, maxHeight);
			sum += Math.abs(gVal - histAry[i]);
			gaussAry[i] = (int) gVal;
		}

		logFile.write("Leaving fitGauss method. Sum is " + sum + "\n");
		return sum;
	}


	public double computeMean(int leftIndex, int rightIndex, int maxHeight, int[] histAry, BufferedWriter logFile) throws IOException {
		logFile.write("Entering computeMean method\n");

		maxHeight = 0;
		double sum = 0;
		int numPixels = 0;

		for (int i = leftIndex; i < rightIndex; i++) {
			sum += (histAry[i] * i);
			numPixels += histAry[i];
			if (histAry[i] > maxHeight)
				maxHeight = histAry[i];
		}

		logFile.write("Leaving computeMean method\n");
		return sum / numPixels;
	}


	public double computeVar(int leftIndex, int rightIndex, double mean, int[] histAry, BufferedWriter logFile) throws IOException {
		logFile.write("\nEntering computeVar method");

		double sum = 0.0;
		int numPixels = 0;

		for (int i = leftIndex; i < rightIndex; i++) {
			sum += Math.pow((double) histAry[i] * ((double) i - mean), 2);
			numPixels += histAry[i];
		}

		logFile.write("\nLeaving computeVar method");
		return sum / numPixels;
	}


	public double modifiedGauss(int x, double mean, double var, int maxHeight) {
		return maxHeight*Math.exp(-Math.pow(x - mean, 2)/(2*var));
	}

	public void plotGaussGraph(int[] bestFitGaussAry, char[][] gaussGraph, BufferedWriter logFile) throws IOException {
		logFile.write("\nEntering plotGaussGraph() method\n");

		setBlanks(gaussGraph);
		for (int i = 0; i <= maxVal; i++)
			if (bestFitGaussAry[i] > 0)
				for (int j = 0; j < bestFitGaussAry[i]; j++)
					gaussGraph[i][j] = '*';

		logFile.write("\nLeaving plotGaussGraph() method\n");
	}


	public void plotGapGraph(int[] histAry, int[] bestFitGaussAry, BufferedWriter logFile) throws IOException {
		logFile.write("\nEntering plotGapGraph() method\n");

		setBlanks(gapGraph);
		int end1, end2;
		for (int i = 0; i <= maxVal; i++) {
			if (bestFitGaussAry[i] <= histAry[i]) {
				end1 = bestFitGaussAry[i];
				end2 = histAry[i];
			} else {
				end1 = histAry[i];
				end2 = bestFitGaussAry[i];
			}
			for (int j = end1; j <= end2; j++)
				gapGraph[i][j] = '@';
		}

		logFile.write("\nLeaving plotGapGraph() method\n");
	}


	public void printBestFitGauss(BufferedWriter outFile) throws IOException {
		outFile.write('\n' + numRows + ' ' + numCols + ' ' + minVal + ' ' + maxVal + '\n');

		int width = Integer.toString(maxVal).length(), fieldWidth = width + 1;
		String formatString = "%-" + fieldWidth + "d%d";

		for (int i = 0; i <= maxVal; i++)
			outFile.write((String.format(formatString, i, bestFitGaussAry[i])) + '\n');
	}


	public void dispGaussGraph(BufferedWriter outFile) throws IOException {
		outFile.write('\n' + numRows + ' ' + numCols + ' ' + minVal + ' ' + maxVal + '\n');

		for (int i = 0; i <= maxVal; i++) {
			outFile.write(i + " :");
			for (int j = 0; j <= histHeight; j++) {
				char c = gaussGraph[i][j];
				if (c != ' ')
					outFile.write(c);
			}
			outFile.newLine();
		}
	}

	public void dispGapGraph(BufferedWriter outFile) throws IOException {
		outFile.write(numRows + ' ' + numCols + ' ' + minVal + ' ' + maxVal + '\n');

		for (int i = 0; i <= maxVal; i++) {
			outFile.write(i + " :");
			for (int j = 0; j <= histHeight; j++) {
				char c = gapGraph[i][j];
				if (c != ' ')
					outFile.write(c);
			}
			outFile.newLine();
		}

		outFile.close();
	}

	private void initFromHeader(BufferedReader bufferedReader) throws IOException {
		String headerLine = bufferedReader.readLine();
		if (headerLine != null) {
			String[] headerTokens = headerLine.split("\\s+");
			numRows = Integer.parseInt(headerTokens[0]);
			numCols = Integer.parseInt(headerTokens[1]);
			minVal = Integer.parseInt(headerTokens[2]);
			maxVal = Integer.parseInt(headerTokens[3]);
			histAry = new int[maxVal + 1]; // reinit histAry based on maxVal

			gaussAry = new int[maxVal + 1];
			bestFitGaussAry = new int[maxVal + 1];
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
	public int[] getHistAry() {return histAry;}
	public int[] getGaussAry() {return gaussAry;}
	public int[] getBestFitGaussAry() {return bestFitGaussAry;}
	public char[][] getGaussGraph() {return gaussGraph;}
	public char[][] getGapGraph() {return gapGraph;}
}