package org.librairy.solr;

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
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.librairy.solr.parse.DocTopicsUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;


public class DocTopicsLuceneIndexTest {
    String doctopics_file = "src/test/resources/cordis-projects-fp1-h2020_nsf-1984-2018_120.doc_topics.bin";
    String indexdir = "target/indexdir";
    boolean createindex = false;
    boolean binformat = true;
    boolean ram_index = false;
    private static final String FIELD_NAME = "doctopics_field";

    float[][] docTopicValuesCleanned;
    short[][] docTopicValues;
    DocTopicsUtil docTopicsUtil;
    long ini, fin;
    // doc1 topic distribution, from cordis-projects-fp1-h2020_nsf-1984-2018_120.doc_topics
    // TODO obtener del primer vector
    float[] testQueryDoctopics = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.02672956f, 0.0f, 0.0f, 0.19654088f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.045597486f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.0f, 0.0f, 0.1399371f, 0.0f, 0.0f, 0.0f, 0.0f, 0.06446541f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.2908805f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.02672956f, 0.0f, 0.10220126f, 0.0f, 0.0f, 0.0f};
    float precision = 1e4f;

    //float epsylon = 0.15f;
    float epsylon = 0.12f;
    float epsylon_2_2sqrt = (float) (2*Math.sqrt(2*epsylon));
    float epsylon_2_2sqrt_short = (float) (epsylon_2_2sqrt*10000);
    float epsylon_cota2_2 = (float) (Math.sqrt((-72f + 24f*Math.sqrt(9+2*epsylon)))*10000);
    float epsylon20000f = epsylon*20000f;


    public DocTopicsLuceneIndexTest() {
        docTopicValuesCleanned = new float[1][1];
        docTopicValues = new short[1][1];
        docTopicsUtil = new DocTopicsUtil();
    }

    public static void main(String[] args) {
        DocTopicsLuceneIndexTest docTopicsLucene = new DocTopicsLuceneIndexTest();
        docTopicsLucene.run(args);
    }

    private void run(String[] args) {
        // parse args

        // -input input/cordis-projects-fp1-h2020_nsf-1984-2018_120.doc_topics -indexdir indexdir
        // -input input/cordis-projects-fp1-h2020_nsf-1984-2018_120.doc_topics.bin -indexdir indexdirbin  -bin -ram

        String usage = "Usage:\tjava DocTopicsLuceneIndexTest [-input doctopics_file] [-output indexdir] [-bin]\n";
        if (args.length > 0 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }
        for(int i = 0;i < args.length;i++) {
            if ("-input".equals(args[i])) {
                doctopics_file = args[i+1];
                i++;
            } else if ("-indexdir".equals(args[i])) {
                indexdir = args[i+1];
                i++;
            } else if ("-bin".equals(args[i])) {
                binformat = true;
            } else if ("-ram".equals(args[i])) {
                ram_index = true;
            }
        }



        // create index

        // read doc_topics file
        try {
            // complete format (Mallet standard output)
            if(!binformat){
                // Complete format: read &  clean doctopics
                // inspect file
                ini = System.currentTimeMillis();
                docTopicsUtil.inspectTopicFile_CompleteFormat(doctopics_file);
                fin = System.currentTimeMillis();
                System.out.println("\ninspectTopicFile_CompleteFormat (ms): " + (fin - ini));

                // load topics
                ini = System.currentTimeMillis();
                if(docTopicsUtil.loadTopics_CompleteFormat(doctopics_file) == 0){
                    System.out.println("Error loading doc-topics...");
                    return;
                }
                fin = System.currentTimeMillis();
                System.out.println("\nloadTopics_CompleteFormat (ms): " + (fin - ini));

                // clean zeros
                ini = System.currentTimeMillis();
                docTopicValuesCleanned = docTopicsUtil.cleanZeros();
                fin = System.currentTimeMillis();
                System.out.println("\ncleanZeros (ms): " + (fin - ini));

                //docTopicsUtil.printDocTopicValuesHeaderFloat(docTopicValuesCleanned, precision);

                // compressed doctopics binary format (Mallet standard output binarized)
            } else {
                docTopicsUtil.inspectBinTopicFile(doctopics_file);

                int numdocs = docTopicsUtil.getNumdocs();
                if(numdocs == 0){
                    System.out.println("Error loading doc-topics: incorrect numdocs...");
                    System.exit(1);
                }
                int numtopics = docTopicsUtil.getNumtopics();
                if(numtopics == 0){
                    System.out.println("Error loading doc-topics: incorrect numtopics...");
                    System.exit(1);
                }

                docTopicValues = new short[numdocs][numtopics];
                if(docTopicsUtil.loadBinTopics(doctopics_file, numdocs, docTopicValues) == 0){
                    System.out.println("Error loading bin doc-topics...");
                    return;
                }
                //docTopicValues = docTopicsUtil.normalizeDocTopics(docTopicValues, 10);

                //docTopicsUtil.printDocTopicValuesHeaderShort(docTopicValues);
            }




            // create lucene index
            Directory indexDir;
            if(ram_index){
                indexDir = new RAMDirectory();
            } else {
                indexDir = FSDirectory.open(new File(indexdir).toPath());
            }

            ini = System.currentTimeMillis();

            // TODO para usa DelimitedTermFrequencyTokenFilter mejor un mapa con los topicos activos que un array
            Analyzer myAnalyzer = new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents(String fieldName) {//, Reader reader
                    Tokenizer tokenizer = new WhitespaceTokenizer();
                    TokenFilter filters = new DelimitedTermFrequencyTokenFilter(tokenizer);
                    return new TokenStreamComponents(tokenizer, filters);
                }
            };

            IndexWriterConfig writerConfig = new IndexWriterConfig(myAnalyzer);
            writerConfig.setOpenMode(OpenMode.CREATE);
            writerConfig.setRAMBufferSizeMB(500.0);


            try (IndexWriter writer = new IndexWriter(indexDir, writerConfig)) {
                if(!binformat){
                    int maxDocs = docTopicValuesCleanned.length;
                    for(int i=0; i<maxDocs; i++){
                        // DelimitedTermFrequencyTokenFilter
                        String vectorString = docTopicsUtil.getVectorString(docTopicValuesCleanned[i], precision);
                        Document test = getTestDocument(vectorString);
                        writer.addDocument(test);
                    }
                } else {
                    int maxDocs = docTopicValues.length;
                    for(int i=0; i<maxDocs; i++){
                        String vectorString = docTopicsUtil.getVectorStringBin(docTopicValues[i]);
                        Document test = getTestDocument(vectorString);
                        writer.addDocument(test);
                    }
                }
                writer.forceMerge(1);
            }
            fin = System.currentTimeMillis();
            System.out.println("\ncreate lucene index (ms): " + (fin - ini));


            // query
            // init index reader
            IndexReader reader;
            if(ram_index){
                reader = DirectoryReader.open(indexDir);
            } else {
                reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir)));
            }
            int maxdocs = reader.maxDoc();
            System.out.println("Num index docs: " + maxdocs);

            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser(FIELD_NAME, myAnalyzer);

            // query1: lucene
            ini = System.currentTimeMillis();
            String queryString = docTopicsUtil.getVectorString(testQueryDoctopics, 1e3f);
            Query query = parser.parse(queryString);

            // analyze results
            TopDocs results = searcher.search(query, maxdocs);
            //System.out.println("Results for querystring, count: " + results.totalHits);

            fin = System.currentTimeMillis();
            System.out.println("\n1. Query index lucene default similarityMetric (ms): " + (fin - ini));

            // show results
            ScoreDoc[] resultDocs = results.scoreDocs;
            System.out.println("First doc[0]:" + resultDocs[0].doc + ",\t\tscore: " + resultDocs[0].score);
            System.out.println("Last  doc["+(resultDocs.length-1)+"]:" + resultDocs[resultDocs.length-1].doc + ",\t\tscore: " + resultDocs[resultDocs.length-1].score);

//		        for(int i=0; i<resultDocs.length; i++){
//		        	ScoreDoc scoreDoc = resultDocs[i];
//		        	System.out.println("doc: " + scoreDoc.doc + ", score: " + scoreDoc.score);
//		        }

            // query2: JS divergence
            ini = System.currentTimeMillis();
            getJS_distances(reader, testQueryDoctopics);
            fin = System.currentTimeMillis();
            System.out.println("\n2. Query index JS similarityMetric (ms): " + (fin - ini));

            // query3: cosine distance
            ini = System.currentTimeMillis();
            getCosine_distances(reader, testQueryDoctopics);
            fin = System.currentTimeMillis();
            System.out.println("\n3. Query index cosine similarityMetric (ms): " + (fin - ini));

            // query4: JS divergence using inverted index
            // TODO test other lucene similarities
            searcher.setSimilarity(new BooleanSimilarity());//new LMDirichletSimilarity((float) 1)
            ini = System.currentTimeMillis();
            int num_cercanos = getJS_distances_inverted_index(reader, searcher, query, testQueryDoctopics);
            fin = System.currentTimeMillis();
            System.out.println("\n4. Query index JS similarityMetric using lucene inverted index (ms): " + (fin - ini));
            System.out.println("Num docs cercanos: "+ num_cercanos);


            // query5: JS divergence using inverted index, previa extraccion de termvectors
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            // TODO Short
            HashMap<Integer, Map<Integer,Integer>> termVectorsMap = getTermVectorsMap(maxdocs, reader);
            long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("\n5. TotalMemory: " + (int)(runtime.totalMemory()/(1024*1024)) + " Mb, freeMemory: " + (int)(runtime.freeMemory()/(1024*1024)) + " Mb, memDiff: " + (int)((usedMemoryAfter-memoryBefore)/(1024*1024)) + " Mb");

            ini = System.currentTimeMillis();
            num_cercanos = getJS_distances_inverted_index_termvectorsmap(reader, searcher, query, testQueryDoctopics, termVectorsMap);
            fin = System.currentTimeMillis();
            System.out.println("\n5. Query index JS similarityMetric using lucene inverted index with termvectors (ms): " + (fin - ini));
            System.out.println("Num docs cercanos: "+ num_cercanos);

            int num_samples = 1000;
            Random r = new Random();
            float mean_time = 0;
            float mean_num_cercanos = 0;
            long total_time = 0;
            for(int i = 1; i <= num_samples; i++){
                int random_doc_number = r.nextInt(maxdocs - 1) + 1;
                ini = System.currentTimeMillis();
                num_cercanos = getJS_distances_inverted_index_termvectorsmap_by_id(reader, searcher, random_doc_number, termVectorsMap, parser);
                fin = System.currentTimeMillis();
                mean_time = (mean_time*(i-1) + (fin-ini))/i;
                mean_num_cercanos = (mean_num_cercanos*(i-1) + num_cercanos)/i;
                total_time += fin-ini;
            }
            System.out.println("\n6. Query Stats: index JS similarityMetric using lucene inverted index with termvectors (ms): " + total_time + ", num_samples: " + num_samples + ", mean_time (ms): " + mean_time + ", mean_num_cercanos: " + mean_num_cercanos);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }



    private HashMap<Integer, Map<Integer, Integer>> getTermVectorsMap(int length, IndexReader reader) throws IOException {
        HashMap<Integer, Map<Integer,Integer>> termVectorsMap = new HashMap<Integer, Map<Integer,Integer>>(length);

        int directoryLength = reader.getDocCount(FIELD_NAME);

        for (int i = 0; i < directoryLength; i++) {
            Map<Integer, Integer> termVector = getTermFrequenciesInteger(reader, i);
            termVectorsMap.put(i, termVector);
        }
        return termVectorsMap;
    }

    //	getJS_distances_inverted_index_termvectorsmap_by_id
    private int getJS_distances_inverted_index_termvectorsmap_by_id(IndexReader reader, IndexSearcher searcher,
                                                                    int testVectorDocId, HashMap<Integer, Map<Integer, Integer>> termVectorsMap, QueryParser parser) throws IOException, ParseException {
        int maxdocs = reader.maxDoc();

        Map<Integer, Integer> testVector = termVectorsMap.get(testVectorDocId);

        //String queryString = docTopicsUtil.getVectorStringfromMap(testVector);
        String queryString = docTopicsUtil.getVectorStringfromMapReduced(testVector, epsylon_cota2_2);//*precision
        Query query = parser.parse(queryString);

        TopDocs results = searcher.search(query, maxdocs);
        ScoreDoc[] resultDocs = results.scoreDocs;

        int numtopics = docTopicsUtil.getNumtopics();

        int num_cercanos = 0;
        // solo score > 0
        for(int i=0; i<resultDocs.length; i++){
            ScoreDoc scoreDoc = resultDocs[i];
            // JSD
            Map<Integer, Integer> termVector = termVectorsMap.get(scoreDoc.doc);
            double sim = getJSSimilarity3(termVector, testVector, numtopics);

            if(sim < epsylon){
                num_cercanos++;
            }
        }
        //System.out.println("resultDocs.length: " + resultDocs.length + ", testVectorDocId: " + testVectorDocId + ", num_cercanos: " + num_cercanos);
        return num_cercanos;
    }


    //TODO Map<Integer, Integer> -> Map<Short, Short>
    private int getJS_distances_inverted_index_termvectorsmap(IndexReader reader, IndexSearcher searcher, Query query,
                                                              float[] testVectorDoctopics, HashMap<Integer, Map<Integer, Integer>> termVectorsMap) throws IOException {
        int maxdocs = reader.maxDoc();
        Explanation explanation = searcher.explain(query, maxdocs);
        TopDocs results = searcher.search(query, maxdocs);
        ScoreDoc[] resultDocs = results.scoreDocs;

        Map<Integer, Integer> testVector = getTestFrequenciesInteger(testVectorDoctopics, precision);
        int numtopics = docTopicsUtil.getNumtopics();

        double maxsim = 0d;
        double minsim = 1e6d;

        System.out.println("\n5. Query index JS similarityMetric using lucene inverted index with termvectors scoring: ");
        float score_ant = 12f;

        // solo score > 0
        int num_cercanos = 0;

        for(int i=0; i<resultDocs.length; i++){
            ScoreDoc scoreDoc = resultDocs[i];
            // JSD
            //Map<Integer, Integer> termVector = getTermFrequenciesInteger(reader, scoreDoc.doc);
            Map<Integer, Integer> termVector = termVectorsMap.get(scoreDoc.doc);
            double sim = getJSSimilarity3(termVector, testVector, numtopics);

            if(sim < epsylon){
                num_cercanos++;
            }

            // min max por scoring
            if(scoreDoc.score < score_ant){
                System.out.println("score: "+ score_ant  + ", minsim: " + minsim + ", maxsim: " + maxsim);

                score_ant = scoreDoc.score;
                maxsim = 0d;
                minsim = 1e6d;
                if(sim > maxsim){
                    maxsim = sim;
                }
                if(sim < minsim){
                    minsim = sim;
                }
            } else {
                if(sim > maxsim){
                    maxsim = sim;
                }
                if(sim < minsim){
                    minsim = sim;
                }
            }
        }
        System.out.println("score: "+ score_ant  + ", minsim: " + minsim + ", maxsim: " + maxsim);
        return num_cercanos;
    }

    private int getJS_distances_inverted_index(IndexReader reader, IndexSearcher searcher, Query query, float[] testVectorDoctopics) throws IOException {
        int maxdocs = reader.maxDoc();
        Explanation explanation = searcher.explain(query, maxdocs);
        TopDocs results = searcher.search(query, maxdocs);
        ScoreDoc[] resultDocs = results.scoreDocs;

        Map<Integer, Integer> testVector = getTestFrequenciesInteger(testVectorDoctopics, precision);
        int numtopics = docTopicsUtil.getNumtopics();

        double maxsim = 0d;
        double minsim = 1e6d;

        System.out.println("\n4. Query index JS similarityMetric using lucene inverted index scoring: ");
        float score_ant = 12f;

        // solo score > 0
        int num_cercanos = 0;

        for(int i=0; i<resultDocs.length; i++){
            ScoreDoc scoreDoc = resultDocs[i];
            // JSD
            Map<Integer, Integer> termVector = getTermFrequenciesInteger(reader, scoreDoc.doc);
            double sim = getJSSimilarity3(termVector, testVector, numtopics);

            if(sim < epsylon){
                num_cercanos++;
            }

            // min max por scoring
            if(scoreDoc.score < score_ant){
                System.out.println("score: "+ score_ant  + ", minsim: " + minsim + ", maxsim: " + maxsim);

                score_ant = scoreDoc.score;
                maxsim = 0d;
                minsim = 1e6d;
                if(sim > maxsim){
                    maxsim = sim;
                }
                if(sim < minsim){
                    minsim = sim;
                }
            } else {
                if(sim > maxsim){
                    maxsim = sim;
                }
                if(sim < minsim){
                    minsim = sim;
                }
            }
        }
        System.out.println("score: "+ score_ant  + ", minsim: " + minsim + ", maxsim: " + maxsim);
        return num_cercanos;
    }




    private Document getTestDocument(String vectorString) {
        Document testDoc = new Document();
        FieldType fieldType = new FieldType(TextField.TYPE_STORED);//TYPE_NOT_STORED
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setStoreTermVectors(true);
        Field textField = new Field(FIELD_NAME, vectorString, fieldType);
        testDoc.add(textField);
        return testDoc;
    }

    private void getJS_distances(IndexReader reader, float[] testVectorDoctopics) throws IOException {
        int directoryLength = reader.getDocCount(FIELD_NAME);
        Map<Integer, Integer> testVector = getTestFrequenciesInteger(testVectorDoctopics, precision);
        int numtopics = docTopicsUtil.getNumtopics();

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
        System.out.println("minsim: " + minsim + ", maxsim: " + maxsim);
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
    private double getJSSimilarity(Map<String, Integer> termVector1, Map<String, Integer> termVector2, int numtopics) {
        // lower bound 1
        int sum = 0;

        for (int i = 0; i < numtopics; i++) {
            Integer freq1 = termVector1.get(""+i);
            if(freq1 == null){
                freq1 = 0;
            }
            Integer freq2 = termVector2.get(""+i);
            if(freq2 == null){
                freq2 = 0;
            }

            int diff = freq1 - freq2;
            sum +=  (diff >= 0 ? diff : -diff);
        }

        // lower bound 2
        if(sum >= epsylon_cota2_2){//sum > 0 y  epsylon_cota2_1 negativo
            return epsylon;
        }

        float sumF = 0f;
        for (int i = 0; i < numtopics; i++) {
            Integer freq1 = termVector1.get(""+i);
            if(freq1 == null){
                freq1 = 0;
            }
            Integer freq2 = termVector2.get(""+i);
            if(freq2 == null){
                freq2 = 0;
            }
            sumF += (freq1 > 0 ? ((int)freq1) * Math.log((2f * (int)freq1) / ((int)freq1 + (int)freq2)) : 0) +
                    (freq2 > 0 ? ((int)freq2) * Math.log((2f * (int)freq2) / ((int)freq1 + (int)freq2)) : 0);
        }
        return (float) sumF/20000f;
    }

    private double getJSSimilarity2(Map<String, Integer> termVector1, Map<String, Integer> termVector2, int numtopics) {
        // lower bound 1
        int sum = 0;
        Set<String> activeKeySet = new HashSet<String>();

        //
        for (Map.Entry<String, Integer> entry : termVector1.entrySet()) {
            //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            String  key1  = entry.getKey();
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
        for (Map.Entry<String, Integer> entry : termVector2.entrySet()) {
            String  key2  = entry.getKey();
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

        Iterator<String> iterator = activeKeySet.iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
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

    private void getCosine_distances(IndexReader reader, float[] testVectorDoctopics) throws IOException {
        int directoryLength = reader.getDocCount(FIELD_NAME);
        Map<String, Integer> testVector = getTestFrequencies(testVectorDoctopics, precision);

        for (int i = 0; i < directoryLength; i++) {
            Map<String, Integer> termVector = getTermFrequencies(reader, i);
            double sim = getCosineSimilarity(termVector, testVector);
            //System.out.println("cosine(doc["+ i + "], test) = " + sim);
        }
    }

    private double getCosineSimilarity(Map<String, Integer> termVector1, Map<String, Integer> termVector2) {
        Double result = 0.0d;
        for (String keyTerm1 : termVector1.keySet()) {
            Integer freq1 = termVector1.get(keyTerm1);
            Integer freq2 = termVector2.get(keyTerm1);

            if(freq1!=null && freq2!=null){
                result += freq1*freq2;
            }
        }
        return result;
    }

    private Map<String, Integer> getTermFrequencies(IndexReader reader, int docId) {
        Map<String, Integer> frequencies = new HashMap<>();

        try {
            Terms vector = reader.getTermVector(docId, FIELD_NAME);
            TermsEnum termsEnum = vector.iterator();
            BytesRef text = null;
            PostingsEnum postings = null;

            while ((text = termsEnum.next()) != null) {
                String termString = text.utf8ToString();
                postings = termsEnum.postings(postings, PostingsEnum.FREQS);
                postings.nextDoc();
                frequencies.put(termString, postings.freq());
                //System.out.println("Term: " + termString + ", freq: " + postings.freq());
            }
            return frequencies;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frequencies;
    }

    private Map<Integer, Integer> getTermFrequenciesInteger(IndexReader reader, int docId) {
        Map<Integer, Integer> frequencies = new HashMap<>();

        try {
            Terms vector = reader.getTermVector(docId, FIELD_NAME);
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

    private Map<String, Integer> getTestFrequencies(float[] testVectorDoctopics, float multiplication_factor) {
        Map<String, Integer> frequencies = new HashMap<>();

        for(int i=0; i<testVectorDoctopics.length;i++){
            int freq = (int)(testVectorDoctopics[i]*multiplication_factor);
            if(freq > 0){
                frequencies.put(""+i, freq);
            }
        }
        return frequencies;
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

}
