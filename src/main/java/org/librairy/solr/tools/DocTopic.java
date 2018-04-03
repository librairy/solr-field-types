package org.librairy.solr.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class DocTopic {

    private static final Double ratio = 10.0;


    public static String fromVector(Double threshold, Double ratio, List<Double> vector, String modelId){

        StringBuilder docTopic = new StringBuilder();

        for(int i=0; i<vector.size();i++){
            final String topicId = "t"+i;
            Double score = vector.get(i);
            if (score <= threshold) continue;

            double norm = score * ratio;
            int freq = Double.valueOf(Math.ceil(norm)).intValue();

            String partialDocTopic = IntStream.range(0, freq).mapToObj(j -> topicId + "@" + modelId).collect(Collectors.joining(" "));
            docTopic.append(partialDocTopic).append(" ");

        }

        return docTopic.toString();

    }

    public static Map<String,Double> relevance (Double threshold, Double ratio, List<Double> vector, String modelId){

        Map<String,Double> docTopic = new HashMap<>();

        for(int i=0; i<vector.size();i++){
            final String topicId = "t"+i;
            Double score = vector.get(i);
            if (score <= threshold) continue;

            double norm = score * ratio;

            String topicValue   = topicId + "@" + modelId;
            Double relevance    = Math.ceil(norm);
            docTopic.put(topicValue,relevance);

        }

        return docTopic;

    }


    public static void main(String[] args){


        Double threshold = 0.0002;

        List<Double> vector = Arrays.asList(new Double[]{0.00028,0.00332,0.0045,0.002,0.0033,0.07});

        String modelId = "m1";

        String docTopic = fromVector(0.001, 1000.0, vector, modelId);

        System.out.println(docTopic);


    }

}
