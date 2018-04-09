package org.librairy.solr.evaluations;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.librairy.solr.TestCasePrinterRule;
import org.librairy.solr.analyzer.TopicAnalyzer;
import org.librairy.solr.factories.IndexFactory;
import org.librairy.solr.parse.DocTopicsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class SimilarityTest {

    private static final Logger LOG = LoggerFactory.getLogger(SimilarityTest.class);


    float epsylon = 0.12f;
    float epsylon_2_2sqrt = (float) (2*Math.sqrt(2*epsylon));
    float epsylon_2_2sqrt_short = (float) (epsylon_2_2sqrt*10000);
    float epsylon_cota2_2 = (float) (Math.sqrt((-72f + 24f*Math.sqrt(9+2*epsylon)))*10000);
    float epsylon20000f = epsylon*20000f;


    private static IndexReader reader;

    float[] testQueryDoctopics = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.02672956f, 0.0f, 0.0f, 0.19654088f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.045597486f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.0f, 0.0f, 0.1399371f, 0.0f, 0.0f, 0.0f, 0.0f, 0.06446541f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.2908805f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.10220126f, 0.0f, 0.0f, 0.0f};

    @Rule
    public TestCasePrinterRule pr = new TestCasePrinterRule();

    @BeforeClass
    public static void setup() throws IOException {
        reader = IndexFactory.newTopicIndex(new RAMDirectory(),ParsingTest.CORPUS_BIN);
    }

    @Test(timeout = 10000)
    public void byDefaultSimilarity() throws IOException, ParseException {
        IndexSearcher searcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(IndexFactory.FIELD_NAME, new TopicAnalyzer());
        int maxdocs = reader.maxDoc();

        // query1: lucene
        String queryString = new DocTopicsUtil().getVectorString(testQueryDoctopics, 1e3f);
        Query query = parser.parse(queryString);
        searcher.search(query, maxdocs);

        Assert.assertTrue(true);
    }


    public void byJSDivergence() throws IOException {
        // query2: JS divergence
        int directoryLength = reader.getDocCount(IndexFactory.FIELD_NAME);
        Map<Integer, Integer> testVector = getTestFrequenciesInteger(testQueryDoctopics, IndexFactory.PRECISION);
        int numtopics = testQueryDoctopics.length;

        double maxsim = 0d;
        double minsim = 1e6d;

        for (int i = 0; i < directoryLength; i++) {
            Map<Integer, Integer> termVector = getTermFrequenciesInteger(reader, i);
            //double sim = getJSSimilarity(termVector, testVector, numtopics);
            double sim = getJSSimilarity3(termVector, testVector, numtopics);
            if(sim > maxsim){
                maxsim = sim;
            }
            if(sim < minsim && sim > 0){
                minsim = sim;
            }
            //System.out.println("JS(doc["+ i + "], test) = " + sim);
        }
    }


    private Map<Integer, Integer> getTestFrequenciesInteger(float[] testVectorDoctopics, float multiplication_factor) {
        Map<Integer, Integer> frequencies = new HashMap<>();

        for(int i=0; i<testVectorDoctopics.length;i++){
            int freq = (int)(testVectorDoctopics[i]*multiplication_factor);
            if(freq > 0){
                frequencies.put(i, freq);
            }
        }
        return frequencies;
    }

    private Map<Integer, Integer> getTermFrequenciesInteger(IndexReader reader, int docId) {
        Map<Integer, Integer> frequencies = new HashMap<>();

        try {
            Terms vector = reader.getTermVector(docId, IndexFactory.FIELD_NAME);
            TermsEnum termsEnum = vector.iterator();
            BytesRef text = null;
            PostingsEnum postings = null;

            while ((text = termsEnum.next()) != null) {
                String termString = text.utf8ToString();
                postings = termsEnum.postings(postings, PostingsEnum.FREQS);
                postings.nextDoc();
                frequencies.put(Integer.parseInt(termString), postings.freq());
                //System.out.println("Term: " + termString + ", freq: " + postings.freq());
            }
            return frequencies;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frequencies;
    }

    private double getJSSimilarity3(Map<Integer, Integer> termVector1, Map<Integer, Integer> termVector2, int numtopics) {
        // lower bound 1
        int sum = 0;
        Set<Integer> activeKeySet = new HashSet<Integer>(numtopics);

        for (Map.Entry<Integer, Integer> entry : termVector1.entrySet()) {
            Integer key1  = entry.getKey();
            Integer freq1 = entry.getValue();
            activeKeySet.add(key1);

            // contenido en termVector1, termVector2
            Integer freq2 = termVector2.get(key1);
            if(freq2 !=null){
                int diff = freq1 - freq2;
                sum += (diff >= 0 ? diff : -diff);
                // contenido en termVector1 pero no en termVector2
            } else {
                sum += freq1;
            }
        }
        // contenido en termVector2 pero no en termVector1
        for (Map.Entry<Integer, Integer> entry : termVector2.entrySet()) {
            Integer key2  = entry.getKey();
            Integer freq2 = entry.getValue();
            activeKeySet.add(key2);

            Integer freq1 = termVector1.get(key2);
            if(freq1 == null){
                sum += freq2;
            }
        }

        // lower bound 2
        if(sum >= epsylon_cota2_2){//sum > 0 y  epsylon_cota2_1 negativo
            return epsylon;
        }

        float sumF = 0f;

        Iterator<Integer> iterator = activeKeySet.iterator();
        while(iterator.hasNext()){
            Integer key = iterator.next();
            Integer freq1 = termVector1.get(key);
            if(freq1 == null){
                freq1 = 0;
            }

            Integer freq2 = termVector2.get(key);
            if(freq2 == null){
                freq2 = 0;
            }
            sumF += (freq1 > 0 ? ((int)freq1) * Math.log((2f * (int)freq1) / ((int)freq1 + (int)freq2)) : 0) +
                    (freq2 > 0 ? ((int)freq2) * Math.log((2f * (int)freq2) / ((int)freq1 + (int)freq2)) : 0);
        }

        return (float) sumF/20000f;
    }

}
