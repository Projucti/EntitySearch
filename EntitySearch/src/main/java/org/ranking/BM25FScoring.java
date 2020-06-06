package org.ranking;

import java.io.IOException;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;

public class BM25FScoring {
	private String files[];
	private String originalFiles[];
	private int documentCount;
	private double avls; // average length of all the docx
	private String[] queryTerms;
	Map<String, Double> docxAndScore;
	Map<String, Integer> queryTermFrequencyInDocx;

	/*
	 * Removal of stop words/ noise from the document
	 */
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

			ArrayList<String> splitDoc = new ArrayList<String>();
			files[i] = files[i].toLowerCase(); // .replaceAll("[^a-z\\s]", "");
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
			files[i] = getStemDocument(files[i]); // normalising file

		}

		return files;
	}

	public BM25FScoring() {
	}

	public BM25FScoring(String file[], String queryString) {

		this.originalFiles = file.clone();
		for (int i = 0; i < this.originalFiles.length; i++) {
			// System.out.println(" document "+i+" :"+this.originalFiles[i]);
		}

		this.files = preprocessing(file); // preprocessing
		this.documentCount = files.length;
		queryString = queryString.trim();
		// String[] queryTerms = queryString.split("\\s+"); // use \\s+
		// this.queryTerms = queryTerms;
		String[] q = new String[1];
		q[0] = queryString.replaceAll("_", " ");
		this.queryTerms = q;

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

	}

	/*
	 * 
	 * Bs = ((1 âˆ’ bs) + bs Â· ls/avls))
	 * 
	 * where bs is a tunable parameter
	 */
	private double getNormalisationFactor(String file, double bs) {
		int ls = file.length();
		return (double) ((1 - bs) + (bs * (ls / this.avls)));

	}

	/*
	 * 
	 * THis method returns the term frequency of the query term in a document
	 */
	private double getTermFrequency(String file, String queryTerm) {
		double tfi = 0.0;
		int tfsi = StringUtils.countMatches(file, queryTerm.toLowerCase());

		tfi = tfsi / getNormalisationFactor(file, 0.75);

		return tfi;

	}

	private double sigmoid(String querStringTerm, String file, double k1, double tfi, double tf) {

		int ni = 0; // ni is the number of documents i occurs in.
		if (queryTermFrequencyInDocx.containsKey(querStringTerm.toLowerCase())) {
			ni = queryTermFrequencyInDocx.get(querStringTerm.toLowerCase());
		} else {
			ni = 0;
		}
		double wiIDF = Math.log(1 + (documentCount - ni + 0.5) / (ni + 0.5));// wiIDF is inverse document frequency
		// sigmoid function
		double wiBM25F = (tf / (k1 + tfi)) * wiIDF;

		return wiBM25F;

	}

	/*
	 * 
	 * NLP Named Entity Recognition
	 * 
	 * This method compares the header of the document with the query term to match
	 * the entity.
	 * 
	 */

	private boolean isSameEntity(String entity1, String entity2) {
		String entity1Type = "";
		String entity2Type = "";

		Properties props = new Properties();

		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");

		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// pipeline.addAnnotator(new TokensRegexAnnotator(
		// "/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/training.rules"));
		CoreDocument document = new CoreDocument(entity1);
		pipeline.annotate(document);

		// get confidences for entity1
		for (CoreEntityMention em : document.entityMentions()) {
			// System.out.println(em.text() + "\t" + em.entityTypeConfidences());
		//	System.out.println("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
			entity1Type = em.entityType(); //em.entityTypeConfidences().toString();

		}
		
		// get confidences for tokens
	//	for (CoreLabel token : document.tokens()) {
			// System.out.println(token.word() + "\t" +
			// token.get(CoreAnnotations.NamedEntityTagProbsAnnotation.class));
	//	}

		// get confidences for entity2
		CoreDocument document2 = new CoreDocument(entity2);
		pipeline.annotate(document2);

		for (CoreEntityMention em : document.entityMentions()) {
			System.out.println("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
			entity2Type = em.entityType(); //em.entityTypeConfidences().toString();

		}
		// get confidences for tokens
	//	for (CoreLabel token : document.tokens()) {
			// System.out.println(token.word() + "\t" +
			// token.get(CoreAnnotations.NamedEntityTagProbsAnnotation.class));
	//	}

		if (entity1Type.equalsIgnoreCase(entity2Type)) {
			return true;
		} else
			return false;
	}

	/*
	 * Custom NER Model Classifier
	 */
	@SuppressWarnings("rawtypes")
	private CRFClassifier getModel(String modelPath) {
		return CRFClassifier.getClassifierNoExceptions(modelPath);
	}

	/*
	 * Custom NER Model Tagger
	 */
	@SuppressWarnings("rawtypes")
	private String doTagging(CRFClassifier model, String input) {
		input = input.trim();
		System.out.println(input + "=>" + model.classifyToString(input));
		return model.classifyToString(input);
	}

	private boolean isSameEntityByCustomModel(String entity1, String entity2) {
		String entity1Type = "";
		String entity2Type = "";

		entity1Type = this.doTagging(
				this.getModel(
						"/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/ner-model.ser.gz"),
				entity1);
		entity2Type = this.doTagging(
				this.getModel(
						"/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/ner-model.ser.gz"),
				entity2);

		if (entity1Type.equalsIgnoreCase(entity2Type)) {
			return true;
		}
		return false;
	}

	private String getEntityType(String entity) {
		//return this.doTagging(
		//		this.getModel(
		//				"/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/ner-model.ser.gz"),
		//		entity);

		String entity1Type = "";		

		Properties props = new Properties();

		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");

		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		CoreDocument document = new CoreDocument(entity);
		pipeline.annotate(document);
		for (CoreEntityMention em : document.entityMentions()) {
			
			System.out.println("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
			entity1Type = em.entityType();//em.entityTypeConfidences().toString();

		}
		return entity1Type;
	}

	/*
	 * Lemmatization
	 * 
	 * This method normalize each word in the document to its lemma / stem form
	 * 
	 */
	private String getStemDocument(String originalFile) {

		String stemFile = "";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		CoreDocument document = new CoreDocument(originalFile);
		pipeline.annotate(document);

		List<CoreLabel> coreLabelList = document.tokens();
		for (CoreLabel coreLabel : coreLabelList) {

			stemFile += coreLabel.lemma() + " ";
		}
		return stemFile.replaceAll("[^a-z\\s]", "");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Double> performRanking() {

		docxAndScore = new HashMap<String, Double>();

		for (int i = 0; i < this.files.length; i++) {
			double wBM25F = 0.0;
			double tf = 0.0; // sum of all term frequencies
			double headerWeight = 5.0;
			double entitySameWeight = 2.0;

			for (int j = 0; j < this.queryTerms.length; j++) {
				tf += getTermFrequency(this.files[i], this.queryTerms[j]);

			}

			for (int j = 0; j < this.queryTerms.length; j++) {
				wBM25F += sigmoid(queryTerms[j], files[i], 1.5, getTermFrequency(files[i], this.queryTerms[j]), tf);
			}

			// if (this.originalFiles[i].split(" <HEADER> ")[0].replaceAll("_", "
			// ").toLowerCase().trim()
			// .equalsIgnoreCase(this.queryTerms[0].trim())) {

			// NER type check to add weight
			if (isSameEntity(this.originalFiles[i].split(" <HEADER> ")[0].replaceAll("_", " ").toLowerCase().trim(),
					this.queryTerms[0].trim())) {
				wBM25F += entitySameWeight;			
			}
			if (this.originalFiles[i].split(" <HEADER> ")[0].replaceAll("_", " ").toLowerCase().trim()
					.equalsIgnoreCase(this.queryTerms[0].trim())) {
				wBM25F += headerWeight;
				System.err.println("Header weight added for  " + this.originalFiles[i].split(" <HEADER> ")[0]);
			}
			// append Entity type at the end.
			String entityType="";
			if (!getEntityType(this.originalFiles[i].split(" <HEADER> ")[0]).equals("")) {
				entityType = getEntityType(this.originalFiles[i].split(" <HEADER> ")[0]);
			}
			else {
				entityType = "O";
			}
			docxAndScore.put(
					this.originalFiles[i] + "<ENTITYTYPE>" + entityType,
					wBM25F);
			// docxAndScore.put(this.originalFiles[i], wBM25F);

		}

		Map sortedMap = new TreeMap(new ValueComparator(docxAndScore));
		sortedMap.putAll(docxAndScore);
		return sortedMap;

	}

	public static void main(String[] args) throws IOException {

		/*
		 * ArrayList<String> fileList = new ArrayList<String>(); TripleIndexContext
		 * context = new TripleIndexContext(); String findStr = "Dresden";
		 * 
		 * List<Triple> list = context.search(null, findStr, null); String file = "";
		 * for (Triple triple : list) { file =
		 * triple.getPredicate()+" <HEADER> "+triple.getSubject();
		 * System.out.println(" object is " + triple.getObject() + " predicate is " +
		 * triple.getPredicate()); fileList.add(file); } String[] files = new
		 * String[fileList.size()]; for (int i = 0; i < fileList.size(); i++) { files[i]
		 * = fileList.get(i); }
		 */
		String[] files = new String[25];

		files[0] = "Dresden  <HEADER>  Dresden (German pronunciation: [ˈdʁeːsdn̩] ) is the capital city of the Free State of Saxony in Germany. It is situated in a valley on the River Elbe, near the border with the Czech Republic. Dresden has a long history as the capital and royal residence for the Electors and Kings of Saxony, who for centuries furnished the city with cultural and artistic splendour. The city was known as the Jewel Box, because of its baroque and rococo city centre. The controversial American and British bombing of Dresden in World War II towards the end of the war killed approximately 25,000, many of whom were civilians, and destroyed the entire city centre. After the war restoration work has helped to reconstruct parts of the historic inner city, including the Katholische Hofkirche, the Semper Oper and the Dresdner Frauenkirche as well as the suburbs. Before and since German reunification in 1990, Dresden was and is a cultural, educational, political, and economic centre of Germany and Europe. The Dresden University of Technology is one of the 10 largest universities in Germany and part of the German Universities Excellence Initiative.\r\n"
				+ "\r\n" + "";
		files[1] = "Dresden 45  <HEADER>  Dresden 45 was a hardcore punk and crossover thrash band from Houston, Texas, also known as D'45 and variantly, D45. They were one of the first hardcore bands to implement a guitar-driven heavy metal sound into their music. Like other Houston bands Dirty Rotten Imbeciles and Verbal Abuse, Dresden 45 played a breathless, high-speed type of hardcore punk now referred to as thrashcore.";

		files[2] = "Dynamo Dresden  <HEADER>  Sportgemeinschaft Dynamo Dresden e.V., commonly known as SG Dynamo Dresden or Dynamo Dresden, is a German association football club, based in Dresden, Saxony. It was founded on 12 April 1953, as a club affiliated with the East German police, and became one of the most popular and successful clubs in East German football, winning eight league titles. After the reunification of Germany, Dynamo played four seasons in the top division Bundesliga (1991–95), but have since drifted between the second and fourth tiers. The club will begin the 2016–17 season in the 2. Bundesliga.";

		files[3] = "Zwinger Dresden  <HEADER>  The Zwinger (German: Dresdner Zwinger, IPA: [ˈdʁeːzdnɐ ˈt͡svɪŋɐ]) is a palace in the eastern German city of Dresden, built in Rococo style and designed by court architect Matthäus Daniel Pöppelmann. It served as the orangery, exhibition gallery and festival arena of the Dresden Court. The location was formerly part of the Dresden fortress of which the outer wall is conserved. The name derives from the German word Zwinger (an enclosed killing ground in front of a castle or city gate); it was for the cannons that were placed between the outer wall and the major wall. The Zwinger was not enclosed until the Neoclassical building by Gottfried Semper called the Semper Gallery was built on its northern side. Today, the Zwinger is a museum complex that contains the Gemäldegalerie Alte Meister (Old Masters Picture Gallery), the Dresden Porcelain Collection (Porzellansammlung) and the Mathematisch-Physikalischer Salon (Royal Cabinet of Mathematical and Physical Instruments).\r\n"
				+ "\r\n" + "";

		files[4] = "Dresden Frauenkirche  <HEADER>  The Dresden Frauenkirche (German: Dresdner Frauenkirche, IPA: [ˈfʁaʊənˌkɪʁçə], Church of Our Lady) is a Lutheran church in Dresden, the capital of the German state of Saxony. An earlier church building was Roman Catholic until it became Protestant during the Reformation, and was replaced in the 18th century by a larger Baroque Lutheran building. It is considered an outstanding example of Protestant sacred architecture, featuring one of the largest domes in Europe. It now also serves as a symbol of reconciliation between former warring enemies. Built in the 18th century, the church was destroyed in the bombing of Dresden during World War II. The remaining ruins were left for 50 years as a war memorial, following decisions of local East German leaders. The church was rebuilt after the reunification of Germany, starting in 1994. The reconstruction of its exterior was completed in 2004, and the interior in 2005. The church was reconsecrated on 30 October 2005 with festive services lasting through the Protestant observance of Reformation Day on 31 October. The surrounding Neumarkt square with its many valuable baroque buildings was also reconstructed in 2004. The Frauenkirche is often called a cathedral, but it is not the seat of a bishop; the church of the Landesbischof of the Evangelical-Lutheran Church of Saxony is the Church of the Cross. Once a month, an Anglican Evensong is held in English, by clergy from St. George's Anglican Church, Berlin.\r\n"
				+ "\r\n" + "";
		files[5] = "Staatskapelle Dresden <HEADER> The Staatskapelle Dresden (officially known in German as the Sächsische Staatskapelle Dresden) is an orchestra based in Dresden, Germany founded in 1548 by Maurice, Elector of Saxony. It is one of the world's oldest orchestras. The precursor ensemble was Die Kurfürstlich-Sächsische und Königlich-Polnische Kapelle (The Saxony Elector and Royal Polish Band). The orchestra is the musical body of the Sächsische Staatsoper (Saxon State Opera). Venue of the orchestra is the Semperoper opera house.";
		files[6] = "Dresden Cathedral <HEADER> Dresden Cathedral, or the Cathedral of the Holy Trinity, Dresden, previously the Catholic Church of the Royal Court of Saxony, called in German Katholische Hofkirche and since 1980 also known as Kathedrale Sanctissimae Trinitatis, is the Roman Catholic Cathedral of Dresden. Always the most important Catholic church of the city, it was elevated to the status of cathedral of the Roman Catholic Diocese of Dresden-Meissen in 1964. It is located near the Elbe River in the historic center of Dresden, Germany.";
		files[7] = "Stadion Dresden <HEADER> The DDV-Stadion is a football stadium in Dresden, Saxony. It is the current home of Dynamo Dresden. The facility was previously known as the Rudolf-Harbig-Stadion (from 1951 to 1971 and from 1990 to 2010) and the Dynamo-Stadion (from 1971 to 1990). In December 2010, the naming rights were sold for 5 years to Bavarian energy company Goldgas which wanted to promote its Glücksgas brandname. Sports facilities have existed on the site of the stadium since 1874. The stadium also hosts events other than soccer games and has hosted several home games of the Dresden Monarchs American Football team of the German Football League, including their lone home appearance in the BIG6 European Football League in 2014.";
		files[8] = "Dresden Castle <HEADER> Dresden Castle or Royal Palace (German: Dresdner Residenzschloss or Dresdner Schloss) is one of the oldest buildings in Dresden. For almost 400 years, it has been the residence of the electors (1547–1806) and kings (1806–1918) of Saxony of the Albertine line of the House of Wettin. It is known for the different architectural styles employed, from Baroque to Neo-renaissance. Today, the residential castle is a museum complex that contains the Historic and New Green Vault, the Numismatic Cabinet, the Collection of Prints, Drawings and Photographs and the Dresden Armory with the Turkish Chamber. It also houses an art library and the management of the Dresden State Art Collections.";
		files[9] = "Dresden Hauptbahnhof <HEADER> Dresden Hauptbahnhof (“main station”, abbreviated Dresden Hbf) is the largest passenger station in the Saxon capital of Dresden. In 1898, it replaced the Böhmischen Bahnhof (\"Bohemian station\") of the former Saxon-Bohemian State Railway (Sächsisch-Böhmische Staatseisenbahn), and was designed with its formal layout as the central station of the city. The combination of a station building on an island between the tracks and a terminal station on two different levels is unique. The building is notable for its halls that are roofed with Teflon-coated glass fibre membranes. This translucent roof design, installed during the comprehensive rehabilitation of the station at the beginning of the 21st century, allows more daylight to reach the concourses than was previously possible. The station is connected by the Dresden railway node to the tracks of the Děčín–Dresden-Neustadt railway and the Dresden–Werdau railway (Saxon-Franconian trunk line), allowing traffic to run to the southeast towards Prague, Vienna and on to south-eastern Europe or to the southwest towards Chemnitz and Nuremberg. The connection of the routes to the north (Berlin), northwest (Leipzig) and east (Görlitz) does not take place at the station, but north of Dresden-Neustadt station (at least for passenger trains).";
		files[10] = "Dresden Armory <HEADER> The Dresden Armory or Dresden Armoury (German: Rüstkammer), also known as the Dresden Historical Museum (German: Historisches Museum Dresden), is one of the world's largest collections of ceremonial weapons, armors and historical textiles. It is part of the Dresden State Art Collections and is located in Dresden Castle in Dresden. The Turkish Chamber (German: Türckische Cammer) is a separate collection within the Dresden Armory that is focused on art from the Ottoman Empire.";
		files[11] = "Gabriel Dresden <HEADER> Gabriel & Dresden is the project name of the American DJs and producers Josh Gabriel and Dave Dresden from San Francisco. They collaborated from 2001 to 2008, then again from 2011 to the present. During that time, they have created numerous hit songs and remixes, some of which are considered all-time classics. They won the coveted Winter Music Conference IDMA award for \"Best American DJ\" twice, in 2007 and 2008, the latter time on the same day that the group split up. The pair reunited in early 2011 and proceeded on a reunion tour, which began at Ruby Skye in San Francisco on March 4, 2011.";
		files[12] = "Harry Dresden <HEADER> Harry Blackstone Copperfield Dresden is a fictional detective and wizard. He was created by Jim Butcher and is the protagonist of the contemporary fantasy series The Dresden Files. The series blends magic and hardboiled detective fiction. In addition to the fifteen The Dresden Files novels, he has appeared in fifteen short stories, as well as a limited series comic and an unlimited series comic. He was also adapted into a character by the same name for the TV series version of the novel series, also called The Dresden Files";
		files[13] = "Dresden Philharmonic <HEADER> The Dresdner Philharmonie (unofficial English translation: Dresden Philharmonic) is a symphony orchestra based in Dresden, Germany. Its principal concert venue is the Kulturpalast. The orchestra also performs at the Kreuzkirche, the Hochschule für Musik Dresden, and the Schloss Albrechtsberg. It receives financial support from the city of Dresden. The choral ensembles affiliated with the orchestra are the Dresden Philharmonic Choir and Dresden Philharmonic Chamber Choir. The orchestra was founded in 1870 and gave its first concert in the Gewerbehaussaal on 29 November 1870, under the name Gewerbehausorchester. The orchestra acquired its current name in 1915. During the existence of the DDR, the orchestra took up its primary residence in the Kulturpalast. After German reunification, plans had been proposed for a new concert hall. These had not come to fruition by the time of the principal conductorship of Marek Janowski, who cited this lack of development of a new hall for the orchestra as the reason for his resignation from the post in 2004. The orchestra's current principal conductor is Michael Sanderling, since 2011. His initial contract was for three years. In October 2013, the orchestra announced the extension of Sanderling's contract as principal conductor through the 2018-2019 season.\r\n"
				+ "\r\n" + "";
		files[14] = "Bezirk Dresden <HEADER> The Bezirk Dresden was a district (Bezirk) of East Germany. The administrative seat and the main town was Dresden.";
		files[15] = "Kreuzkirche Dresden <HEADER> The Dresden Kreuzkirche (Church of the Holy Cross) is a Lutheran church in Dresden, Germany. It is the main church and seat of the Landesbischof of the Evangelical-Lutheran Church of Saxony, and the largest church building in the Free State of Saxony. It also is home of the Dresdner Kreuzchor boys' choir.";
		files[16] = "Dresden Ontario <HEADER> Dresden is an agricultural community in southwestern Ontario, Canada, part of the municipality of Chatham-Kent. Dresden is best known as the home of Josiah Henson, the former U.S. slave whose life story was the inspiration for the novel Uncle Tom's Cabin. It has been, therefore, characterized as the \"Terminus of the Underground Railroad\", although many escaped slaves were known to gather, at least to worship, as far south and east as what is today Chatham, Ontario. The Henson homestead is a historic site located near what is today the town of Dresden, and is owned and operated by the Ontario Heritage Trust. Dresden is located on the Sydenham River. The community is named after Dresden, Germany. The major crops in the area are wheat, soybeans, corn and tomatoes.";

		files[17] = "Dresden Region <HEADER> Dresden is one of the three former Direktionsbezirke of the Free State of Saxony, Germany, located in the east of the state. It coincided with the Planungsregionen Oberlausitz-Niederschlesien and Oberes Elbtal/Osterzgebirge. It was disbanded in March 2012.";

		files[18] = "Neumarkt Dresden <HEADER> The Neumarkt in Dresden is a central and culturally significant section of the Dresden inner city. The historic area was almost completely wiped out during the Allied bomb attack during the Second World War. After the war Dresden fell under Soviet occupation and later the communist German Democratic Republic who rebuilt the Neumarkt area in socialist realist style and partially with historic buildings. However huge areas and parcels of the place remained untilled. After the fall of Communism and German reunification the decision was made to restore the Neumarkt to its pre-war look.\r\n"
				+ "\r\n" + "";
		files[19] = "Dresden Maine <HEADER> Dresden is a town in Lincoln County, Maine, United States that was incorporated in 1794. The population was 1,672 at the 2010 census.";
		files[20] = "Dresden Basin <HEADER> The Dresden Basin (German: (Dresdner) Elbtalkessel or Dresdner Elbtalweitung) is a roughly 45 km long and 10 km wide area of the Elbe Valley between the towns of Pirna and Meißen. The city of Dresden lies in the Dresden Basin.";

		files[21] = "Dresden Codak <HEADER> Dresden Codak is a webcomic written and illustrated by Aaron Diaz. Described by Diaz as a \"celebration of science, death and human folly\", the comic presents stories that deal with elements of philosophy, science and technology, and/or psychology. The comic was recognized in 2008 at the Web Cartoonist's Choice Awards for Outstanding Use of Color and Outstanding Use of The Medium. On October 22, 2008, Dresden Codak concluded a long-running sequence called \"Hob\", which focused on the character Kimiko's discovery of a post-Singularity robot and its attempted recovery by people from a future in which Earth was destroyed in a war with the artificial intelligence that once tended the planet. On February 25th, 2013, Aaron Diaz launched a Kickstarter campaign to raise funds for a hard cover book edition of the webcomic. Dubbed The Tomorrow Girl: Dresden Codak Volume 1, it collected the first 5 years of the webcomic plus additional art and reformatted everything to fit printed media. The campaign reached its original goal of $30,000 in less than 24 hours and ended with a total of $534,994.";

		files[22] = "Dresden Monarchs <HEADER> The Dresden Monarchs are an American football team from Dresden, Germany. They have been a member of the first tier German Football League since 2002 and play in its Northern Division. As its greatest success, the club reached the German Bowl for the first time in 2013, where it lost to the Braunschweig Lions by a point";

		files[23] = "Dresden Ohio <HEADER> Dresden is a village in Jefferson and Cass townships in Muskingum County, Ohio, United States, along the Muskingum River at the mouth of Wakatomika Creek. It was incorporated on March 9, 1835. The population was 1,529 at the 2010 census.\r\n"
				+ "\r\n" + "";

		files[24] = "Dresden Zoo <HEADER> Dresden Zoo, or Zoo Dresden, is a zoo situated in the city of Dresden in Germany. It was opened in 1861, making it Germany's fourth oldest zoo. It was originally designed by Peter Joseph Lenné. The zoo is located on the southern edge of the Großer Garten (Great Garden), a large city centre park. The zoo houses about 3000 animals of almost 400 species, especially Asian animals. It is a member of the World Association of Zoos and Aquariums (WAZA) and the European Association of Zoos and Aquaria (EAZA). The zoo is served on its southern side by tram lines 9 and 13 of the Dresdner Verkehrsbetriebe, the local municipal transport company. On its northern side is the Zoo station of the Dresdner Parkeisenbahn, a minimum gauge railway through the Großer Garten that is largely operated by children.\r\n"
				+ "\r\n" + "";
		String findStr = "Dresden Hauptbahnhof";

		System.out.println("scoring started");
		BM25FScoring bm25fScoring = new BM25FScoring(files, findStr);
		 System.out.println(bm25fScoring.performRanking());

		// bm25fScoring.isSameEntity("Dortmund", "Paderborn");
		String[] tests = new String[] { "Paderborn Railway Station", "Detroit Red Wings", "HP", "Bachelor of Arts",
				"Berlin airport", "Manchester United", "Cologne Church", "Angela Merkel", "frankfurt hauptbahnhof" };
		for (String item : tests) {
			// bm25fScoring.doTagging(
			// bm25fScoring.getModel(
			// "/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/ner-model.ser.gz"),
			// item);
			//bm25fScoring.isSameEntity(item, item);
		}

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
