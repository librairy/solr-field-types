package org.librairy.solr.metric;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class JSDSimilarityTest {

    private static final Logger LOG = LoggerFactory.getLogger(JSDSimilarityTest.class);

    @Test
    public void calculate(){
        List<Double> v1 = Arrays.asList(new Double[]{0.5170500304375164,
                0.01599728924958733,
                0.02638541439612864,
                0.0036881811256474473,
                0.021333019486424104,
                0.0008083528013745158,
                0.032177209229131955,
                0.022963751949536707,
                0.02826552347508106,
                0.00225870342949005,
                6.794340373852136e-7,
                0.0004978508572178078,
                0.030798653606529357,
                0.00019711399002678422,
                0.29757822653226706});

        List<Double> v2 = Arrays.asList(new Double[]{ 0.6179757832326896,
                0.0038015306943692978,
                0.00041240740992645725,
                0.0019404587011289864,
                0.01320176388286571,
                0.005093856378300684,
                0.005822975339701762,
                0.007494340535315106,
                0.0006709381423211175,
                0.002495360517002289,
                9.24236612025723e-7,
                0.0005951216127214837,
                0.0032377393169565373,
                0.00029711373045226855,
                0.3369596862696319});


        LOG.info("Similarity: "+JSDSimilarity.btw(v1,v2));
    }


}
