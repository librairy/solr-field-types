package org.librairy.solr.evaluations.accuracy;

import cc.mallet.types.NormalizedDotProductMetric;
import cc.mallet.types.SparseVector;
import cc.mallet.util.Maths;
import com.google.common.primitives.Doubles;
import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
import com.jujutsu.utils.TSneUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.librairy.solr.factories.EvaluationFactory;
import org.librairy.solr.metric.JSDSimilarity;
import org.librairy.solr.metric.MetricsUtils;
import org.librairy.solr.metric.TermFreqSimilarity;
import org.librairy.solr.model.RepresentationalAlgorithm;
import org.librairy.solr.parse.CRDCClustering;
import org.librairy.solr.parse.DocTopicsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class SimilarityTest {

    private static final Logger LOG = LoggerFactory.getLogger(SimilarityTest.class);

    private static final String SAMPLE_CORPUS       = "src/test/resources/cordis-projects-fp1-h2020_nsf-1984-2018_120.doc_topics.gz";
    private static final Integer SAMPLE_SIZE        = 100;
    private static final Integer NUM_SIMILAR_DOCS   = 20;

    private static EvaluationFactory evaluationFactory;

    @BeforeClass
    public static void setup() throws IOException {
        evaluationFactory = new EvaluationFactory(SAMPLE_CORPUS, SAMPLE_SIZE, NUM_SIMILAR_DOCS);
    }


    @Rule
    public TestName testName = new TestName();

    @Rule
    public TestWatcher testWatcher = new TestWatcher() {
        @Override
        protected void starting(final Description description) {
            String methodName = description.getMethodName();
            String className = description.getClassName();
            className = className.substring(className.lastIndexOf('.') + 1);
            System.err.println("Starting JUnit-test: " + className + " " + methodName);
        }
    };



    @Test
    public void CleanZero() throws IOException {

        int numTopics = 120;

        // evaluate index
        evaluationFactory.newFrom(new RepresentationalAlgorithm() {
            @Override
            public String representationOf(List<Double> topicDistributions) {
                return DocTopicsUtil.getVectorString(topicDistributions, 1e4f);
            }

            @Override
            public List<Double> shapeFrom(String topicRepresentation) {
                return DocTopicsUtil.getVectorFromString(topicRepresentation, 1e4f, numTopics);
            }

            @Override
            public String id() {
                return testName.getMethodName();
            }

            @Override
            public Similarity similarityMetric() {
                return new BooleanSimilarity();
            }

            @Override
            public Double similarityScore(List<Double> v1, List<Double> v2) {
                return JSDSimilarity.btw(v1, v2);
            }
        });
    }

    @Test
    public void cleanZeroAndEpsylon() throws IOException {

        int numTopics   = 120;
        float precision = 1e4f;
        float epsylon = 0.01f;
        float epsylon_2_2sqrt = (float) (2*Math.sqrt(2*epsylon));
        float epsylon_2_2sqrt_short = (float) (epsylon_2_2sqrt*precision);
        float epsylon_cota2_2 = (float) (Math.sqrt((-72f + 24f*Math.sqrt(9+2*epsylon)))*precision);
        float epsylon20000f = epsylon*20000f;

        // evaluate index
        evaluationFactory.newFrom(new RepresentationalAlgorithm() {
            @Override
            public String representationOf(List<Double> topicDistributions) {
                return DocTopicsUtil.getVectorString(topicDistributions, precision, epsylon);
            }

            @Override
            public List<Double> shapeFrom(String topicRepresentation) {
                return DocTopicsUtil.getVectorFromString(topicRepresentation, precision, numTopics, epsylon);
            }

            @Override
            public String id() {
                return testName.getMethodName();
            }

            @Override
            public Similarity similarityMetric() {
                return new BooleanSimilarity();
            }

            @Override
            public Double similarityScore(List<Double> v1, List<Double> v2) {
                return JSDSimilarity.btw(v1, v2);
            }
        });
    }

    @Test
    public void cleanZeroAndEpsylonLMDirichlet() throws IOException {

        int numTopics   = 120;
        float precision = 1e4f;
        float epsylon = 0.12f;

        // evaluate index
        evaluationFactory.newFrom(new RepresentationalAlgorithm() {
            @Override
            public String representationOf(List<Double> topicDistributions) {
                return DocTopicsUtil.getVectorString(topicDistributions, precision, epsylon);
            }

            @Override
            public List<Double> shapeFrom(String topicRepresentation) {
                return DocTopicsUtil.getVectorFromString(topicRepresentation, precision, numTopics, epsylon);
            }

            @Override
            public String id() {
                return testName.getMethodName();
            }

            @Override
            public Similarity similarityMetric() {
                return new LMDirichletSimilarity();
            }

            @Override
            public Double similarityScore(List<Double> v1, List<Double> v2) {
                return JSDSimilarity.btw(v1, v2);
            }
        });
    }

    @Test
    public void cleanZeroAndEpsylonTF() throws IOException {

        int numTopics   = 120;
        float precision = 1e4f;
        float epsylon   = 0.12f;

        // evaluate index
        evaluationFactory.newFrom(new RepresentationalAlgorithm() {
            @Override
            public String representationOf(List<Double> topicDistributions) {
                return DocTopicsUtil.getVectorString(topicDistributions, precision, epsylon);
            }

            @Override
            public List<Double> shapeFrom(String topicRepresentation) {
                return DocTopicsUtil.getVectorFromString(topicRepresentation, precision, numTopics, epsylon);
            }

            @Override
            public String id() {
                return testName.getMethodName();
            }

            @Override
            public Similarity similarityMetric() {
                return new TermFreqSimilarity();
            }

            @Override
            public Double similarityScore(List<Double> v1, List<Double> v2) {
                return JSDSimilarity.btw(v1, v2);
            }
        });
    }

    @Test
    public void cleanZeroAndEpsylonTermVectorsMap() throws IOException {

        int numTopics   = 120;
        float precision = 1e4f;
        float epsylon = 0.01f;
        float epsylon_2_2sqrt = (float) (2*Math.sqrt(2*epsylon));
        float epsylon_2_2sqrt_short = (float) (epsylon_2_2sqrt*precision);
        float epsylon_cota2_2 = (float) (Math.sqrt((-72f + 24f*Math.sqrt(9+2*epsylon)))*precision);
        float epsylon20000f = epsylon*20000f;

        // evaluate index
        evaluationFactory.newFrom(new RepresentationalAlgorithm() {
            @Override
            public String representationOf(List<Double> topicDistributions) {
                String termVectorString = DocTopicsUtil.getVectorString(topicDistributions, precision, epsylon);
                Map<Integer,Integer> termVector = new HashMap<>();
                String[] topics = termVectorString.split(" ");
                for (int i=0; i< topics.length; i++){
                    Integer topicId = Integer.valueOf(StringUtils.substringBefore(topics[i], "|"));
                    Integer topicValue = Integer.valueOf(StringUtils.substringAfter(topics[i], "|"));
                    termVector.put(topicId,topicValue);
                }
                return DocTopicsUtil.getVectorStringfromMapReduced(termVector, epsylon_cota2_2);
            }

            @Override
            public List<Double> shapeFrom(String topicRepresentation) {
                List<Double> vector = DocTopicsUtil.getVectorFromString(topicRepresentation, precision, numTopics, epsylon);
                return vector;
            }

            @Override
            public String id() {
                return testName.getMethodName();
            }

            @Override
            public Similarity similarityMetric() {
                return new BooleanSimilarity();
            }

            @Override
            public Double similarityScore(List<Double> v1, List<Double> v2) {
                return JSDSimilarity.btw(v1, v2);
            }
        });
    }

    @Test
    public void docTopicsLucene2SimilarityGraphArray() throws IOException {

        int numTopics   = 120;
        float precision = 1e4f;
        float epsylon = 0.15f;
        float epsylon_2_2sqrt = (float) (2*Math.sqrt(2*epsylon));
        float epsylon_2_2sqrt_short = (float) (epsylon_2_2sqrt*10000);
        float epsylon_cota2_2 = (float) (Math.sqrt((-72f + 24f*Math.sqrt(9+2*epsylon)))*10000);
        float epsylon20000f = epsylon*20000f;


        AtomicInteger nullShapes = new AtomicInteger(1);
        // evaluate index
        evaluationFactory.newFrom(new RepresentationalAlgorithm() {
            @Override
            public String representationOf(List<Double> topicDistributions) {
                String termVectorString = DocTopicsUtil.getVectorString(topicDistributions, precision, epsylon);
                try{
                    Map<Integer,Integer> termVector = new HashMap<>();
                    String[] topics = termVectorString.split(" ");
                    for (int i=0; i< topics.length; i++){
                        Integer topicId = Integer.valueOf(StringUtils.substringBefore(topics[i], "|"));
                        Integer topicValue = Integer.valueOf(StringUtils.substringAfter(topics[i], "|"));
                        termVector.put(topicId,topicValue);
                    }
                    return DocTopicsUtil.getVectorStringfromMapReduced(termVector, epsylon_cota2_2);
                }catch (Exception e){
                    LOG.warn(""+nullShapes.getAndIncrement()+ " empty shapes from DocTopicUtils");
                    return "";
                }
            }

            @Override
            public List<Double> shapeFrom(String topicRepresentation) {
                List<Double> vector = DocTopicsUtil.getVectorFromString(topicRepresentation, precision, numTopics, epsylon);
                return vector;
            }

            @Override
            public String id() {
                return testName.getMethodName();
            }

            @Override
            public Similarity similarityMetric() {
                return new BooleanSimilarity();
            }

            @Override
            public Double similarityScore(List<Double> v1, List<Double> v2) {

                List<Double> v1Norm = v1.stream().map(v -> v * precision).collect(Collectors.toList());
                List<Double> v2Norm = v2.stream().map(v -> v * precision).collect(Collectors.toList());

                float distance = MetricsUtils.jsd_tuning2(v1Norm, v2Norm, epsylon, epsylon_2_2sqrt_short, epsylon_cota2_2);

                double similarity = 0.0f;


                if(distance < epsylon){
                    // Normalize distance
                    double distanceNormalized =  0.5d*(epsylon - distance)/epsylon + 0.5d;
                    similarity = distanceNormalized;
                }
                return similarity;
            }
        });
    }

    @Test
    public void crdc() throws IOException {

        double threshold    = 0.85;
        int numTopics       = 120;
        int multiplier      = 10000;

        // evaluate index
        evaluationFactory.newFrom(new RepresentationalAlgorithm() {
            @Override
            public String representationOf(List<Double> topicDistributions) {
                return CRDCClustering.getLabel(topicDistributions,threshold,multiplier);
            }

            @Override
            public List<Double> shapeFrom(String topicRepresentation) {
                return CRDCClustering.getVector(topicRepresentation, numTopics,multiplier);
            }

            @Override
            public String id() {
                return testName.getMethodName();
            }

            @Override
            public Similarity similarityMetric() {
                return new BooleanSimilarity();
            }

            @Override
            public Double similarityScore(List<Double> v1, List<Double> v2) {
                return JSDSimilarity.btw(v1, v2);
            }
        });
    }

    @Test
    public void tSNE() throws IOException {

        int initial_dims = 120;
        double perplexity = 10.0; // 20.0

        InputStream inputStream = new GZIPInputStream(new FileInputStream(new File(evaluationFactory.getCorpusPath())));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
        String line;
        List<List<Double>> shapes = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        while ((line = bufferedReader.readLine()) != null) {
            // comment line
            if(line.trim().startsWith("#")){
                continue;
            }
            String[] result = line.split("\\t");
            String id = result[1].replace("\"", "").replace("/","-").toUpperCase().trim();
            List<Double> shape = new ArrayList<>();
            for (int i=2; i<result.length; i++){
                shape.add(Double.valueOf(result[i]));
            }
            shapes.add(shape);
//            if (counter.getAndIncrement() == 1000) break;
        }
        bufferedReader.close();
        LOG.info("corpus initialized");

//        double [][] X = MatrixUtils.simpleRead2DMatrix(new File("src/main/resources/datasets/mnist2500_X.txt"), "   ");
        double[][] X = new double[shapes.size()][shapes.get(0).size()];
        for(int i=0; i<shapes.size(); i++){
            X[i] = Doubles.toArray(shapes.get(i));
        }
        LOG.info("X matrix initialized");

        //System.out.println(MatrixOps.doubleArrayToPrintString(X, ", ", 50,10));
        BarnesHutTSne tsne;
        boolean parallel = false;
        if(parallel) {
            tsne = new ParallelBHTsne();
        } else {
            tsne = new BHTSne();
        }
        TSneConfiguration config = TSneUtils.buildConfig(X, 2, initial_dims, perplexity, 1000);
        LOG.info("initializing t-SNE ..");
        double [][] Y = tsne.tsne(config);
        LOG.info("t-SNE completed!");


        LOG.info("Vector: " + Doubles.asList(Y[0]));

        double[] xValues = new double[Y.length];
        double[] yValues = new double[Y.length];
        for(int i=0;i<Y.length;i++){
            xValues[i] = Y[i][0];
            yValues[i] = Y[i][1];
        }

        double[] minValues = new double[2];
        minValues[0] = Double.valueOf(StatUtils.min(xValues));
        minValues[1] = Double.valueOf(StatUtils.min(yValues));

        double[] maxValues = new double[2];
        maxValues[0] = Double.valueOf(StatUtils.max(xValues));
        maxValues[1] = Double.valueOf(StatUtils.max(yValues));


        double[][] normalizedY = new double[Y.length][2];
        for(int i=0;i<Y.length;i++){
            double x = Y[i][0];
            normalizedY[i][0] =  ((x - minValues[0]) / (maxValues[0] - minValues[0]))+1;

            double y = Y[i][1];
            normalizedY[i][1] =  ((y - minValues[1]) / (maxValues[1] - minValues[1]))+1;
        }




        Map<List<Double>,String> shapesMap = new HashMap<>();
        int multiplier = 10000;
        for(int i=0;i<X.length;i++){
            List<Double> key = Doubles.asList(X[i]);
            double[] value = normalizedY[i];
            StringBuilder shape = new StringBuilder();
            for(int j=0;j<value.length;j++){
                double x = value[j];
                int score = Double.valueOf(x * multiplier).intValue();
                shape.append("t").append(j).append("|").append(score).append(" ");
            }
            shapesMap.put(key,shape.toString());
        }


        double[] v1 = X[0];
        double[] v2 = X[1];
        double[] v3 = X[2];
        System.out.println("v1: " + Doubles.asList(v1));
        System.out.println("v2: " + Doubles.asList(v2));
        System.out.println("v3: " + Doubles.asList(v3));

        System.out.println("JSD v1-v2: " + (1 - Maths.jensenShannonDivergence(v1,v2)));
        System.out.println("JSD v1-v3: " + (1 - Maths.jensenShannonDivergence(v1,v3)));
        System.out.println("JSD v2-v3: " + (1 - Maths.jensenShannonDivergence(v2,v3)));

        double[] V1 = normalizedY[0];
        double[] V2 = normalizedY[1];
        double[] V3 = normalizedY[2];
        System.out.println("V1: " + Doubles.asList(V1));
        System.out.println("V2: " + Doubles.asList(V2));
        System.out.println("V3: " + Doubles.asList(V3));


        System.out.println("JSD t-SNE v1-v2: " + (1 - Maths.jensenShannonDivergence(V1,V2)));
        System.out.println("JSD t-SNE v1-v3: " + (1 - Maths.jensenShannonDivergence(V1,V3)));
        System.out.println("JSD t-SNE v2-v3: " + (1 - Maths.jensenShannonDivergence(V2,V3)));


        NormalizedDotProductMetric metric = new NormalizedDotProductMetric();
        SparseVector a = new SparseVector(V1, true);
        SparseVector b = new SparseVector(V2, true);
        SparseVector c = new SparseVector(V3, true);
        System.out.println("Sparse V1: " + a);
        System.out.println("Sparse V2: " + b);
        System.out.println("Sparse V3: " + c);

        System.out.println("Cosine Similarity v1-v2: " + metric.distance(a,b));
        System.out.println("Cosine Similarity v1-v3: " + metric.distance(a,c));
        System.out.println("Cosine Similarity v2-v3: " + metric.distance(b,c));


        EuclideanDistance euclideanDistance = new EuclideanDistance();

        System.out.println("Euclidean Distance v1-v2: " + euclideanDistance.compute(V1,V2));
        System.out.println("Euclidean Distance v1-v3: " + euclideanDistance.compute(V1,V3));
        System.out.println("Euclidean Distance v2-v3: " + euclideanDistance.compute(V2,V3));

        // evaluate index
        evaluationFactory.newFrom(new RepresentationalAlgorithm() {
            @Override
            public String representationOf(List<Double> topicDistributions) {
                String shape = shapesMap.get(topicDistributions);
                return shape;
            }

            @Override
            public List<Double> shapeFrom(String topicRepresentation) {
                //List<Double> vector = shapesMap.entrySet().parallelStream().filter(entry -> entry.getValue().equalsIgnoreCase(topicRepresentation)).map(e -> e.getKey()).collect(Collectors.toList()).get(0);
                List<Double> vector = new ArrayList<Double>();
                String[] values = topicRepresentation.split(" ");
                vector.add(Double.valueOf(StringUtils.substringAfter(values[0],"|"))/Double.valueOf(multiplier));
                vector.add(Double.valueOf(StringUtils.substringAfter(values[1],"|"))/Double.valueOf(multiplier));


                return vector;
            }

            @Override
            public String id() {
                return testName.getMethodName();
            }

            @Override
            public Similarity similarityMetric() {
                return new BooleanSimilarity();
            }

            @Override
            public Double similarityScore(List<Double> v1, List<Double> v2) {
                return -euclideanDistance.compute(Doubles.toArray(v1), Doubles.toArray(v2));
            }
        });
    }

}
