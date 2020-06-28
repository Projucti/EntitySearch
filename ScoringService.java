package org.ranking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndexContext;

@Path("/scoringService")
public class ScoringService {

	public ScoringService() {

	}

	@GET
	@Path("/text")
	@Produces(MediaType.TEXT_PLAIN)
	public String getString() {
		String json = "HELLO";

		Map<String, String> elements = new HashMap();
		elements.put("Key1", "Value1");
		elements.put("Key2", "Value2");
		elements.put("Key3", "Value3");

		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		for (Map.Entry<String, String> entry : elements.entrySet())
			builder.append("'" + entry.getKey() + "' : '" + entry.getValue() + "'," + "\n");
		builder.append("}");
		json = builder.toString();
		return json;
	}

	@GET

	@Path("/{queryTerms}")

	@Produces("text/plain")
	public String getDocuments(@PathParam("queryTerms") String queryTerms) throws IOException {

		ArrayList<String> fileList = new ArrayList<String>();
		TripleIndexContext context = new TripleIndexContext();

		List<Triple> list = context.search(queryTerms, null, null);
		String file = "";
		for (Triple triple : list) {
			file = triple.getObject() + triple.getSubject() + triple.getPredicate();
			fileList.add(file);
		}
		String[] files = new String[fileList.size()];
		for (int i = 0; i < fileList.size(); i++) {
			files[i] = fileList.get(i);
		}
		BM25FScoring bm25fScoring = new BM25FScoring(files, queryTerms);
		System.out.println(bm25fScoring.performRanking());

		String json = "";
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		for (Map.Entry<String, Double> entry : bm25fScoring.performRanking().entrySet())
			builder.append("'" + entry.getKey() + "' : '" + entry.getValue() + "'," + "\n");
		builder.append("}");
		json = builder.toString();
		return json;

	}

}
