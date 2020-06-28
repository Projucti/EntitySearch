package org.ranking;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndexContext;
//import org.aksw.agdistis.webapp.WikiDataService;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientResponse;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;

/**
 * Service class to return the BM25F scoring API Response
 *
 * @author Aman Sharma
 */

@Path("/scoringService")
public class ScoringService {

	public ScoringService() {
	}

	/*
	 * service method to return Response from DBPedia
	 */

	@GET
	@Path("/getDocumentsFromDBPedia")
	@Produces(MediaType.APPLICATION_JSON)
	public String getDocumentsFromDBPedia(@QueryParam("q") String queryTerms) throws IOException {
		ArrayList<String> fileList = new ArrayList<String>();
		TripleIndexContext context = new TripleIndexContext();

		List<Triple> list = context.search(null, queryTerms, null);
		String file = "";
		for (Triple triple : list) {
			file = triple.getPredicate() + " <HEADER> " + triple.getSubject();
			fileList.add(file);
		}
		String[] files = new String[fileList.size()];
		for (int i = 0; i < fileList.size(); i++) {
			files[i] = fileList.get(i);
		}
		BM25FScoring bm25fScoring = new BM25FScoring(files, queryTerms);
		ServiceResponse[] responses = new ServiceResponse[bm25fScoring.performRanking().size()];
		int index = 0;
		for (Map.Entry<String, Double> entry : bm25fScoring.performRanking().entrySet()) {
			String entityType = "";
			if (!entry.getKey().split("<ENTITYTYPE>")[1].equalsIgnoreCase("")
					|| entry.getKey().split("<ENTITYTYPE>")[1] != null) {
				entityType = (entry.getKey().split("<ENTITYTYPE>")[1]); // .split("=")[0];
			} else {
				entityType = "O";
			}

			responses[index] = new ServiceResponse(
					"http://dbpedia.org/resource/" + entry.getKey().split(" <HEADER> ")[0] + "",
					entry.getKey().split(" <HEADER> ")[0], entry.getValue(), entityType);

			index++;
		}

		ObjectMapper mapper = new ObjectMapper();

		String writer = null;
		try {
			writer = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responses);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer;
	}

	/*
	 * service method to return Response from wikiData. This service makes a GET
	 * request to AGDISTIS to fetch documents from index.
	 */

	@GET
	@Path("/getDocumentsFromWikiData")
	@Produces(MediaType.APPLICATION_JSON)
	public String getDocumentsFromWikiPedia(@QueryParam("q") String queryTerms) throws IOException {
		String writer = null;
		String termsArray[] = queryTerms.split(" ");
		StringBuffer designedQueryTerms = new StringBuffer();
		for (int i = 0; i < termsArray.length; i++) {
			designedQueryTerms.append(capitailizeWord(termsArray[i]));
			designedQueryTerms.append(" ");
		}
		designedQueryTerms.deleteCharAt(designedQueryTerms.length() - 1);
		System.out.println("Searched entity" + designedQueryTerms.toString());

		List<WikiDataResponse> wikiDataResponseList = new ArrayList<WikiDataResponse>();
		ArrayList<String> fileList = new ArrayList<String>();

		try {

			com.sun.jersey.api.client.Client sunClient = com.sun.jersey.api.client.Client.create();

			com.sun.jersey.api.client.WebResource webResource = sunClient.resource(
					"http://localhost:8080/AGDISTIS?entity=" + URLEncoder.encode(designedQueryTerms.toString() + ""));

			String response = webResource.get(String.class);

			String responseArray[] = response.split("\n");
			for (String responseString : responseArray) {

				if (!responseString.equalsIgnoreCase("")) {

					String wikiDataParts[] = responseString.split("\t");
					WikiDataResponse dataResponseTemp = new WikiDataResponse();
					dataResponseTemp.setDescription(wikiDataParts[0]);
					dataResponseTemp.setUrl(wikiDataParts[1]);
					dataResponseTemp.setLabel(wikiDataParts[2]);
					dataResponseTemp.setUnique_identifier(wikiDataParts[3]);

					wikiDataResponseList.add(dataResponseTemp);
				}
			}

		} catch (com.sun.jersey.api.client.UniformInterfaceException e) {

			// writer ="";
			// return writer;

		} catch (Exception e) {

			// e.printStackTrace();
			// writer ="";
			// return writer;

		}

		String file = "";
		for (WikiDataResponse response : wikiDataResponseList) {
			file = response.label + " <HEADER> " + response.description + " <URL> " + response.Url;
			fileList.add(file);
		}

		String[] files = new String[fileList.size()];
		for (int i = 0; i < fileList.size(); i++) {
			files[i] = fileList.get(i);
		}
		BM25FScoring bm25fScoring = new BM25FScoring(files, queryTerms);
		ServiceResponse[] responses = new ServiceResponse[bm25fScoring.performRanking().size()];
		int index = 0;
		for (Map.Entry<String, Double> entry : bm25fScoring.performRanking().entrySet()) {
			String entityType = "";
			if (!entry.getKey().split("<ENTITYTYPE>")[1].equalsIgnoreCase("")
					|| entry.getKey().split("<ENTITYTYPE>")[1] != null) {
				entityType = (entry.getKey().split("<ENTITYTYPE>")[1]); // .split("=")[0];
			} else {
				entityType = "O";
			}
			String URL = entry.getKey().split(" <URL> ")[1].split("<ENTITYTYPE>")[0];
			responses[index] = new ServiceResponse(URL, entry.getKey().split(" <HEADER> ")[0], entry.getValue(),
					entityType);

			index++;
		}

		ObjectMapper mapper = new ObjectMapper();

		try {
			writer = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responses);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer;
	}

	/*
	 * This is a test method to check if the service is up and running. This method
	 * is also used to mock the API response for testing.
	 */

	@GET
	@Path("/text")
	@Produces(MediaType.APPLICATION_JSON)
	public String getString(@QueryParam("q") String queryTerms) {

		Map<String, Double> elements = new HashMap<String, Double>();
		elements.put("Key1", 0.5);
		elements.put("Key2", 1.0);
		elements.put("Key3", 0.1);

		ServiceResponse[] responses = new ServiceResponse[elements.size()];
		int index = 0;
		for (Map.Entry<String, Double> entry : elements.entrySet()) {

			responses[index] = new ServiceResponse("", entry.getKey(), entry.getValue(), "ENTITYTYPE");

			index++;
		}
		ObjectMapper mapper = new ObjectMapper();

		String writer = null;
		try {
			// mapper.writeValue(stringWriter, stringWriter);
			writer = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responses);
		} catch (JsonGenerationException e) {

			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer = "[{\n" + "	\"url\": \"http:\\/\\/www.wikidata.org\\/entity\\/Q13909\"," + "\n"
				+ "	\"label\": \"Angelina Jolie\"," + "\n" + "	\"pagerank\": 1" + "\n" + "	\"entityType\": PERSON,"
				+ "\n" + "}," + "{\n" + "\"url\" : \"http://www.wikidata.org/entity/Q37539174\",\n"
				+ "\"label\" : \"Dresden\",\n" + "\"pagerank\" : 7.047880028685326,\n" + "\"entityType\" : \"CITY\"\n"
				+ "}]";
		return writer;
	}

	static String capitailizeWord(String str) {
		StringBuffer s = new StringBuffer();

		// Declare a character of space
		// To identify that the next character is the starting
		// of a new word
		char ch = ' ';
		for (int i = 0; i < str.length(); i++) {

			// If previous character is space and current
			// character is not space then it shows that
			// current letter is the starting of the word
			if (ch == ' ' && str.charAt(i) != ' ')
				s.append(Character.toUpperCase(str.charAt(i)));
			else
				s.append(str.charAt(i));
			ch = str.charAt(i);
		}

		// Return the string with trimming
		return s.toString().trim();
	}

	public static void main(String[] args) throws IOException {
		System.out.println(new ScoringService().getDocumentsFromDBPedia("Dresden"));
		System.out.println(new ScoringService().getString("dummy"));
		System.out.println(new ScoringService().getDocumentsFromWikiPedia("barack michecl obama"));

	}
}