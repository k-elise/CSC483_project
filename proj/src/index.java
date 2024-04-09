import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
	String inputFilePath ="wiki-example.txt";
	boolean indexExists = false;
	private void buildIndex() {
		// Get file from resources folder
		String title = "";
		boolean firstTitleFound = false;
		String content = "";
		ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(inputFilePath).getFile());
        IndexWriter w = null ;
        
        
        try {
			w = new IndexWriter(index, config);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try (Scanner inputScanner = new Scanner(file)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
              //Errors with parsing, initially trying to go by "[["" then grabbing index of "]]" but when "[[" existed and "]]" existed on lines that were not titles error occurred
                if(line.indexOf("[[")==0&&line.indexOf("]]")>=0){// if its the title
                	if(firstTitleFound) {
                		//try {
                		addDoc(w,content,title,"");
                    		title = line.substring(line.indexOf("[[")+2,line.indexOf("]"));
                    		//}
//                    		catch(Exception e) {
//                    			System.out.println("ERROR: "+line);
//                    		}
        
                	  // System.out.println("New Title: "+title);
                		//System.out.println("New Title: index : "+line.indexOf("[["));
                		
                		// reset longString
                		
                		firstTitleFound = true;
                		// save new title
                		//System.out.println(content);
                		content = "";
                	}
                	else {// else this is the FIRST title
                		// save the new and FIRST title
//                		try {
                		title = line.substring(line.indexOf("[[")+2,line.indexOf("]"));
//                		}
//                		catch(Exception e) {
//                		
//                    			System.out.println("ERROR: "+line);
//                		}
                		firstTitleFound = true;
                		//System.out.println("First Title index : "+line.indexOf("[["));
                		//System.out.println("First Title: "+title);
                		//System.out.println(content);
                	}
                }
                else {// else its more content 
                	//System.out.println("hiiii");
                	content += " "+ line;
                }
               
            }
            inputScanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
        	addDoc(w,content,title,"");
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
		//ind.buildIndex();
		ind.runner();
	}
	
	  public void runner() throws java.io.FileNotFoundException,java.io.IOException {

	        if(!indexExists) {
	            buildIndex();
	        }
	        
	        try {
	        	String querystr = "content:hi";
				Query q = new QueryParser("content", analyzer).parse(querystr);
				int hitsPerPage = 10;
		        IndexReader reader = DirectoryReader.open(index);
		        IndexSearcher searcher = new IndexSearcher(reader);
		        TopDocs docs = searcher.search(q, hitsPerPage);
		        ScoreDoc[] hits = docs.scoreDocs;

		        // 4. display results
		        System.out.println("Found " + hits.length + " hits.");
		        for(int i=0;i<hits.length;++i) {
		            int docId = hits[i].doc;
		            Document d = searcher.doc(docId);
		            System.out.println((i + 1) + ". " + d.get("title") + "\t" + hits[i].score);
		        }
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        

	    }

	 private static void addDoc(IndexWriter w, String content, String title, String category) throws IOException {
	        Document doc = new Document();
	        doc.add(new TextField("content", content, Field.Store.YES));
	        doc.add(new TextField("category", category, Field.Store.YES));
	        // use a string field for isbn because we don't want it tokenized
	        doc.add(new StringField("title", title, Field.Store.YES));
	        
	        w.addDocument(doc);
	    }
}
