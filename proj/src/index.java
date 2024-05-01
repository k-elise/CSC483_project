import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
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
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;

import org.apache.lucene.store.ByteBuffersDirectory;

public class index {

	Analyzer analyzer = new EnglishAnalyzer();
	Directory index = new ByteBuffersDirectory();
	IndexWriterConfig config = new IndexWriterConfig(analyzer);

	// config.setSimilarity(new BM25Similarity());
//	String inputFilePath ="wiki-example.txt";
	String inputFilePath = "wikiFiles/enwiki-20140602-pages-articles.xml-0006.txt";
	boolean indexExists = false;
	String cat = "";
	boolean referenceing = false;

	private void buildIndex() {
		// Get file from resources folder
		String title = "";
		boolean firstTitleFound = false;
		String content = "";
		ClassLoader classLoader = getClass().getClassLoader();
		config.setSimilarity(new ClassicSimilarity());
		File[] files = new File(classLoader.getResource("wikiFiles").getFile()).listFiles();
		IndexWriter w = null;

		try {
			w = new IndexWriter(index, config);
		} catch (IOException e) {
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

							content = removeStopWords(content);
							cat = removeStopWords(cat);

							addDoc(w, content, title, cat); // create the document

							title = line.substring(line.indexOf("[[") + 2, line.indexOf("]")); // grab the new title

							firstTitleFound = true; // ensuring the firsttitlefound is true

							// resetting the categories and content
							content = "";
							cat = "";

						} else {// else this is the FIRST title
								// save the new and FIRST title

							title = line.substring(line.indexOf("[[") + 2, line.indexOf("]"));

							firstTitleFound = true;
						}
					} else {// else its more content

						if (line.indexOf("C") == 0 && line.indexOf(":") == 10) {
							cat = line.substring(11, line.length());

						}

						content += " " + line;

					}

				}
				inputScanner.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			addDoc(w, content, title, cat); // if reach end of line - add document
			cat = "";
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		indexExists = true;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Hello.");
		index ind = new index();
		int i = 0;
		String category = "";
		String content = "";
		String answer = "";
		int total = 0;

		double sum = 0;

		try (Scanner inputScanner = new Scanner(new File("questions.txt"))) {

			while (inputScanner.hasNextLine()) {
				String line = inputScanner.nextLine();

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
					// tokenizing, removing stopwords on the content and queries

					content = removeStopWords(content);
					category = category.split("\\(")[0];// removing the (alex: ... ) part of the categories
					category = removeStopWords(category);
					System.out.println("RUNNING QUERY " + total);
					double curRank = ind.runner(category, content, answer);
					if (curRank != 0) {// taking the reciprocal of the current rank ( 0 stays as 0 )
						curRank = 1 / curRank;
					}

					sum = curRank + sum;// adding reciprocal to the sum
					total += 1;// counting the number of queries
				}

			}
			// calculating the last querie as the loop gets stuck on 99

			content = removeStopWords(content);
			category = category.split("\\(")[0];// removing the (alex: ... ) part of the categories
			category = removeStopWords(category);
			System.out.println("RUNNING QUERY " + total);
			double curRank = ind.runner(category, content, answer);
			if (curRank != 0) {// taking the reciprocal of the current rank ( 0 stays as 0 )
				curRank = 1 / curRank;
			}

			sum = curRank + sum;// adding reciprocal to the sum
			total += 1;// counting the number of queries

		}
		System.out.println("MRR SCORE: " + (double) (sum / total));// printing out the MRR , reciprocal sum / number of
																	// queries

	}

	public int runner(String category, String content, String answer)
			throws java.io.FileNotFoundException, java.io.IOException {
		content = content + " " + category;
		String querystr = "content: " + content;// query searching the content and category

		IndexReader reader = null;
		IndexSearcher searcher = null;
		ScoreDoc[] hits = null;

		int rank = 0;// setting the base rank if not found then its 0
		if (!indexExists) {

			buildIndex();
		}

		try {

			Query q = new QueryParser("content", analyzer).parse(querystr);
			int hitsPerPage = 10;// getting top 10 hits for the query
			reader = DirectoryReader.open(index);
			searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(q, hitsPerPage);
			searcher.setSimilarity(new ClassicSimilarity());
			hits = docs.scoreDocs;

			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				System.out.println((i + 1) + ". " + d.get("title")); // + "\t" + hits[i].score);
				if (answer.split("\\|").length > 0) {

					for (String s : answer.split("\\|")) {
						if (d.get("title").trim().equals(s.trim())) {// if the document in the top 10 is the right
																		// answer

							rank = i + 1;// its rank is its position +1 ( base 1 indexing)
							break;
						}
					}
				} else if (d.get("title").trim().equals(answer.trim())) {// if the document in the top 10 is the right
																			// answer

					rank = i + 1;// its rank is its position +1 ( base 1 indexing)

				}
			}

			System.out.println("Answer is :" + answer + " Rank is :" + rank); // printing out the answer to the query
																				// and the rank it was at in the doc
			return rank;

		} catch (ParseException e) {
			e.printStackTrace();

		}
		System.out.println("Answer is :" + answer + " Rank is :" + rank);
		return rank;
	}

	private static void addDoc(IndexWriter w, String content, String title, String category) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("content", content, Field.Store.YES));
		doc.add(new TextField("category", category, Field.Store.YES));
		// use a string field for title because we don't want it tokenized
		doc.add(new StringField("title", title, Field.Store.YES));

		w.addDocument(doc);
	}

	public static String removeStopWords(String textFile) throws Exception {
		String answer = "";
		CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();

		Analyzer analyzer = new EnglishAnalyzer(stopWords);
		TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(textFile));
		tokenStream.reset();

		while (tokenStream.incrementToken()) {
			String terms = (tokenStream.getAttribute(CharTermAttribute.class).toString());
			answer += terms + " ";
		}
		tokenStream.close();
		analyzer.close();
		return answer;
	}

}
