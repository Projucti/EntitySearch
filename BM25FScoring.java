package org.ranking;

import java.io.IOException;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndexContext;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class BM25FScoring {
	private String files[];
	private String originalFiles[];
	private int documentCount;
	private double avls; // average length of all the docx
	private String[] queryTerms;
	Map<String, Double> docxAndScore;
	Map<String, Integer> queryTermFrequencyInDocx;

	public String[] preprocessing(String[] files) {
		String[] noisyWords = { "ourselves", "hers", "between", "ourself", "but", "again", "there", "about", "once",
				"during", "out", "very", "having", "with", "they", "own", "an", "be", "some", "for", "do", "its",
				"yours", "such", "into", "of", "most", "itself", "other", "off", "is", "s", "am", "or", "who", "as",
				"from", "him", "each", "the", "themselves", "until", "below", "are", "we", "these", "your", "his",
				"through", "don", "nor", "me", "were", "her", "more", "himself", "this", "down", "should", "our",
				"their", "while", "above", "both", "up", "to", "ours", "had", "she", "all", "no", "when", "at", "any",
				"before", "them", "same", "and", "been", "have", "in", "will", "on", "does", "yourselves", "then",
				"that", "because", "what", "over", "why", "so", "can", "did", "not", "now", "under", "he", "you",
				"herself", "has", "just", "where", "too", "only", "myself", "which", "those", "i", "after", "few",
				"whom", "being", "if", "theirs", "my", "against", "a", "by", "doing", "it", "how", "further", "was",
				"here", "than" };
		for (int i = 0; i < files.length; i++) {

			System.err.println("file before===============" + files[i]);

			ArrayList<String> splitDoc = new ArrayList<String>();
			files[i] = files[i].toLowerCase().replaceAll("[^a-z\\s]", "");
			files[i] = files[i].replaceAll("\\s+", " ");
			String[] words = files[i].split(" ");
			for (int j = 0; j < words.length; j++) {
				if (words[j] != " " || words[j] != null) {
					splitDoc.add(words[j]);
				}
			}

			
			for (int k = 0; k < noisyWords.length; k++) {
				splitDoc.removeAll(Collections.singleton(noisyWords[k]));
			}
			files[i] = String.join(" ", splitDoc);

			System.err.println("file after===============" + files[i]);

		}

		return files;
	}

	public BM25FScoring(String file[], String queryString) {

		this.originalFiles = file.clone();

		this.files = preprocessing(file); // preprocessing
		this.documentCount = files.length;
		queryString = queryString.trim();
		String[] queryTerms = queryString.split("\\s+"); // use \\s+
		this.queryTerms = queryTerms;

		// setting avls and preprocessing
		for (int i = 0; i < files.length; i++) {
			this.avls += files[i].length();
			files[i] = files[i].toLowerCase();
		}
		this.avls = this.avls / documentCount;

		// setting ni
		queryTermFrequencyInDocx = new HashMap<String, Integer>();
		for (int j = 0; j < queryTerms.length; j++) {
			for (int i = 0; i < files.length; i++) {
				int tfsi = StringUtils.countMatches(files[i], queryTerms[j].toLowerCase());
				if (tfsi > 0) {

					if (!queryTermFrequencyInDocx.containsKey(queryTerms[j].toLowerCase())) {
						queryTermFrequencyInDocx.put(queryTerms[j].toLowerCase(), 1);
					} else {
						queryTermFrequencyInDocx.put(queryTerms[j].toLowerCase(),
								queryTermFrequencyInDocx.get(queryTerms[j].toLowerCase()) + 1);
					}

				}
			}

		}
		// System.out.println(queryTermFrequencyInDocx);
	}

	/*
	 * 
	 * Bs = ((1 âˆ’ bs) + bs Â· ls/avls))
	 * 
	 * where bs is a tunable parameter and
	 */
	public double getNormalisationFactor(String file, double bs) {
		int ls = file.length();
		return (double) ((1 - bs) + (bs * (ls / this.avls)));

	}

	public double getTermFrequency(String file, String queryTerm) {
		double tfi = 0.0;
		int tfsi = StringUtils.countMatches(file, queryTerm.toLowerCase());

		tfi = tfsi / getNormalisationFactor(file, 0.75);
		// System.out.println("tfsi is " + tfsi + " query term is " + queryTerm + " tfi
		// is " + tfi);
		return tfi;

	}

	private double sigmoid(String querStringTerm, String file, double k1, double tfi, double tf) {

		int ni = 0; // ni is the number of documents i occurs in.
		if (queryTermFrequencyInDocx.containsKey(querStringTerm.toLowerCase())) {
			ni = queryTermFrequencyInDocx.get(querStringTerm.toLowerCase());
		} else {
			ni = 0;
		}
		// System.out.println(ni+" for "+querStringTerm);
		double wiIDF = Math.log(1 + (documentCount - ni + 0.5) / (ni + 0.5));// wiIDF is inverse document frequency
		// sigmoid function
		double wiBM25F = (tf / (k1 + tfi)) * wiIDF;
		// DecimalFormat df = new DecimalFormat("#.#######");
		// System.out.println("score for " + querStringTerm + " is :" + wiBM25F);
		return wiBM25F;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Double> performRanking() {

		docxAndScore = new HashMap<String, Double>();

		for (int i = 0; i < this.files.length; i++) {
			double wBM25F = 0.0;
			double tf = 0.0; // sum of all term frequencies

			for (int j = 0; j < this.queryTerms.length; j++) {
				tf += getTermFrequency(this.files[i], this.queryTerms[j]);

			}
			// System.out.println("tf is " + tf);
			for (int j = 0; j < this.queryTerms.length; j++) {
				wBM25F += sigmoid(queryTerms[j], files[i], 1.5, getTermFrequency(files[i], this.queryTerms[j]), tf);
				// System.err.println(sigmoid(queryTerms[j], files[i], 1.5,
				// getTermFrequency(files[i], this.queryTerms[j]), tf));
			}
			System.err.println(i + "   is " + wBM25F + "    ---->" + this.files[i] + "------->");
			docxAndScore.put(this.originalFiles[i], wBM25F);

		}

		@SuppressWarnings("unchecked")
		Map sortedMap = new TreeMap(new ValueComparator(docxAndScore));
		sortedMap.putAll(docxAndScore);

		return sortedMap;

	}

	public static void main(String[] args) throws IOException {

		/*
		 * String[] files = {
		 * "Dresden (German pronunciation: [ËˆdÊ�eË�sdnÌ©] ) is the capital city of the Free State of Saxony in Germany. It is situated in a valley on the River Elbe, near the border with the Czech Republic. Dresden has a long history as the capital and royal residence for the Electors and Kings of Saxony, who for centuries furnished the city with cultural and artistic splendour. The city was known as the Jewel Box, because of its baroque and rococo city centre. The controversial American and British bombing of Dresden in World War II towards the end of the war killed approximately 25,000, many of whom were civilians, and destroyed the entire city centre. After the war restoration work has helped to reconstruct parts of the historic inner city, including the Katholische Hofkirche, the Semper Oper and the Dresdner Frauenkirche as well as the suburbs. Before and since German reunification in 1990, Dresden was and is a cultural, educational, political, and economic centre of Germany and Europe. The Dresden University of Technology is one of the 10 largest universities in Germany and part of the German Universities Excellence Initiative."
		 * ,
		 * "Dresden 45 was a hardcore punk and crossover thrash band from Houston, Texas, also known as D'45 and variantly, D45. They were one of the first hardcore bands to implement a guitar-driven heavy metal sound into their music. Like other Houston bands Dirty Rotten Imbeciles and Verbal Abuse, Dresden 45 played a breathless, high-speed type of hardcore punk now referred to as thrashcore."
		 * ,
		 * "Sportgemeinschaft Dynamo Dresden e.V., commonly known as SG Dynamo Dresden or Dynamo Dresden, is a German association football club, based in Dresden, Saxony. It was founded on 12 April 1953, as a club affiliated with the East German police, and became one of the most popular and successful clubs in East German football, winning eight league titles. After the reunification of Germany, Dynamo played four seasons in the top division Bundesliga (1991â€“95), but have since drifted between the second and fourth tiers. The club will begin the 2016â€“17 season in the 2. Bundesliga."
		 * ,
		 * "The Zwinger (German: Dresdner Zwinger, IPA: [ËˆdÊ�eË�zdnÉ� ËˆtÍ¡svÉªÅ‹É�]) is a palace in the eastern German city of Dresden, built in Rococo style and designed by court architect MatthÃ¤us Daniel PÃ¶ppelmann. It served as the orangery, exhibition gallery and festival arena of the Dresden Court. The location was formerly part of the Dresden fortress of which the outer wall is conserved. The name derives from the German word Zwinger (an enclosed killing ground in front of a castle or city gate); it was for the cannons that were placed between the outer wall and the major wall. The Zwinger was not enclosed until the Neoclassical building by Gottfried Semper called the Semper Gallery was built on its northern side. Today, the Zwinger is a museum complex that contains the GemÃ¤ldegalerie Alte Meister (Old Masters Picture Gallery), the Dresden Porcelain Collection (Porzellansammlung) and the Mathematisch-Physikalischer Salon (Royal Cabinet of Mathematical and Physical Instruments)."
		 * ,
		 * "The Dresden Frauenkirche (German: Dresdner Frauenkirche, IPA: [ËˆfÊ�aÊŠÉ™nËŒkÉªÊ�Ã§É™], Church of Our Lady) is a Lutheran church in Dresden, the capital of the German state of Saxony. An earlier church building was Roman Catholic until it became Protestant during the Reformation, and was replaced in the 18th century by a larger Baroque Lutheran building. It is considered an outstanding example of Protestant sacred architecture, featuring one of the largest domes in Europe. It now also serves as a symbol of reconciliation between former warring enemies. Built in the 18th century, the church was destroyed in the bombing of Dresden during World War II. The remaining ruins were left for 50 years as a war memorial, following decisions of local East German leaders. The church was rebuilt after the reunification of Germany, starting in 1994. The reconstruction of its exterior was completed in 2004, and the interior in 2005. The church was reconsecrated on 30 October 2005 with festive services lasting through the Protestant observance of Reformation Day on 31 October. The surrounding Neumarkt square with its many valuable baroque buildings was also reconstructed in 2004. The Frauenkirche is often called a cathedral, but it is not the seat of a bishop; the church of the Landesbischof of the Evangelical-Lutheran Church of Saxony is the Church of the Cross. Once a month, an Anglican Evensong is held in English, by clergy from St. George's Anglican Church, Berlin."
		 * ,
		 * "Dresden Cathedral, or the Cathedral of the Holy Trinity, Dresden, previously the Catholic Church of the Royal Court of Saxony, called in German Katholische Hofkirche and since 1980 also known as Kathedrale Sanctissimae Trinitatis, is the Roman Catholic Cathedral of Dresden. Always the most important Catholic church of the city, it was elevated to the status of cathedral of the Roman Catholic Diocese of Dresden-Meissen in 1964. It is located near the Elbe River in the historic center of Dresden, Germany."
		 * ,
		 * "The DDV-Stadion is a football stadium in Dresden, Saxony. It is the current home of Dynamo Dresden. The facility was previously known as the Rudolf-Harbig-Stadion (from 1951 to 1971 and from 1990 to 2010) and the Dynamo-Stadion (from 1971 to 1990). In December 2010, the naming rights were sold for 5 years to Bavarian energy company Goldgas which wanted to promote its GlÃ¼cksgas brandname. Sports facilities have existed on the site of the stadium since 1874. The stadium also hosts events other than soccer games and has hosted several home games of the Dresden Monarchs American Football team of the German Football League, including their lone home appearance in the BIG6 European Football League in 2014."
		 * ,
		 * "Dresden Airport (IATA: DRS, ICAO: EDDC) is the international airport in Dresden, the capital of the German Free State of Saxony. It is located in Klotzsche, a district of Dresden 9 km (5.6 mi) north of the city centre. It was known in German as Flughafen Dresden-Klotzsche. Destinations from the airport include a few European cities and several holiday destinations. EADS EFW, a subsidiary of EADS and mainly responsible for freighter aircraft conversion, is based at the airport."
		 * 
		 * };
		 */
		ArrayList<String> fileList = new ArrayList<String>();
		TripleIndexContext context = new TripleIndexContext();
		String findStr = "dresden";
		List<Triple> list = context.search(findStr, null, null);
		String file = "";
		for (Triple triple : list) {
			file = triple.getObject() + triple.getSubject() + triple.getPredicate();
			fileList.add(file);
		}
		String[] files = new String[fileList.size()];
		for (int i = 0; i < fileList.size(); i++) {
			files[i] = fileList.get(i);
		}
		BM25FScoring bm25fScoring = new BM25FScoring(files, findStr);
		System.out.println(bm25fScoring.performRanking());

	}
}

@SuppressWarnings("rawtypes")
class ValueComparator implements Comparator {
	Map map;

	public ValueComparator(Map map) {
		this.map = map;
	}

	@SuppressWarnings("unchecked")
	public int compare(Object keyA, Object keyB) {
		Comparable valueA = (Comparable) map.get(keyA);
		Comparable valueB = (Comparable) map.get(keyB);
		return valueB.compareTo(valueA);
	}
}