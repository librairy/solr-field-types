package org.librairy.solr.evaluations.accuracy;

import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.librairy.solr.evaluations.model.RepresentationalAlgorithm;
import org.librairy.solr.factories.EvaluationFactory;
import org.librairy.solr.metric.TermFreqSimilarity;
import org.librairy.solr.parse.DocTopicsUtil;

import java.io.IOException;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class SimilarityTest {

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
            public Similarity similarity() {
                return new BooleanSimilarity();
            }
        });
    }

    @Test
    public void cleanZeroAndEpsylon() throws IOException {

        int numTopics   = 120;
        float precision = 1e4f;
        float epsylon = 0.12f;
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
            public Similarity similarity() {
                return new BooleanSimilarity();
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
            public Similarity similarity() {
                return new LMDirichletSimilarity();
            }
        });
    }

    @Test
    public void cleanZeroAndEpsylonTF() throws IOException {

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
            public Similarity similarity() {
                return new TermFreqSimilarity();
            }
        });
    }

}
