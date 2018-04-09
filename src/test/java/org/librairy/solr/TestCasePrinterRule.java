package org.librairy.solr;

import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TestCasePrinterRule implements TestRule {

    private static final Logger LOG = LoggerFactory.getLogger(TestCasePrinter.class);
    private final TestCasePrinter printer = new TestCasePrinter();

    private String beforeContent = null;
    private String afterContent = null;
    private Instant timeStart;
    private Instant timeEnd;

    public TestCasePrinterRule() {
    }

    private class TestCasePrinter extends ExternalResource {
        @Override
        protected void before() throws Throwable {
            timeStart = Instant.now();
            LOG.info(beforeContent);
        };


        @Override
        protected void after() {
            timeEnd = Instant.now();

            LOG.info(afterContent + "Time elapsed: " + ChronoUnit.MINUTES.between(timeStart,timeEnd) + "min " + (ChronoUnit.SECONDS.between(timeStart,timeEnd)%60) + "secs " + (ChronoUnit.MILLIS.between(timeStart,timeEnd)%1000) + "msecs");

        };
    }

    public final Statement apply(Statement statement, Description description) {
        beforeContent = "["+description.getMethodName()+"-START]";
        afterContent =  "["+description.getMethodName()+"-END]";
        return printer.apply(statement, description);
    }
}