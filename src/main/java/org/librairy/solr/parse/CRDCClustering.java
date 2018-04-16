package org.librairy.solr.parse;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.librairy.solr.model.Document;
import org.librairy.solr.model.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class CRDCClustering {

    private static final Logger LOG = LoggerFactory.getLogger(CRDCClustering.class);

    public static String getLabel(List<Double> vector, Double threshold, Integer multiplier){

        List<Score> sortedVector = IntStream.range(0, vector.size()).mapToObj(i -> new Score(vector.get(i), new Document(String.valueOf(i)), null)).sorted((a, b) -> -a.getValue().compareTo(b.getValue())).collect(Collectors.toList());

        StringBuilder shape = new StringBuilder();
        int index = 0;
        Double acc = 0.0;
        for(int i=0; i< vector.size();i++){
            if (acc >= threshold) break;
            Score res = sortedVector.get(i);
            Integer score = Double.valueOf(res.getValue()*multiplier).intValue();
            shape.append("t").append(res.getReference().getId()).append("_").append(i).append("|").append(score).append(" ");
            acc += res.getValue();
        }

        return shape.toString();
    }

    public static List<Double> getVector(String shape, Integer numDimensions, Integer multiplier){

        if (Strings.isNullOrEmpty(shape)) return Collections.emptyList();

        Double[] vector = new Double[numDimensions];
        Arrays.fill(vector,0.0);

        String[] topics = shape.split(" ");

        for(int i=0; i< topics.length; i++){

            String topic = topics[i];
            String[] topicValues = topic.split("\\|");
            Double score    = Double.valueOf(topicValues[1])/Double.valueOf(multiplier);
            Integer index   = Integer.valueOf(StringUtils.substringAfter(StringUtils.substringBefore(topicValues[0],"_"),"t"));
            vector[index] = score;
        }

        return Arrays.asList(vector);
    }

    public static void main(String[] args) {

        List<Double> vector = Arrays.asList(new Double[]{0.3,0.2,0.4,0.1});

        LOG.info("Vector: " + vector);

        Integer multiplier = 10;

        String shape = getLabel(vector, 0.7, multiplier);
        LOG.info("Shape: " + shape);

        LOG.info("Vector: " + getVector(shape, vector.size(), multiplier));


    }
}
