import java.io.UnsupportedEncodingException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class NumBits {
	private int getNumBits(ArrayList<Integer> sort, int c) {
		for (int i=0; i<sort.size(); i++) {
			if (sort.get(i) == c) {
				return i==sort.size()-1 ? i : i+1;
			}
		}
		throw new RuntimeException("Not in list: " + (int)c);
	}

	public NumBits() {
		//Load Huffman encoding
		ArrayList<Integer> numBits = new ArrayList<Integer>();
		ArrayList<Integer> realCounts = new ArrayList<Integer>();
		int totalBits = 0;
		int totalBitsFull = 0;
		boolean repeatFile = false;
		for (String filePath : new String[]{"huffman.txt", "words-english-tabwpd-zg2009.txt", "words-english-tabwpd-zg2009.txt"}) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
			} catch (FileNotFoundException ex) {
				System.out.println("File not found.");
				return;
			} catch (UnsupportedEncodingException ex1) {
				System.out.println("Unsupported encoding (UTF-8).");
				return;
			}

			//Read each line
			try {
				OUTER:
				for (String line = reader.readLine(); line!=null; line=reader.readLine()) {
					line = line.trim();
					if (line.length()==0)
						continue;

					//Huffman?
					if (filePath.equals("huffman.txt")) {
						numBits.add(Integer.parseInt(line, 16));
					} else {
						for (char c : line.toCharArray()) {
							if (!repeatFile) {
								//Put in a new array
								if (!realCounts.contains((int)c))
									realCounts.add((int)c);
							} else {
								//Count
								totalBits += getNumBits(realCounts, c);
								totalBitsFull++;
							}

							//Only WORDS, for now
							if (c=='\t')
								continue OUTER;
						}
					}
				}

				//Adjust
				if (!filePath.equals("huffman.txt") && !repeatFile) {
					repeatFile = true;
				}
			} catch (IOException ex) {
				System.out.println("IO exception: " + ex.toString());
				return;
			}

			try {
				reader.close();
			} catch (IOException ex) {}
		}

		//Display
		System.out.println("[ENG] Number of letters in words: " + totalBitsFull + "  (" + (totalBitsFull*2)/(1024) + " kb)");
		System.out.println("[ENG] Number of bits w/ fixed-length: " + totalBitsFull*6 + "+" + "  (" + ((totalBitsFull*6)+(int)Math.pow(2,6))/(1024*8) + " kb)");
		System.out.println("[ENG] Number of bits in words w/ Huffman: " + totalBits + "  (" + (totalBits)/(1024*8) + " kb)");
		System.out.println("Array sizes: " + numBits.size() + "," + realCounts.size());
	}


	// 210 kb for all words in ENG (fast list display), stored w/ minimum number of bits.
	// After that, segment?
	public static void main(String[] args) {
		new NumBits();
	}
}

