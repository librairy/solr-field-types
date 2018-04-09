package org.librairy.solr.loads;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.librairy.solr.filter.TopicWordsFactory;
import org.librairy.solr.model.Algorithm;
import org.librairy.solr.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class LoadDocuments {

    private static final Logger LOG = LoggerFactory.getLogger(LoadDocuments.class);


    public static List<Algorithm> algorithms = Arrays.asList( new Algorithm[]{
            new Algorithm("tw-tfidf",   0.0,    1000.0),
            new Algorithm("ltw-tfidf",  0.001,  1000.0),
            new Algorithm("tw-bm25",    0.0,    1000.0),
            new Algorithm("ltw-bm25",   0.001,  1000.0)
    });

    public static void main(String[] args) throws UnirestException, IOException, SolrServerException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("src/test/resources/corpus.json.gz"))));


        Map<String,SolrClient> clients = new HashMap<>();
        Map<String,AtomicInteger> counters = new HashMap<>();

        for(Algorithm algorithm: algorithms){
            clients.put(algorithm.getName(),new HttpSolrClient.Builder("http://localhost:8983/solr/"+algorithm.getName()).build());
            counters.put(algorithm.getName(), new AtomicInteger(1));
        }


        ExecutorService executors = Executors.newFixedThreadPool(10);
        ObjectMapper jsonMapper = new ObjectMapper();

        String json;
        while((json = reader.readLine()) != null) {

            Document doc = jsonMapper.readValue(json, Document.class);

            executors.submit(() -> {
                try {
                    for (Algorithm algorithm : algorithms) {

                        String docTopic = TopicWordsFactory.toText(algorithm.getThreshold(), algorithm.getRatio(), doc.getShape(), "m1");

                        SolrInputDocument document = new SolrInputDocument();
                        document.addField("id", doc.getId());
                        document.addField("name", doc.getName());
                        document.addField("type", doc.getType());
                        document.addField("about", docTopic);

                        clients.get(algorithm.getName()).add(document);

                        if (counters.get(algorithm.getName()).getAndIncrement() % 1000 == 0) {
                            LOG.info(counters.get(algorithm.getName()).get()-1 + " points added to Solr/" + algorithm.getName());
                            clients.get(algorithm.getName()).commit();

                        }
                    }

                } catch (Exception e) {
                    LOG.warn("Error", e);
                }
            });

        }

        LOG.info("Waiting to finish..");
        executors.shutdown();
        try {
            executors.awaitTermination(1, TimeUnit.HOURS);

            for (Algorithm algorithm : algorithms) {

                LOG.info(counters.get(algorithm.getName()).get()-1 + " points finally added to Solr/" + algorithm.getName());
                clients.get(algorithm.getName()).commit();
            }
        } catch (InterruptedException e) {
            LOG.warn("Error",e);
        }

    }



}
