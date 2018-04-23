package org.librairy.solr;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.librairy.solr.metric.MetricsUtils;
import org.librairy.solr.parse.DocTopicsUtil;

import java.io.*;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.stream.IntStream;

public class DocTopicsLucene2SimilarityGraphArray {
    String indexdir = "indexdir";
    String outputdir = "output_graph";
    long ini, fin;
    static DecimalFormat df4;

    float epsylon = 0.15f;
    float epsylon_2_2sqrt = (float) (2*Math.sqrt(2*epsylon));
    float epsylon_2_2sqrt_short = (float) (epsylon_2_2sqrt*10000);
    float epsylon_cota2_2 = (float) (Math.sqrt((-72f + 24f*Math.sqrt(9+2*epsylon)))*10000);
    float epsylon20000f = epsylon*20000f;

    String FIELD_NAME = "doctopics_field";
    DocTopicsUtil docTopicsUtil;

    public DocTopicsLucene2SimilarityGraphArray() {
        docTopicsUtil = new DocTopicsUtil();
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.');
        df4 = new DecimalFormat("#.####", simbolos);
    }

    public static void main(String[] args) {
        DocTopicsLucene2SimilarityGraphArray docTopicsLucene2SimilarityGraph = new DocTopicsLucene2SimilarityGraphArray();
        docTopicsLucene2SimilarityGraph.run(args);
    }

    private void run(String[] args) {
        // parse args

        // -input indexdirbin -output output_graph -epsylon 0.10 -field_name doctopics_field
        String usage = "Usage:\tjava DocTopicsLucene2SimilarityGraphArray [-input indexdir] [-output directory] [-epsylon value] [-field_name topic_field]\n";
        if (args.length > 0 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

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
            } else if ("-field_name".equals(args[i])) {
                FIELD_NAME = args[i+1];
                i++;
            }
        }

        try {
            // init index reader
            ini = System.currentTimeMillis();
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir)));
            // TODO test read only index, speedup
            //IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir), NoLockFactory.INSTANCE) );
            int maxdocs = reader.maxDoc();
            //int maxdocs = reader.getDocCount(FIELD_NAME);

            // TODO extract from index or index metadata
            short numtopics = 120;
            fin = System.currentTimeMillis();
            System.out.println("READ INDEX: " + (fin - ini) + " (ms), num index docs: " + maxdocs);



            // read Term vectors to memory array
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            ini = System.currentTimeMillis();
            short[][] docTopicValues = getTermVectorsArray(maxdocs, numtopics, reader);
            long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
            fin = System.currentTimeMillis();
            System.out.println("READ TERM VECTORS: " + (fin - ini) + " (ms)");
            System.out.println("TotalMemory: " + (int)(runtime.totalMemory()/(1024*1024)) + " Mb, freeMemory: " + (int)(runtime.freeMemory()/(1024*1024)) + " Mb, memDiff: " + (int)((usedMemoryAfter-memoryBefore)/(1024*1024)) + " Mb");

            // calculate graph & save to file
            saveSimilarityMatrixBinParallel(docTopicValues, maxdocs);
        } catch (IOException e) {
            e.printStackTrace();
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

    private short[][] getTermVectorsArray(int numdocs, int numtopics, IndexReader reader) throws IOException {
        //int directoryLength = reader.getDocCount(FIELD_NAME);
        short[][] docTopicValues = new short[numdocs][numtopics];

        //int max_topic_freq = 0;
        for (int docId = 0; docId < numdocs; docId++) {
            Terms vector = reader.getTermVector(docId, FIELD_NAME);
            TermsEnum termsEnum = vector.iterator();
            BytesRef text = null;
            PostingsEnum postings = null;

            while ((text = termsEnum.next()) != null) {
                String termString = text.utf8ToString();
                postings = termsEnum.postings(postings, PostingsEnum.FREQS);
                postings.nextDoc();
                short topic_number = Short.parseShort(termString);
                short topic_freq = (short)postings.freq();
                //System.out.println("topic_number: " + topic_number + ", topic_freq: " + topic_freq);
                docTopicValues[docId][topic_number] = topic_freq;
//				if(postings.freq() > max_topic_freq){
//					max_topic_freq = postings.freq();
//				}
            }
        }
//		System.out.println("max_topic_freq: " + max_topic_freq);
        return docTopicValues;
    }


    void saveSimilarityMatrixBinParallel(short[][] docTopicValues, int numdocs) {
        try {
            FileOutputStream outputStream;
            outputStream = new FileOutputStream(new File(outputdir + File.separator + "edges.csv"));
            BufferedWriter stdWriter = new BufferedWriter(new OutputStreamWriter(outputStream,"UTF-8"));

            long start_time = System.currentTimeMillis();

            //header
            stdWriter.write("\"Source\",\"Target\",\"Weight\",\"Type\"\n");

            // parallel
            IntStream.range(0, numdocs)
                    .parallel()
                    .forEach( id -> {
                        long time_ini = System.currentTimeMillis();
                        try {
                            //TODO param, choose in memory or write to disk after each operation
                            productSequence(id, docTopicValues, stdWriter, numdocs);
                            if(id%1000==0){
                                long time_fin = System.currentTimeMillis();
                                System.out.println("numdocs: " + id + ", time (ms): " + (time_fin - time_ini));
                                time_ini = System.currentTimeMillis();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });


            try {
                stdWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            long end_time = System.currentTimeMillis();
            System.out.println("\nTime: " + (end_time - start_time) + " ms" + "\n");
            stdWriter.flush();
            outputStream.close();

        } catch (IOException e) {
            System.err.println("Error writing topic distance file: ");
            e.printStackTrace();
        }
    }

    void productSequence(int i, short[][] docTopicValues, BufferedWriter stdWriter, int numdocs) throws IOException{
        for(int j = i+1; j < numdocs; j++){
            // va mejor tuning 2 que 3 !!
            float distance = MetricsUtils.jsd_tuning2(docTopicValues[i], docTopicValues[j], epsylon, epsylon_2_2sqrt_short, epsylon_cota2_2);

            if(distance < epsylon){
                // Normalize distance
                double distanceNormalized = 0.5d*(epsylon - distance)/epsylon + 0.5d;

                //TODO thread runnable
                String str = df4.format(distanceNormalized);
                stdWriter.write(i + "," + j + "," + str + ",\"Undirected\"\n");

//				connectedNode[i] = true;
//				connectedNode[j] = true;
            }
        }
    }

}