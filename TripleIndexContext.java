package org.aksw.agdistis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import smile.math.Math;

public class TripleIndexContext {

	private static final Version LUCENE44 = Version.LUCENE_44;

	private org.slf4j.Logger log = LoggerFactory.getLogger(TripleIndexContext.class);

	public static final String FIELD_NAME_CONTEXT = "CONTEXT";
	public static final String FIELD_NAME_SURFACE_FORM = "SURFACE_FORM";
	public static final String FIELD_NAME_URI = "URI";
	public static final String FIELD_NAME_URI_COUNT = "URI_COUNT";

	private int defaultMaxNumberOfDocsRetrievedFromIndex = 5;

	private Directory directory;
	private IndexSearcher isearcher;
	private DirectoryReader ireader;
	private Cache<BooleanQuery, List<Triple>> cache;

	public TripleIndexContext() throws IOException {
		Properties prop = new Properties();
		InputStream input = TripleIndexContext.class.getResourceAsStream("/config/agdistis.properties");
		prop.load(input);

		String envIndex = System.getenv("AGDISTIS_INDEX_BY_CONTEXT");
		String index = envIndex != null ? envIndex : prop.getProperty("index");
		log.info("The index will be here: " + index);
		System.out.println("The index will be here: " + index);

		
		//File file = new File(pathname)
		directory = new MMapDirectory(new File(index));
		ireader = DirectoryReader.open(directory);
		isearcher = new IndexSearcher(ireader);
		new UrlValidator();

		cache = CacheBuilder.newBuilder().maximumSize(50000).build();
	}

	public List<Triple> search(String subject, String predicate, String object) {
		return search(subject, predicate, object, defaultMaxNumberOfDocsRetrievedFromIndex);
	}

	public List<Triple> search(String subject, String predicate, String object, int maxNumberOfResults) {
		BooleanQuery bq = new BooleanQuery();
		List<Triple> triples = new ArrayList<Triple>();
		try {
			if (subject != null && subject.equals("http://aksw.org/notInWiki")) {
				log.error(
						"A subject 'http://aksw.org/notInWiki' is searched in the index. That is strange and should not happen");
			}
			if (subject != null) {
				Query q = null;
				Analyzer analyzer = new LiteralAnalyzer(LUCENE44);
				QueryParser parser = new QueryParser(LUCENE44, FIELD_NAME_CONTEXT, analyzer);
				parser.setDefaultOperator(QueryParser.Operator.AND);
				q = parser.parse(QueryParserBase.escape(subject));
				bq.add(q, BooleanClause.Occur.MUST);
			}
			if (predicate != null) {

				TermQuery tq = new TermQuery(new Term(FIELD_NAME_SURFACE_FORM, predicate));
				bq.add(tq, BooleanClause.Occur.SHOULD);
			}
			if (object != null) {
				TermQuery tq = new TermQuery(new Term(FIELD_NAME_URI_COUNT, object));
				bq.add(tq, BooleanClause.Occur.MUST);
			}
			// use the cache
			if (null == (triples = cache.getIfPresent(bq))) {
				triples = getFromIndex(maxNumberOfResults, bq);
				if (triples == null) {
					return new ArrayList<Triple>();
				}
				cache.put(bq, triples);
			}

		} catch (Exception e) {
			log.error(e.getLocalizedMessage() + " -> " + subject);

		}
		return triples;
	}

	private List<Triple> getFromIndex(int maxNumberOfResults, BooleanQuery bq) throws IOException {
		 log.debug("\t start asking index by context...");
		ScoreDoc[] hits = isearcher.search(bq, null, maxNumberOfResults).scoreDocs;

		if (hits.length == 0) {
			return new ArrayList<Triple>();
		}
		List<Triple> triples = new ArrayList<Triple>();
		String s, p, o;
		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = isearcher.doc(hits[i].doc);
			s = hitDoc.get(FIELD_NAME_CONTEXT);
			p = hitDoc.get(FIELD_NAME_URI);
			o = hitDoc.get(FIELD_NAME_URI_COUNT);
			Triple triple = new Triple(s, p, o);
			triples.add(triple);
		}
		log.debug("\t finished asking index...");

		Collections.sort(triples);

		if (triples.size() < 500) {
			return triples.subList(0, triples.size());
		} else {
		return triples.subList(0, 500);
		}
	}

	public void close() throws IOException {
		ireader.close();
		directory.close();
	}

	public DirectoryReader getIreader() {
		return ireader;
	}

	/*public static void main(String[] args) throws IOException {
		TripleIndexContext context = new TripleIndexContext();
		List<Triple> list = context.search("Shah", null, null);
		System.out.println(list.size());
		for (Triple triple : list) {
			System.out.println(triple);
		}
		*/
	public static void main(String[] args) throws IOException {
		TripleIndexContext context = new TripleIndexContext();
		List<Triple> list = context.search("dresden", null, null);
		int D=list.size();//no. of document
		System.out.println(D);
		int i = 0;
		int avls = 0;
		int totalLength = 0;
		for (Triple triple : list) {
			String file = "";
			file = triple.getObject() + triple.getSubject() + triple.getPredicate();
			System.out.println("length of docx " + i + " is " + file.split(" ").length);
			System.out.println(file);
			totalLength += file.split(" ").length;
			i++;
			//System.out.println("file contains the:"+ file.contains("the"));
		
		}
		avls = totalLength / 5; //average length of document
		System.out.println("avls is : " + avls);
		i = 0;
		for (Triple triple : list) {
			String file = "";
			file = triple.getObject() + triple.getSubject() + triple.getPredicate();
			int ls = file.split(" ").length; //no. of tokens in a document
			double bs = 0.75;//tunable parameter
			double Bs = (double) (1 - bs) + (bs * (ls / avls)); //normalization factor
			System.out.println("Bs for docx " + i +" "+ Bs);
			//tfsi=term frequency of query per document
			String findStr="Dresden";
			int tfsi=StringUtils.countMatches(file, findStr);
			System.out.println("frequency dresden:"+tfsi);
			//double totaltfi= 0;
			double tfi=tfsi/Bs;//Vs= 1 here as s is number of type in a text, for our case, it is 1
			//totaltfi=totaltfi+tfi;
			System.out.println("weighted frequency:"+ tfi);
			//System.out.println("total weighted frequency:"+ totaltfi);
			int ni=5; //(ni is the number of documents i occurs in)
			double wiIDF=Math.log((D-ni+0.5)/(ni+0.5)+1);//wiIDF is inverse document frequency
			System.out.println(wiIDF);
			//now comes the sigmoid function :p
			double k1= 1.5;
			double wiBM25F= (tfi/(k1+tfi))*wiIDF;
			System.out.println("Score:"+ wiBM25F);
			
			i++;
		}
		
		
	
	

	}}
	

	



