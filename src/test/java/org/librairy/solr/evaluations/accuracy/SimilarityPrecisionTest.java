package org.librairy.solr.evaluations.accuracy;

import cc.mallet.util.Maths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Doubles;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
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
import org.librairy.solr.filter.TopicWordsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class SimilarityPrecisionTest {

    private static final Logger LOG = LoggerFactory.getLogger(SimilarityPrecisionTest.class);

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

        HashMap<String, List<Double>> documents = new HashMap<String,List<Double>>();

        BufferedReader bufReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("src/test/resources/corpus.json.gz"))));
        ObjectMapper jsonMapper = new ObjectMapper();
        String json;
        while((json = bufReader.readLine()) != null) {

            org.librairy.solr.model.Document jsonDoc = jsonMapper.readValue(json, org.librairy.solr.model.Document.class);
            documents.put(jsonDoc.getId(), jsonDoc.getShape());

        }

        MoreLikeThis mlt = new MoreLikeThis(reader); // Pass the index reader
        mlt.setFieldNames(new String[] {"about"}); // specify the fields for similarity
        mlt.setAnalyzer(myAnalyzer);
        mlt.setMinDocFreq(0);
        mlt.setMinTermFreq(0);
        mlt.setBoost(true);
//        mlt.setSimilarity(new TermFreqSimilarity());
        mlt.setStopWords(Collections.emptySet());

        int docId = 0; //"5286b3cf34b2ae102e8b6e47"
        Query query = mlt.like(docId); // Pass the doc id
        Document refDoc = searcher.doc(docId);
        LOG.info("Searching similar docs to '" + refDoc.get("name")+"'["+refDoc.get("id")+"]:");
        TopDocs topDocs = searcher.search(query, 10);


        for (ScoreDoc sd : topDocs.scoreDocs)
        {
            Document d = searcher.doc(sd.doc);

            Double similarity = 1 - Maths.jensenShannonDivergence(Doubles.toArray(documents.get(refDoc.get("id"))), Doubles.toArray(documents.get(d.get("id"))));

            LOG.info("[DocId:"+sd.doc+" - Score:"+sd.score+"] '"+String.format(d.get("id")) + "'\t ("+ String.format(d.get("type")) + ") \t- " + String.format(d.get("name")) + "\t sim: " + similarity);
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

    private static Document createDocument(org.librairy.solr.model.Document doc)
    {
        Document document = new Document();
        document.add(new TextField("id", doc.getId() , Field.Store.YES)); //StringField
        document.add(new TextField("name", doc.getName() , Field.Store.YES));
        document.add(new TextField("type", doc.getType() , Field.Store.YES));

        FieldType topicFieldType = new FieldType(TextField.TYPE_STORED);
        topicFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        topicFieldType.setStoreTermVectors(true);
        topicFieldType.setOmitNorms(true);

        String docTopic = TopicWordsFactory.toSortedText(0.0, 1000.0, doc.getShape(), "m1");
        document.add(new Field("about",  docTopic,  topicFieldType));
//        document.add(new TextField("about", docTopic , Field.Store.YES));



        return document;
    }

    private static void createIndex(Directory dir) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(myAnalyzer);
//        Similarity mySimilarity = new TermFreqSimilarity();
//        config.setSimilarity(mySimilarity);

        IndexWriter writer = new IndexWriter(dir, config);

        writer.deleteAll();

        Instant start = Instant.now();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("src/main/resources/corpus.json.gz"))));
        ObjectMapper jsonMapper = new ObjectMapper();
        String json;
        AtomicInteger counter = new AtomicInteger(1);
        while((json = reader.readLine()) != null) {

            if (counter.getAndIncrement() % 100 == 0) break;

            org.librairy.solr.model.Document jsonDoc = jsonMapper.readValue(json, org.librairy.solr.model.Document.class);
            writer.addDocument(createDocument(jsonDoc));

        }
        LOG.info("index " + (counter.get()-1) + " docs");
        reader.close();
        writer.close();
        Instant end = Instant.now();
        LOG.info("Index created in: " + ChronoUnit.MINUTES.between(start,end) + "min " + (ChronoUnit.SECONDS.between(start,end)%60) + "secs");
    }

}
