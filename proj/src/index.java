import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

import org.apache.lucene.store.ByteBuffersDirectory;

public class index {

	StandardAnalyzer analyzer = new StandardAnalyzer();
	Directory index = new ByteBuffersDirectory();
	IndexWriterConfig config = new IndexWriterConfig(analyzer);
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
						if (line.indexOf("C") == 0 && line.indexOf(":") == 10) {
							cat = line.substring(11, line.length());
							// System.out.println(cat);
						}

						content += " " + line;
					}

				}
				inputScanner.close();
			} catch (IOException e) {
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

	public static void main(String[] args) throws FileNotFoundException, IOException {
		System.out.println("Hello.");
		index ind = new index();
		// ind.buildIndex();
		int i = 0;
		String category = "";
		String content = "";
		String answer = "";
		int count = 0;
		int total = 0;

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
					Analyzer analyzer = new StandardAnalyzer();
					
					content = tokenizeString(analyzer,content);
					category = tokenizeString(analyzer,category);
					
					boolean found = ind.runner(category, content, answer);
					if (found) {
						count += 1;
					}
					total += 1;
				}

			}
		}
		System.out.println("FOUND " + count + " CORRECTLY OUT OF " + total);

	}

	public boolean runner(String category, String content, String answer)
			throws java.io.FileNotFoundException, java.io.IOException {
		String querystr = "content: " + content + " category: " + category+"^2.0f";
		IndexReader reader = null;
		IndexSearcher searcher = null;
		ScoreDoc[] hits = null;
		if (!indexExists) {

			buildIndex();
		}

		try {

			Query q = new QueryParser("content", analyzer).parse(querystr);
			int hitsPerPage = 10;
			reader = DirectoryReader.open(index);
			searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(q, hitsPerPage);
			hits = docs.scoreDocs;

			// 4. display results
			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				System.out.println((i + 1) + ". " + d.get("title")); // + "\t" + hits[i].score);
			}
//			System.out.println("number 1:"+searcher.doc(hits[0].doc).get("title").trim());
//			System.out.println("answer:"+answer.trim());
//			System.out.println(searcher.doc(hits[0].doc).get("title").trim() == answer.trim());	
			if (searcher.doc(hits[0].doc).get("title").trim().equals(answer.trim()) ){
				return true;
			}
			return false;

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		return false;
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
}
