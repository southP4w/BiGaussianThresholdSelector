import java.io.*;

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

		for (int i = 0; i < histAry.length; i++)
			histAry[i] = 0;
		for (int i = 0; i < gaussAry.length; i++)
			gaussAry[i] = 0;
		for (int i = 0; i < bestFitGaussAry.length; i++)
			bestFitGaussAry[i] = 0;
		for (int i = 0; i < maxVal + 1; i++)
			for (int j = 0; j < histHeight + 1; j++)
				gaussGraph[i][j] = ' ';
		for (int i = 0; i < maxVal + 1; i++)
			for (int j = 0; j < histHeight + 1; j++)
				gapGraph[i][j] = ' ';
	}

	public int loadHist(String inFile) {
		int maxFrequency = 0;  // store most frequent value in histAry

		try (BufferedReader br = new BufferedReader(new FileReader(inFile))) {
			extractToHistAry(br);

			String line;    // read the histogram data line by line from inFile (using BufferedReader br)
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split("\\s+");  // Split by any whitespace
				int pixelValue = Integer.parseInt(tokens[0]);  // First column: pixel intensity
				int frequency = Integer.parseInt(tokens[1]);   // Second column: frequency

				maxFrequency = getMaxFrequency(pixelValue, frequency, maxFrequency);
			}
		} catch (IOException e) {
			System.out.println("Error reading file: " + inFile);
			e.printStackTrace();
		}

		return maxFrequency;    // return the max frequency found in the histogram
	}

	public void printHist(String outFileName) {
		try (BufferedWriter histCountFile = new BufferedWriter(new FileWriter(outFileName))) {
			histCountFile.write(numRows + " " + numCols + " " + minVal + " " + maxVal);
			histCountFile.newLine();

			int width = Integer.toString(maxVal).length();
			int fieldWidth = width + 1;

			String formatString = "%-" + fieldWidth + "d%d";
			for (int i = 0; i <= maxVal; i++) {
				String line = String.format(formatString, i, histAry[i]);
				histCountFile.write(line);
				histCountFile.newLine();
			}
		} catch (IOException e) {
			System.out.println("Error writing file: " + outFileName);
			e.printStackTrace();
		}
	}

	public void dispHist(String histGraphFileName) {
		try (BufferedWriter histGraphFile = new BufferedWriter(new FileWriter(histGraphFileName))) {
			histGraphFile.write(numRows + " " + numCols + " " + minVal + " " + maxVal);
			histGraphFile.newLine();
			for (int i = 0; i <= maxVal; i++) {
				histGraphFile.write(i + " (" + histAry[i] + "):");
				for (int j = 0; j < histAry[i]; j++)
					histGraphFile.write("+");
				histGraphFile.newLine();
			}
		} catch (IOException e) {
			System.out.println("Error writing file: " + histGraphFileName);
			e.printStackTrace();
		}
	}

	public void copyArys(int[] ary1, int[] ary2) {
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

	private void extractToHistAry(BufferedReader br) throws IOException {
		String headerLine = br.readLine();    // read header from file (numRows numCols minVal maxVal)
		if (headerLine != null) {
			String[] headerTokens = headerLine.split("\\s+");  // regex to split by any whitespace
			numRows = Integer.parseInt(headerTokens[0]);
			numCols = Integer.parseInt(headerTokens[1]);
			minVal = Integer.parseInt(headerTokens[2]);
			maxVal = Integer.parseInt(headerTokens[3]);
			histAry = new int[maxVal + 1];    // reinit histAry based on maxVal
		}
	}

	private int getMaxFrequency(int pixelValue, int frequency, int maxFrequency) {
		if (pixelValue >= 0 && pixelValue <= maxVal) {
			histAry[pixelValue] = frequency;
			if (frequency > maxFrequency)    // Update the maxFrequency if the current frequency is greater
				maxFrequency = frequency;
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