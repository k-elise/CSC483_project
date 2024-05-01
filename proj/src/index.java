import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;

import org.apache.lucene.store.ByteBuffersDirectory;

public class index {

	Analyzer analyzer = new  EnglishAnalyzer();
	Directory index = new ByteBuffersDirectory();
	IndexWriterConfig config = new IndexWriterConfig(analyzer);
	
	//config.setSimilarity(new BM25Similarity());
//	String inputFilePath ="wiki-example.txt";
	String inputFilePath = "wikiFiles/enwiki-20140602-pages-articles.xml-0006.txt";
	boolean indexExists = false;
	String cat = "";

	private void buildIndex() {
		// Get file from resources folder
		String title = "";
		boolean firstTitleFound = false;
		String content = "";
		ClassLoader classLoader = getClass().getClassLoader();
		config.setSimilarity(new BM25Similarity());
		// File file = new File(classLoader.getResource(inputFilePath).getFile());
		File[] files = new File(classLoader.getResource("wikiFiles").getFile()).listFiles();
		IndexWriter w = null;

		try {
			w = new IndexWriter(index, config);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < files.length; i++) {
			try (Scanner inputScanner = new Scanner(files[i])) {
				System.out.println("working on file " + i + ": " + files[i]);
				while (inputScanner.hasNextLine()) {
					String line = inputScanner.nextLine();

					// Errors with parsing, initially trying to go by "[["" then grabbing index of
					// "]]" but when "[[" existed and "]]" existed on lines that were not titles
					// error occurred
					if (line.indexOf("[[") == 0 && line.indexOf("]]") >= 0) {// if its the title
						if (firstTitleFound) {
							// try {
							// System.out.println("categories: "+cat);
							//content = removeStopWords(content);
//							content = content.toLowerCase();
//							cat = cat.toLowerCase();
					//		System.out.println(content);
							content = removeStopWords(tokenizeString(analyzer, content));
							cat = removeStopWords((tokenizeString(analyzer, cat)));
//							System.out.println("-------------------------------------------------");
//							System.out.println(content);
							addDoc(w, content, title, cat);
						
							title = line.substring(line.indexOf("[[") + 2, line.indexOf("]"));
							// }
//                    		catch(Exception e) {
//                    			System.out.println("ERROR: "+line);
//                    		}

							// System.out.println("New Title: "+title);
							// System.out.println("New Title: index : "+line.indexOf("[["));

							// reset longString

							firstTitleFound = true;
							// save new title
							// System.out.println(content);
							content = "";
							cat = "";
						
						} else {// else this is the FIRST title
								// save the new and FIRST title
//                		try {
							title = line.substring(line.indexOf("[[") + 2, line.indexOf("]"));
//                		}
//                		catch(Exception e) {
//                		
//                    			System.out.println("ERROR: "+line);
//                		}
							firstTitleFound = true;
							// System.out.println("First Title index : "+line.indexOf("[["));
							// System.out.println("First Title: "+title);
							// System.out.println(content);
						}
					} else {// else its more content
							// System.out.println("hiiii");
//						Pattern plink = Pattern.compile("==+(.*?)+==");
//						Matcher m = plink.matcher(line);
						if (line.indexOf("C") == 0 && line.indexOf(":") == 10) {
							cat = line.substring(11, line.length());
							// System.out.println(cat);
						}
//						if (m.find()) {
//							cat += ", " + m.group(1);
//						}
					
							content += " " + line;
						
					
						
					}

				}
				inputScanner.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			addDoc(w, content, title, cat);
			cat = "";
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// if reach end of line - add document

		indexExists = true;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Hello.");
		index ind = new index();
		 //ind.buildIndex();
		int i = 0;
		String category = "";
		String content = "";
		String answer = "";
		int count = 0;
		int total = 0;
	//	Analyzer analyzer = new StandardAnalyzer();
		double sum = 0;
		//System.out.println(tokenizeString(analyzer,"Pierre Cauchon, Bishop of Beauvais, presided over the trial of this woman who went up in smoke May 30, 1431"));
		try (Scanner inputScanner = new Scanner(new File("questions.txt"))) {

			while (inputScanner.hasNextLine()) {
				String line = inputScanner.nextLine();
				//System.out.println("line: " + line + " I: " + i);
				if (i == 0) {
					category = line;
					i++;
				} else if (i == 1) {
					content = line;
					i++;
				} else if (i == 2) {
					answer = line;
					i++;
				} else {// if i = 3 , which is a new line
					i = 0;
					Analyzer analyzer = new EnglishAnalyzer();
					
				//	content = removeStopWords(tokenizeString(analyzer,content));
					content = removeStopWords((tokenizeString(analyzer,content)));
					category = category.split("\\(")[0];
					category = removeStopWords(tokenizeString(analyzer,category));
					System.out.println("RUNNING QUERY "+total);
					double curRank = ind.runner(category, content, answer);
					if(curRank!= 0) {
						curRank = 1/curRank;
					}
					
					sum = curRank+sum;
					total += 1;
				}

			}
		}
		System.out.println("MRR SCORE: "+(double)(sum/total));
			//System.out.println("GOT "+sum+" OUT OF "+total);
	}

	public int runner(String category, String content, String answer)
			throws java.io.FileNotFoundException, java.io.IOException {
	String querystr = "content: " + content + " category: " + category;
		//String querystr = "(content: " + content + " category: " + category+")^2.0f OR content: " + content ;
		IndexReader reader = null;
		IndexSearcher searcher = null;
		ScoreDoc[] hits = null;
	
		int rank =0;
		if (!indexExists) {

			buildIndex();
		}

		try {

			Query q = new QueryParser("content", analyzer).parse(querystr);
			int hitsPerPage = 10;
			reader = DirectoryReader.open(index);
			searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(q, hitsPerPage);
			searcher.setSimilarity(new BM25Similarity());
			hits = docs.scoreDocs;

			// 4. display results
			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				System.out.println((i + 1) + ". " + d.get("title")); // + "\t" + hits[i].score);
				if(d.get("title").trim().equals(answer.trim())) {
					
						rank = i+1;
					
				}
			}
//			System.out.println("number 1:"+searcher.doc(hits[0].doc).get("title").trim());
//			System.out.println("answer:"+answer.trim());
//			System.out.println(searcher.doc(hits[0].doc).get("title").trim() == answer.trim());	
//			if(searcher.doc(hits[0].doc).get("title").trim() == answer.trim()) {
//				rank = 1;
//			}
				System.out.println("Answer is :"+answer+" Rank is :"+rank);
			return rank;

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		System.out.println("Answer is :"+answer+" Rank is :"+rank);
		return rank;
	}
	
	
	private static void addDoc(IndexWriter w, String content, String title, String category) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("content", content, Field.Store.YES));
		doc.add(new TextField("category", category, Field.Store.YES));
		// use a string field for isbn because we don't want it tokenized
		doc.add(new StringField("title", title, Field.Store.YES));

		w.addDocument(doc);
	}
	
	public static String tokenizeString(Analyzer analyzer, String string) {
		String result = "";
	    try {
	    	 result = "";
	      TokenStream stream  = analyzer.tokenStream(null, new StringReader(string));
	      stream.reset();
	      while (stream.incrementToken()) {
	        result+=(stream.getAttribute(CharTermAttribute.class).toString())+" ";
	      }
	      stream.close();
	    } catch (IOException e) {
	      // not thrown b/c we're using a string reader...
	      throw new RuntimeException(e);
	    }
	    
	    return result;
	  }
	
	public static String removeStopWords(String textFile) throws Exception {
		String answer = "";
	    CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
	   // TokenStream tokenStream = new StandardTokenizer();
	    Analyzer analyzer = new  EnglishAnalyzer(stopWords);
	    TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(textFile));
	    CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
	    tokenStream.reset();
	 
	    while (tokenStream.incrementToken()) {
	        String terms = (tokenStream.getAttribute(CharTermAttribute.class).toString());
	        answer+= terms + " ";
	    }
	    tokenStream .close();
	    
	    return answer;
	}

	private static String getFrequency(String contents) throws IOException {
		
		Map<String, Integer> termFrequencyMap = new HashMap<>();

		
		CharArraySet stops = EnglishAnalyzer.getDefaultStopSet();
		Analyzer anl = new EnglishAnalyzer(stops);
//		TokenStream tokenStream = anl.tokenStream(null, new StringReader(contents));
//		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
//	    tokenStream.reset();
//	    
//	    while (tokenStream.incrementToken()) {
//	    	String term = charTermAttribute.toString();
//            termFrequencyMap.put(term, termFrequencyMap.getOrDefault(term, 0) + 1);
//
//	    }
		try (StandardTokenizer tokenizer = new StandardTokenizer()) {
		tokenizer.setReader(new StringReader(contents));
		tokenizer.reset();
		
		LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(tokenizer);
		CharTermAttribute charTermAttribute = lowerCaseFilter.addAttribute(CharTermAttribute.class);
		
		while (lowerCaseFilter.incrementToken()) {
		    String term = charTermAttribute.toString();
		    if (!stops.contains(term)) {
			termFrequencyMap.put(term, termFrequencyMap.getOrDefault(term, 0) + 1);
		    }
		}
		
		tokenizer.end();
		}

//        try (StandardTokenizer tokenizer = new StandardTokenizer()) {
//            tokenizer.setReader(new StringReader(contents));
//            tokenizer.reset();
//
//            LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(tokenizer);
//            CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
//
//            while (tokenizer.incrementToken()) {
//                String term = charTermAttribute.toString();
//                termFrequencyMap.put(term, termFrequencyMap.getOrDefault(term, 0) + 1);
//            }
//
//            tokenizer.end();
//        }
        
        Map<String, Integer> top10FrequentWords = termFrequencyMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, HashMap::new));
        System.out.println(top10FrequentWords);
		
        String words = "";
        
        for (Map.Entry<String, Integer> entry : top10FrequentWords.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
            words += entry.getKey() +" ";
        }
        words.trim();
        return words;
	}
	
}
	
}
