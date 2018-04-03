package org.librairy.solr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.solr.client.solrj.SolrServerException;
import org.librairy.service.space.rest.model.Point;
import org.librairy.solr.client.LibrairyClient;
import org.librairy.solr.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class LoadPointsToSpace {

    private static final Logger LOG = LoggerFactory.getLogger(LoadPointsToSpace.class);



    public static void main(String[] args) throws UnirestException, IOException, SolrServerException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("src/main/resources/corpus.json.gz"))));

        LibrairyClient librAIryClient = new LibrairyClient("http://localhost:8080");

        ExecutorService executors = Executors.newFixedThreadPool(10);
        ObjectMapper jsonMapper = new ObjectMapper();

        String json;
        AtomicInteger counter = new AtomicInteger();
        while((json = reader.readLine()) != null) {

            final Document doc = jsonMapper.readValue(json, Document.class);

            executors.submit(() -> {
                try {
                    Point point = new Point();
                    point.setId(doc.getId());
                    point.setType(doc.getType());
                    point.setName(doc.getName());
                    point.setShape(doc.getShape());

                    librAIryClient.add(point);

                    if (counter.getAndIncrement() % 100 == 0){
                        LOG.info("Added " + counter.get() + " points");
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

            LOG.info(counter.get() + " points finally added to Space");

        } catch (InterruptedException e) {
            LOG.warn("Error",e);
        }

    }



}
