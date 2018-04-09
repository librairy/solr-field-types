package org.librairy.solr.evaluations.time;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.librairy.solr.TestCasePrinterRule;
import org.librairy.solr.factories.IndexFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class IndexTest {


    @Rule
    public TestCasePrinterRule pr = new TestCasePrinterRule();

    @Test(timeout = 10000)
    public void timeToSaveInMemoryFromBinary() throws IOException {
        IndexFactory.newTopicIndex(new RAMDirectory(),ParsingTest.CORPUS_BIN);
        Assert.assertTrue(true);
    }

    @Test(timeout = 10000)
    public void timeToSaveInMemoryFromTxt() throws IOException {
        IndexFactory.newTopicIndex(new RAMDirectory(),ParsingTest.CORPUS_TXT_GZ);
        Assert.assertTrue(true);
    }

    @Test(timeout = 10000)
    public void timeToSaveInFSFromBinary() throws IOException {
        IndexFactory.newTopicIndex(FSDirectory.open(new File("target/index").toPath()),ParsingTest.CORPUS_BIN);
        Assert.assertTrue(true);
    }

    @Test(timeout = 10000)
    public void timeToSaveInFSFromTxt() throws IOException {
        IndexFactory.newTopicIndex(FSDirectory.open(new File("target/index").toPath()),ParsingTest.CORPUS_TXT_GZ);
        Assert.assertTrue(true);
    }

}
