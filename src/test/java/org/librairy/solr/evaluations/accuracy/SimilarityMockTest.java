package org.librairy.solr.evaluations.accuracy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.librairy.solr.metric.TermFreqSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class SimilarityMockTest {

    private static final Logger LOG = LoggerFactory.getLogger(SimilarityMockTest.class);

    private static final String INDEX_DIR = "lucene-compact-index";

    private static Analyzer myAnalyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {//, Reader reader
            Tokenizer tokenizer = new WhitespaceTokenizer();
            TokenFilter filters = new LowerCaseFilter(tokenizer);
            return new TokenStreamComponents(tokenizer, filters);
        }
    };

    private static DirectoryReader reader;
    private static IndexSearcher searcher;

    @BeforeClass
    public static void setup() throws IOException {
        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
        createIndex(dir);
        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
    }


    @Test
    public void searchById() throws ParseException, IOException {
        QueryParser qp = new QueryParser("id", myAnalyzer);
        Query idQuery = qp.parse("N57zDx3fvKc04");
        showResults(searcher.search(idQuery, 10));
    }

    @Test
    public void searchByTitle() throws ParseException, IOException {
        QueryParser qp = new QueryParser("name", myAnalyzer);
        Query firstNameQuery = qp.parse("data");
        showResults(searcher.search(firstNameQuery, 10));
    }

    @Test
    public void searchByType() throws ParseException, IOException {
        QueryParser qp = new QueryParser("type", myAnalyzer);
        Query firstNameQuery = qp.parse("chapter");
        showResults(searcher.search(firstNameQuery, 10));
    }

    @Test
    public void searchByDocument() throws ParseException, IOException {


        MoreLikeThis mlt = new MoreLikeThis(reader); // Pass the index reader
        mlt.setFieldNames(new String[] {"about"}); // specify the fields for similarityMetric
        mlt.setAnalyzer(myAnalyzer);
        mlt.setMinDocFreq(0);
        mlt.setMinTermFreq(0);
        mlt.setBoost(true);
        mlt.setSimilarity(new TermFreqSimilarity());
        mlt.setStopWords(Collections.emptySet());

        int docId = 1; //"5286b3cf34b2ae102e8b6e47"
        Query query = mlt.like(docId); // Pass the doc id
        Document refDoc = searcher.doc(docId);
        LOG.info("Searching similar docs to '" + refDoc.get("about")+"'["+refDoc.get("id")+"]:");
        TopDocs topDocs = searcher.search(query, 10);


        for (ScoreDoc sd : topDocs.scoreDocs)
        {
            Document d = searcher.doc(sd.doc);

            LOG.info("[DocId:"+sd.doc+" - Score:"+sd.score+"] '"+String.format(d.get("id")) + "'- " + String.format(d.get("about")) );
//            LOG.info("About: " + String.format(d.get("about")));
        }
    }

    private void showResults(TopDocs foundDocs) throws IOException {

        for (ScoreDoc sd : foundDocs.scoreDocs)
        {
            Document d = searcher.doc(sd.doc);
            LOG.info("[DocId:"+sd.doc+" - Score:"+sd.score+"] '"+String.format(d.get("id")) + "'\t ("+ String.format(d.get("type")) + ") \t- " + String.format(d.get("name")));
//            LOG.info("About: " + String.format(d.get("about")));
        }
        LOG.info("Total Results :: " + foundDocs.totalHits);
    }

    private static Document createDocument(String id, String name, String type, String about)
    {
        Document document = new Document();
        document.add(new TextField("id", id , Field.Store.YES)); //StringField
        document.add(new TextField("name", name , Field.Store.YES));
        document.add(new TextField("type", type , Field.Store.YES));

        FieldType topicFieldType = new FieldType(TextField.TYPE_STORED);
        topicFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        topicFieldType.setStoreTermVectors(true);
        topicFieldType.setOmitNorms(true);

        document.add(new Field("about",  about,  topicFieldType));
//        document.add(new TextField("about", about, Field.Store.YES));



        return document;
    }

    private static void createIndex(Directory dir) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(myAnalyzer);
//        Similarity mySimilarity = new TermFreqSimilarity();
//        config.setSimilarity(mySimilarity);

        IndexWriter writer = new IndexWriter(dir, config);

        writer.deleteAll();

        writer.addDocument(createDocument("1","a b c","type1","t1 t2 t3 t4"));
        writer.addDocument(createDocument("2","f b c","type1","t4 t4 t3 t3 t2 t1"));
        writer.addDocument(createDocument("3","g a c","type1","t4 t4 t2 t3 t1"));
        writer.addDocument(createDocument("4","h a c","type1","t1 t2 t3 t3 t4 t4"));

        writer.close();
    }

}
