package org.librairy.solr.filter;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class TopicWordsFactory {

    private static final Double ratio = 10.0;


    public static String toText(Double threshold, Double ratio, List<Double> vector, String modelId){

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

    public static String toSortedText(Double threshold, Double ratio, List<Double> vector, String modelId){

        Map<Integer,String> topics = new HashMap<>();

        for(int i=0; i<vector.size();i++){
            final String topicId = "t"+i;
            Double score = vector.get(i);
            if (score <= threshold) continue;

            double norm = score * ratio;
            int freq = Double.valueOf(Math.ceil(norm)).intValue();
//            int freq = Double.valueOf(Math.floor(norm)).intValue();

            String partialDocTopic = IntStream.range(0, freq).mapToObj(j -> topicId + "@" + modelId).collect(Collectors.joining(" "));
            topics.put(i,partialDocTopic);

        }

        String shape = IntStream.range(0, vector.size()).mapToObj(i -> ImmutableMap.of("index", i, "score", vector.get(i))).sorted((a, b) -> -((Double) a.get("score")).compareTo((Double) b.get("score"))).map(map -> topics.get(map.get("index"))).collect(Collectors.joining(" "));

        return shape;

    }

    public static Map<String,Double> toRelevanceMap(Double threshold, Double ratio, List<Double> vector, String modelId){

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

        List<Double> vector = Arrays.asList(new Double[]{0.05028,0.00332,0.0045,0.002,0.0033,0.07});

        String modelId = "m1";

        System.out.println(toText(0.0, 1000.0, vector, modelId));

        System.out.println(toSortedText(0.0, 1000.0, vector, modelId));


    }

}
