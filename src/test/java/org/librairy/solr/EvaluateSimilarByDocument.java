package org.librairy.solr;

import cc.mallet.util.Maths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Doubles;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.solr.client.solrj.SolrServerException;
import org.librairy.solr.client.LibrairyClient;
import org.librairy.solr.client.SolrClient;
import org.librairy.solr.model.Algorithm;
import org.librairy.solr.model.Document;
import org.librairy.solr.model.Evaluation;
import org.librairy.solr.model.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class EvaluateSimilarByDocument {
    private static final Logger LOG = LoggerFactory.getLogger(LoadDocumentsToSolr.class);


    public static void main(String[] args) throws UnirestException, IOException, SolrServerException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("src/main/resources/corpus.json.gz"))));

        ObjectMapper jsonMapper = new ObjectMapper();
        List<Document> documents = new ArrayList<>();

        String json;
        while((json = reader.readLine()) != null) {
            documents.add(jsonMapper.readValue(json, Document.class));
        }

        reader.close();
        LOG.info("Corpus in memory");


        Integer SAMPLE_SIZE     = 1000;
        Integer SIMILAR_SIZE    = 25;

        List<Document> sampleList = documents.stream().limit(SAMPLE_SIZE).collect(Collectors.toList());


        Map<String,Evaluation> evaluations = new ConcurrentHashMap<>();
        Map<String,SolrClient> clients = new ConcurrentHashMap<>();
        for(Algorithm algorithm: Algorithms.list){
            clients.put(algorithm.getName(),new SolrClient("http://localhost:8983/solr/"+algorithm.getName()));
            evaluations.put(algorithm.getName(), new Evaluation());
        }

        evaluations.put("crdc",new Evaluation());
        LibrairyClient librAIryClient = new LibrairyClient("http://localhost:8080");


        sampleList.parallelStream().forEach( sample -> {
            try{
                double[] shapeArray = Doubles.toArray(sample.getShape());

                // Get gold standard
                List<String> goldStandard = documents.stream().map(doc -> new Score(1-Maths.jensenShannonDivergence(shapeArray, Doubles.toArray(doc.getShape())), sample, doc)).sorted((a, b) -> -a.getValue().compareTo(b.getValue())).limit(SIMILAR_SIZE).map(doc -> doc.getSimilar().getId()).collect(Collectors.toList());

                LOG.info("Calculated similar documents to sample: " + sample.getId());

                for(Algorithm algorithm: Algorithms.list){

                    List<String> similarList = clients.get(algorithm.getName()).getMoreLikeThis(sample.getId(), SIMILAR_SIZE);

                    evaluations.get(algorithm.getName()).addResult(goldStandard, similarList);
                }

                List<String> similarList = librAIryClient.getRelated(sample.getId(), SIMILAR_SIZE, false).getNeighbours().stream().map(rel -> rel.getId()).collect(Collectors.toList());
                evaluations.get("crdc").addResult(goldStandard, similarList);
            }catch (Exception e){
                LOG.warn("Error on evaluation",e);
            }
        });

        for(Map.Entry evaluation: evaluations.entrySet()){
            LOG.info("Algorithm " + evaluation.getKey() + ": " + evaluation.getValue());
        }

    }



}