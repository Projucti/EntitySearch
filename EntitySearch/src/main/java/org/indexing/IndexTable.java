package org.indexing;

import java.util.ArrayList;

public class IndexTable {

	public static void main(String[] args) {
		String doc1 = "Shah Rukh Khan (born 2 November 1965), also known by the initialism SRK, is an Indian actor, film producer, and television personality. Referred to in the media as the \"Badshah of Bollywood\", \"King of Bollywood\" and \"King Khan\", he has appeared in more than 80 Bollywood films, and earned numerous accolades, including 14 Filmfare Awards. The Government of India has awarded him the Padma Shri, the fourth-highest Indian civilian honour, and the Government of France the Officier of the Ordre des Arts et des Lettres, the second-degree of the honour, and the Chevalier of the Legion of Honour, the fifth degree of the honour. Khan has a significant following in Asia and the Indian diaspora worldwide. In terms of audience size and income, he has been described as one of the most successful film stars in the world.[a]\n"
				+ "\n"
				+ "Khan began his career with appearances in several television series in the late 1980s. He made his Bollywood debut in 1992 with Deewana. Early in his career, Khan was recognised for portraying villainous roles in the films Darr (1993), Baazigar (1993) and Anjaam (1994). He then rose to prominence after starring in a series of romantic films, including Dilwale Dulhania Le Jayenge (1995), Dil To Pagal Hai (1997), Kuch Kuch Hota Hai (1998), Mohabbatein (2000) and Kabhi Khushi Kabhie Gham... (2001). Khan went on to earn critical acclaim for his portrayal of an alcoholic in Devdas (2002), a NASA scientist in Swades (2004), a hockey coach in Chak De! India (2007) and a man with Asperger syndrome in My Name Is Khan (2010). His highest-grossing films include the comedies Chennai Express (2013), Happy New Year (2014), Dilwale (2015), and the crime film Raees (2017). Many of his films display themes of Indian national identity and connections with diaspora communities, or gender, racial, social and religious differences and grievances.\n"
				+ "\n"
				+ "As of 2015, Khan is co-chairman of the motion picture production company Red Chillies Entertainment and its subsidiaries, and is the co-owner of the Indian Premier League cricket team Kolkata Knight Riders and the Caribbean Premier League team Trinbago Knight Riders. He is a frequent television presenter and stage show performer. The media often label him as \"Brand SRK\" because of his many endorsement and entrepreneurship ventures. Khan's philanthropic endeavours have provided health care and disaster relief, and he was honoured with UNESCO's Pyramide con Marni award in 2011 for his support of children's education and the World Economic Forum's Crystal Award in 2018 for his leadership in championing women's and children's rights in India. He regularly features in listings of the most influential people in Indian culture, and in 2008, Newsweek named him one of their fifty most powerful people in the world.";
		String doc2 = "document2 content";

		DocumentFile d1 = new DocumentFile(doc1);
		d1.setDocId(1);
// call the service to get IMP words
		String[] impWords = { "Shah_Rukh_Khan", "Indian", "France", "the_Ordre_des_Arts_et_des_Lettres", "Asia",
				"Deewana", "Darr", "Dilwale_Dulhania_Le_Jayenge", "Kuch_Kuch_Hota_Hai", "Mohabbatein", "Gham", "Devdas",
				"NASA", "Swades", "Chak_De", "India", "Asperger", "My_Name", "Chennai_Express", "New_Year", "Dilwale",
				"Raees", "Indian", "Red_Chillies_Entertainment", "Kolkata_Knight_Riders", "Trinbago_Knight_Riders",
				"UNESCO", "Marni", "Crystal_Award", "Newsweek" };

		ArrayList<Word> words = new ArrayList<Word>();
		for (int i = 0; i < impWords.length; i++) {
			Word word = new Word();
			ArrayList<Occurrence> occurences = new ArrayList<Occurrence>();
			int index = doc1.indexOf(impWords[i]);
			while (index >= 0) {
				Occurrence occurrence = new Occurrence();
				occurrence.setPos(index);
				occurrence.setDocId(d1.getDocId()); // subject to changes
				occurences.add(occurrence);
				index = doc1.indexOf(impWords[i], index + impWords[i].length());
			}
			word.setWord(impWords[i]);
			word.setOccurence(occurences);
			words.add(word);
		}

		for (int i = 0; i < words.size(); i++) {
			System.out.print(words.get(i).getWord() + "  ");
			for (int j = 0; j < words.get(i).getOccurence().size(); j++) {
				System.out.print(words.get(i).getOccurence().get(j).getPos() + "  ");
			}
			System.out.println();
		}

	}
}