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


public class SimpleHuff {
	public SimpleHuff() {
		Hashtable<Character, Integer> counts = new Hashtable<Character, Integer>();

		for (String filePath : new String[]{"words-english-tabwpd-zg2009.txt", "words-myanmar-tabwdp-zg2009.txt"}) {
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
				for (String line = reader.readLine(); line!=null; line=reader.readLine()) {
					line = line.trim();
					if (line.length()==0)
						continue;

					//Format
					line.replaceAll("\\t", "|");
					if (!line.endsWith("\n"))
						line += "\n";

					//Check and count
					for (char c : line.toCharArray()) {
						if (!counts.containsKey(c))
							counts.put(c, 0);
						counts.put(c, counts.get(c)+1);
					}
				}
			} catch (IOException ex) {
				System.out.println("IO exception: " + ex.toString());
				return;
			}

			try {
				reader.close();
			} catch (IOException ex) {}
		}


		//Sort
		ArrayList<Map.Entry<Character, Integer>> arrayRep=new ArrayList<Map.Entry<Character, Integer>>(counts.entrySet());
		Collections.sort(arrayRep, new Comparator() {
			public int compare(Object obj1, Object obj2){

				int result=0;Map.Entry e1 = (Map.Entry)obj1 ;

				Map.Entry e2 = (Map.Entry)obj2 ;//Sort based on values.

				Integer value1 = (Integer)e1.getValue();
				Integer value2 = (Integer)e2.getValue();

				if(value1.compareTo(value2)==0){

					String word1 = e1.getKey()+"";
					String word2=e2.getKey()+"";

					//Sort String in an alphabetical order
					result = word1.compareToIgnoreCase(word2);

				} else{
					//Sort values in a descending order
					result = value2.compareTo( value1 );
				}

				return result;
			}
		});

		//Display
		for (Map.Entry entry : arrayRep) {
			System.out.println(Integer.toHexString((Character)entry.getKey()) + "\t" + entry.getValue());
		}

	}


	public static void main(String[] args) {
		new SimpleHuff();
	}
}

