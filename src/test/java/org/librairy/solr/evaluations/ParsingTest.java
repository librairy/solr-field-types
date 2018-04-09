package org.librairy.solr.evaluations;

import org.junit.Rule;
import org.librairy.solr.TestCasePrinterRule;
import org.librairy.solr.parse.DocTopicsUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class ParsingTest {

    private static final Logger LOG = LoggerFactory.getLogger(ParsingTest.class);

    public static String CORPUS_TXT_GZ = "src/test/resources/cordis-projects-fp1-h2020_nsf-1984-2018_120.doc_topics.gz";
    public static String CORPUS_BIN = "src/test/resources/cordis-projects-fp1-h2020_nsf-1984-2018_120.doc_topics.bin";


    float[][] docTopicValuesCleanned;
    short[][] docTopicValues;
    DocTopicsUtil docTopicsUtil;
    long ini, fin;

    @Rule
    public TestCasePrinterRule pr = new TestCasePrinterRule();


    @Before
    public void setup(){
        docTopicValuesCleanned = new float[1][1];
        docTopicValues = new short[1][1];
        docTopicsUtil = new DocTopicsUtil();
    }

    @Test(timeout = 10000)
    public void timeFromTxt() throws IOException {

        // Complete format: read &  clean doctopics
        // inspect file
        ini = System.currentTimeMillis();
        // docTopicsUtil.inspectBinTopicFile(doctopics_file);
        docTopicsUtil.inspectTopicFile_CompleteFormat(CORPUS_TXT_GZ);
        fin = System.currentTimeMillis();
        LOG.info("from text file inspectTopicFile_CompleteFormat (ms): " + (fin - ini));

        // load topics
        ini = System.currentTimeMillis();
        // if(docTopicsUtil.loadBinTopics(doctopics_file, numdocs, docTopicValues) == 0){
        int res = docTopicsUtil.loadTopics_CompleteFormat(CORPUS_TXT_GZ);
        fin = System.currentTimeMillis();
        LOG.info("from text file loadTopics_CompleteFormat (ms): " + (fin - ini));
        Assert.assertTrue("Error loading doc-topics...", res != 0);

        // clean zeros
        ini = System.currentTimeMillis();
        docTopicValuesCleanned = docTopicsUtil.cleanZeros();
        fin = System.currentTimeMillis();
        LOG.info("from text file cleanZeros (ms): " + (fin - ini));
    }

    @Test(timeout = 10000)
    public void timeFromBin() throws IOException {

        // Complete format: read &  clean doctopics
        // inspect file
        ini = System.currentTimeMillis();
        docTopicsUtil.inspectBinTopicFile(CORPUS_BIN);
        fin = System.currentTimeMillis();
        System.out.println("from bin file inspectTopicFile_CompleteFormat (ms): " + (fin - ini));

        int numdocs = docTopicsUtil.getNumdocs();
        Assert.assertTrue("Error loading doc-topics: incorrect numdocs...",numdocs > 0);

        int numtopics = docTopicsUtil.getNumtopics();
        Assert.assertTrue("Error loading doc-topics: incorrect numtopics...",numtopics > 0);

        docTopicValues = new short[numdocs][numtopics];
        ini = System.currentTimeMillis();
        int res = docTopicsUtil.loadBinTopics(CORPUS_BIN, numdocs, docTopicValues);
        fin = System.currentTimeMillis();
        System.out.println("from bin file loadTopics_binFormat (ms): " + (fin - ini));
        Assert.assertTrue("Error loading bin doc-topics...",res != 0);
    }
}
