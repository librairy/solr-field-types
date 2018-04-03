package org.librairy.solr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.solr.client.solrj.SolrServerException;
import org.librairy.solr.client.LibrairyClient;
import org.librairy.solr.client.SolrClient;
import org.librairy.solr.model.Algorithm;
import org.librairy.solr.model.Document;
import org.librairy.solr.model.Evaluation;
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
public class EvaluateListByTopic {
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


        Integer SAMPLE_SIZE     = 15;
        Integer SIMILAR_SIZE    = 25;



        Map<String,Evaluation> evaluations = new ConcurrentHashMap<>();
        Map<String,SolrClient> clients = new HashMap<>();
        for(Algorithm algorithm: Algorithms.list){
            clients.put(algorithm.getName(),new SolrClient("http://localhost:8983/solr/"+algorithm.getName()));
            evaluations.put(algorithm.getName(), new Evaluation());
        }

        evaluations.put("crdc",new Evaluation());


        for (int i=0;i<SAMPLE_SIZE;i++){


            final Integer index = i;
            // Get gold standard
            List<String> goldStandard = documents.parallelStream().sorted((a,b) -> -a.getShape().get(index).compareTo(b.getShape().get(index))).limit(SIMILAR_SIZE).map(d -> d.getId()).collect(Collectors.toList());

            LOG.info("Created gold-standard from topic: " + i);

            for(Algorithm algorithm: Algorithms.list){

                List<String> similarList = clients.get(algorithm.getName()).getByTermFrequency("about", "t"+i+"@m1", SIMILAR_SIZE);

                evaluations.get(algorithm.getName()).addResult(goldStandard, similarList);
            }

        }


        for(Map.Entry evaluation: evaluations.entrySet()){
            LOG.info("Algorithm " + evaluation.getKey() + ": " + evaluation.getValue());
        }

    }



}