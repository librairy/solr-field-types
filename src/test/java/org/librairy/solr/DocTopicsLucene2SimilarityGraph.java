package org.librairy.solr;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.DelimitedTermFrequencyTokenFilter;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.librairy.solr.parse.DocTopicsUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

public class DocTopicsLucene2SimilarityGraph {
    String indexdir = "indexdir";
    String outputdir = "output_graph";
    long ini, fin;

    float epsylon = 0.15f;
    float epsylon_2_2sqrt = (float) (2*Math.sqrt(2*epsylon));
    float epsylon_2_2sqrt_short = (float) (epsylon_2_2sqrt*10000);
    float epsylon_cota2_2 = (float) (Math.sqrt((-72f + 24f*Math.sqrt(9+2*epsylon)))*10000);
    float epsylon20000f = epsylon*20000f;

    private final String FIELD_NAME = "doctopics_field";
    DocTopicsUtil docTopicsUtil;

    public DocTopicsLucene2SimilarityGraph() {
        docTopicsUtil = new DocTopicsUtil();
    }

    public static void main(String[] args) {
        DocTopicsLucene2SimilarityGraph docTopicsLucene2SimilarityGraph = new DocTopicsLucene2SimilarityGraph();
        docTopicsLucene2SimilarityGraph.run(args);
    }

    private void run(String[] args) {
        // parse args

        // -input indexdirbin -output output_graph -epsylon 0.20
        String usage = "Usage:\tjava DocTopicsLucene2SimilarityGraph [-input indexdir] [-output directory] [-epsylon value]\n";
        if (args.length > 0 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

//		options.addOption("m", true, "node metadata file");
//		options.addOption("ns", false, "dont save node metadata file");

        for(int i = 0;i < args.length;i++) {
            if ("-input".equals(args[i])) {
                indexdir = args[i+1];
                i++;
            } else if ("-output".equals(args[i])) {
                outputdir = args[i+1];
                i++;
            } else if ("-epsylon".equals(args[i])) {
                epsylon = Float.parseFloat(args[i+1]);
                i++;
            }
        }

        try {
            // init index reader
            ini = System.currentTimeMillis();
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir)));
            //IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir), NoLockFactory.INSTANCE) );


            int maxdocs = reader.maxDoc();
            fin = System.currentTimeMillis();
            System.out.println("READ INDEX: " + (fin - ini) + " (ms), num index docs: " + maxdocs);

            // read Term vectors to memory
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            ini = System.currentTimeMillis();
            // TODO Short
            HashMap<Integer, Map<Short,Short>> termVectorsMap = getTermVectorsMap(maxdocs, reader);
            long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
            fin = System.currentTimeMillis();
            System.out.println("READ TERM VECTORS: " + (fin - ini) + " (ms)");
            System.out.println("TotalMemory: " + (int)(runtime.totalMemory()/(1024*1024)) + " Mb, freeMemory: " + (int)(runtime.freeMemory()/(1024*1024)) + " Mb, memDiff: " + (int)((usedMemoryAfter-memoryBefore)/(1024*1024)) + " Mb");


            // query init
            Analyzer myAnalyzer = new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents(String fieldName) {//, Reader reader
                    Tokenizer tokenizer = new WhitespaceTokenizer();
                    TokenFilter filters = new DelimitedTermFrequencyTokenFilter(tokenizer);
                    return new TokenStreamComponents(tokenizer, filters);
                }
            };
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser(FIELD_NAME, myAnalyzer);

//	   		long ini_total = System.currentTimeMillis();
//			for(int i = 0; i < maxdocs; i++){
//			    try {
//			    	//ini = System.currentTimeMillis();
//			    	getJS_distances_inverted_index_termvectorsmap_by_id(reader, searcher, 1000, termVectorsMap, parser);
//			    	printDot(i);
//			    	//fin = System.currentTimeMillis();
//					//System.out.println("JS iter: " + (fin - ini) + " (ms)");
//			    } catch (IOException | ParseException e) {
//					//e.printStackTrace();
//			    	System.out.println("Error doc: " + i);
//
//				}
//			}
//    		long fin_total = System.currentTimeMillis();
//			System.out.println("JS: " + (fin_total - ini_total) + " (ms)");

            // parallel query
            int numtopics = docTopicsUtil.getNumtopics();
            long ini_total = System.currentTimeMillis();
            IntStream.range(0, maxdocs)
                    .parallel()
                    .forEach( id -> {
                        try {
                            //getJS_distances_inverted_index_termvectorsmap_by_id(reader, searcher, id, termVectorsMap, parser);
                            productSequence(id, maxdocs, termVectorsMap, numtopics);
                            printDot(id);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            long fin_total = System.currentTimeMillis();
            System.out.println("JS: " + (fin_total - ini_total) + " (ms)");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void productSequence(int i, int numdocs, HashMap<Integer, Map<Short, Short>> termVectorsMap, int numtopics) throws IOException{
        for(int j = i+1; j < numdocs; j++){
            Map<Short, Short> termVector1 = termVectorsMap.get(i);
            Map<Short, Short> termVector2 = termVectorsMap.get(j);

            // va mejor tuning 2 que 3 !!
            double distance = getJSSimilarity3(termVector1, termVector2, numtopics);

//			if(distance < epsylon){
//				// Normalize distance
//				//double distanceNormalized = 0.5d*(epsylon - distance)/epsylon + 0.5d;
//
//			}
        }
    }

    private void printDot(int id) {
        if(id>0 && id%1000==0){
            System.out.print(".");
            if(id%100000==0){
                System.out.println("");
            }
        }
    }

    private HashMap<Integer, Map<Short, Short>> getTermVectorsMap(int length, IndexReader reader) throws IOException {
        HashMap<Integer, Map<Short,Short>> termVectorsMap = new HashMap<Integer, Map<Short,Short>>(length);

        int directoryLength = reader.getDocCount(FIELD_NAME);

        for (int i = 0; i < directoryLength; i++) {
            Map<Short, Short> termVector = getTermFrequenciesInteger(reader, i);
            termVectorsMap.put(i, termVector);
        }
        return termVectorsMap;
    }

    private Map<Short, Short> getTermFrequenciesInteger(IndexReader reader, int docId) {
        Map<Short, Short> frequencies = new HashMap<>();

        try {
            Terms vector = reader.getTermVector(docId, FIELD_NAME);
            TermsEnum termsEnum = vector.iterator();
            BytesRef text = null;
            PostingsEnum postings = null;

            while ((text = termsEnum.next()) != null) {
                String termString = text.utf8ToString();
                postings = termsEnum.postings(postings, PostingsEnum.FREQS);
                postings.nextDoc();
                frequencies.put(Short.parseShort(termString), (short)postings.freq());
                //System.out.println("Term: " + termString + ", freq: " + postings.freq());
            }
            return frequencies;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frequencies;
    }

    private double getJSSimilarity3(Map<Short, Short> termVector1, Map<Short, Short> termVector2, int numtopics) {
        // lower bound 1
        int sum = 0;
        Set<Short> activeKeySet = new HashSet<Short>(numtopics);

        for (Map.Entry<Short, Short> entry : termVector1.entrySet()) {
            Short key1  = entry.getKey();
            Short freq1 = entry.getValue();
            activeKeySet.add(key1);

            // contenido en termVector1, termVector2
            Short freq2 = termVector2.get(key1);
            if(freq2 !=null){
                int diff = freq1 - freq2;
                sum += (diff >= 0 ? diff : -diff);
                // contenido en termVector1 pero no en termVector2
            } else {
                sum += freq1;
            }
        }
        // contenido en termVector2 pero no en termVector1
        for (Map.Entry<Short, Short> entry : termVector2.entrySet()) {
            Short key2  = entry.getKey();
            Short freq2 = entry.getValue();
            activeKeySet.add(key2);

            Short freq1 = termVector1.get(key2);
            if(freq1 == null){
                sum += freq2;
            }
        }

        // lower bound 2
        if(sum >= epsylon_cota2_2){//sum > 0 y  epsylon_cota2_1 negativo
            return epsylon;
        }

        float sumF = 0f;

        Iterator<Short> iterator = activeKeySet.iterator();
        while(iterator.hasNext()){
            Short key = iterator.next();
            Short freq1 = termVector1.get(key);
            if(freq1 == null){
                freq1 = 0;
            }

            Short freq2 = termVector2.get(key);
            if(freq2 == null){
                freq2 = 0;
            }
            sumF += (freq1 > 0 ? ((int)freq1) * Math.log((2f * (int)freq1) / ((int)freq1 + (int)freq2)) : 0) +
                    (freq2 > 0 ? ((int)freq2) * Math.log((2f * (int)freq2) / ((int)freq1 + (int)freq2)) : 0);
        }

        return (float) sumF/20000f;
    }


}