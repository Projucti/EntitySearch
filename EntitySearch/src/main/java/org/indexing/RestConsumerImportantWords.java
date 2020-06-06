package org.indexing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.Gson;

public class RestConsumerImportantWords {

	public ImportantWords collectWords(DocumentFile fileContent) throws IOException {

		String targetUrl = "http://localhost:5003/getImpWords";
		URL url = new URL(targetUrl + "?filename=" + URLEncoder.encode(fileContent.getText(), "UTF-8"));
		System.out.println("URL called: " + url.toString());
		HttpURLConnection postConnection = (HttpURLConnection) url.openConnection();
		postConnection.setRequestMethod("POST");
		postConnection.setRequestProperty("Content-Type", "application/json");
		postConnection.setDoOutput(true);
		OutputStream os = postConnection.getOutputStream();

		os.flush();
		os.close();
		StringBuffer response = new StringBuffer();
		System.out.println("POST Response Code :  " + postConnection.getResponseCode());
		System.out.println("POST Response Message : " + postConnection.getResponseMessage());
		if (postConnection.getResponseCode() == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(postConnection.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null && inputLine != "") {
				response.append(inputLine);
			}
			in.close();

		} else {
			System.out.println("SERVICE FAILED");
		}

		char quote = '"';
		char escQuote = '\"';

		return this.jsonify(response.toString().replace(quote, escQuote));

	}

	private ImportantWords jsonify(String response) {

		Gson gson = new Gson();

		ImportantWords impWords = gson.fromJson(response, ImportantWords.class);

		return impWords;
	}

	public static void main(String[] args) throws IOException {
		String file = "Shah Rukh Khan (born 2 November 1965), also known by the initialism SRK, is an Indian actor, film producer, and television personality. Referred to in the media as the \"Badshah of Bollywood\", \"King of Bollywood\" and \"King Khan\", he has appeared in more than 80 Bollywood films, and earned numerous accolades, including 14 Filmfare Awards. The Government of India has awarded him the Padma Shri, the fourth-highest Indian civilian honour, and the Government of France the Officier of the Ordre des Arts et des Lettres, the second-degree of the honour, and the Chevalier of the Legion of Honour, the fifth degree of the honour. Khan has a significant following in Asia and the Indian diaspora worldwide. In terms of audience size and income, he has been described as one of the most successful film stars in the world.[a]\n"
				+ "\n"
				+ "Khan began his career with appearances in several television series in the late 1980s. He made his Bollywood debut in 1992 with Deewana. Early in his career, Khan was recognised for portraying villainous roles in the films Darr (1993), Baazigar (1993) and Anjaam (1994). He then rose to prominence after starring in a series of romantic films, including Dilwale Dulhania Le Jayenge (1995), Dil To Pagal Hai (1997), Kuch Kuch Hota Hai (1998), Mohabbatein (2000) and Kabhi Khushi Kabhie Gham... (2001). Khan went on to earn critical acclaim for his portrayal of an alcoholic in Devdas (2002), a NASA scientist in Swades (2004), a hockey coach in Chak De! India (2007) and a man with Asperger syndrome in My Name Is Khan (2010). His highest-grossing films include the comedies Chennai Express (2013), Happy New Year (2014), Dilwale (2015), and the crime film Raees (2017). Many of his films display themes of Indian national identity and connections with diaspora communities, or gender, racial, social and religious differences and grievances.\n"
				+ "\n"
				+ "As of 2015, Khan is co-chairman of the motion picture production company Red Chillies Entertainment and its subsidiaries, and is the co-owner of the Indian Premier League cricket team Kolkata Knight Riders and the Caribbean Premier League team Trinbago Knight Riders. He is a frequent television presenter and stage show performer. The media often label him as \"Brand SRK\" because of his many endorsement and entrepreneurship ventures. Khan's philanthropic endeavours have provided health care and disaster relief, and he was honoured with UNESCO's Pyramide con Marni award in 2011 for his support of children's education and the World Economic Forum's Crystal Award in 2018 for his leadership in championing women's and children's rights in India. He regularly features in listings of the most influential people in Indian culture, and in 2008, Newsweek named him one of their fifty most powerful people in the world.";
		DocumentFile documentFile = new DocumentFile(file);

		RestConsumerImportantWords wordsProcessor = new RestConsumerImportantWords();
		ImportantWords words = wordsProcessor.collectWords(documentFile);
		System.out.println(words);

	}
}
