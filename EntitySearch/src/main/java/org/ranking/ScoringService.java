package org.ranking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndexContext;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/scoringService")
public class ScoringService {

	public ScoringService() {
	}

	@GET
	@Path("/text")
	@Produces(MediaType.APPLICATION_JSON)
	public String getString() {

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return writer;
	}

	// @Path("/{queryTerms}")
	// public String getDocuments(@PathParam("queryTerms") String queryTerms) throws
	// IOException {
	@GET
	@Path("/getdocuments")
	@Produces(MediaType.APPLICATION_JSON)
	public String getDocuments(@QueryParam("q") String queryTerms) throws IOException {
		ArrayList<String> fileList = new ArrayList<String>();
		TripleIndexContext context = new TripleIndexContext();

		List<Triple> list = context.search(null, queryTerms, null);
		String file = "";
		for (Triple triple : list) {
			// file = triple.getObject() + triple.getSubject() + triple.getPredicate();
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
				entityType = (entry.getKey().split("<ENTITYTYPE>")[1]); //.split("=")[0];
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
			// mapper.writeValue(stringWriter, stringWriter);
			writer = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responses);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return writer;
	}

	public static void main(String[] args) throws IOException {
	//	System.out.println(new ScoringService().getDocuments("Dresden"));
		System.out.println(new ScoringService().getString());

	}
}