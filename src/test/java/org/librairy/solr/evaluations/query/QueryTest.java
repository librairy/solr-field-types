package org.librairy.solr.evaluations.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.DelimitedTermFrequencyTokenFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;
import org.librairy.solr.analyzer.TopicAnalyzer;
import org.librairy.solr.factories.IndexFactory;
import org.librairy.solr.metric.TermFreqSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class QueryTest {

    private static final Logger LOG = LoggerFactory.getLogger(QueryTest.class);

    @Test
    public void searchByTermFrequency() throws ParseException, IOException {

//        File indexFile = new File("target/index-cleanZeroAndEpsylonLMDirichlet/");
//        if (indexFile.exists()) indexFile.delete();
//        FSDirectory directory   = FSDirectory.open(indexFile.toPath());

        Directory directory = new RAMDirectory();

        createIndex(directory);

        IndexReader reader      = DirectoryReader.open(directory);



        for(int i=0;i<reader.numDocs();i++){
            Document document = reader.document(i);
            LOG.info("Document:");
            for(IndexableField field: document.getFields()){
                LOG.info("\t " + field.name() + " = " + document.get(field.name()));
            }

            LOG.info("term vectors: ");
            Fields termVectors = reader.getTermVectors(i);
            Iterator<String> it = termVectors.iterator();
            while(it.hasNext()){
                String field = it.next();
                Terms termVector    = reader.getTermVector(i, field);
                TermsEnum termsEnum = termVector.iterator();
                BytesRef text = null;
                PostingsEnum postings = null;

                Map<String, Integer> frequencies = new HashMap<>();
                while ((text = termsEnum.next()) != null) {
                    String termString = text.utf8ToString();
                    postings = termsEnum.postings(postings, PostingsEnum.FREQS);
                    postings.nextDoc();
                    frequencies.put(termString, postings.freq());
                    //System.out.println("Term: " + termString + ", freq: " + postings.freq());
                }
                LOG.info("\t Vector: " + frequencies);
            }
        }

        QueryParser parser = new QueryParser("about", new TopicAnalyzer());
        //String topics = "24|1894 76|1328 103|2838";
        String topics = "t1|15 t2|22";

        Query query = parser.parse(topics);

        LOG.info("Query by " + query.toString());

        IndexSearcher searcher  = new IndexSearcher(reader);
        searcher.setSimilarity(new TermFreqSimilarity());

        Explanation explanation = searcher.explain(query, 10);
        LOG.info("Query explanation: '" + query + "' : " + explanation);

        TopDocs results = searcher.search(query, 100);
        LOG.info("Results:: " + results.totalHits);
        for (int i=0; i< results.totalHits; i++){
            ScoreDoc doc = results.scoreDocs[i];
            LOG.info("Doc: " + reader.document(doc.doc).get("id") + " || " +  reader.document(doc.doc).get("about") + " || " + doc.score);
        }

    }

    private static Document createDocument(String id, String about)
    {
        Document document = new Document();
        document.add(new TextField("id", id , Field.Store.YES)); //StringField

        FieldType topicFieldType = new FieldType(TextField.TYPE_STORED);
        topicFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        topicFieldType.setStoreTermVectors(true);
        topicFieldType.setStoreTermVectorOffsets(false);
        topicFieldType.setStoreTermVectorPositions(false);
        topicFieldType.setOmitNorms(true);

        document.add(new Field("about",  about,  topicFieldType));
//        document.add(new TextField("about", about, Field.Store.YES));

        return document;
    }

    private static void createIndex(Directory dir) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(new TopicAnalyzer());
//        Similarity mySimilarity = new TermFreqSimilarity();
//        config.setSimilarity(mySimilarity);

        IndexWriter writer = new IndexWriter(dir, config);


        writer.deleteAll();

        writer.addDocument(createDocument("1","t1|10 t2|5 t3|2 t4|1"));
        writer.addDocument(createDocument("2","t4|20 t3|3"));
        writer.addDocument(createDocument("3","t2|10 t1|10"));
        writer.addDocument(createDocument("4","t1|5 t2|3 t3|20"));
        writer.addDocument(createDocument("5","t5"));

        writer.close();
    }


}
