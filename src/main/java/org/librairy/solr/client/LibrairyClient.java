package org.librairy.solr.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.librairy.service.space.rest.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class LibrairyClient {

    private static final Logger LOG = LoggerFactory.getLogger(LibrairyClient.class);

    private final String endpoint;

    static{
        Unirest.setDefaultHeader("Accept", "application/json");
        Unirest.setDefaultHeader("Content-Type", "application/json");

        com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
//        jacksonObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        jacksonObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Unirest.setObjectMapper(new ObjectMapper() {

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    public LibrairyClient(String endpoint) {
        this.endpoint = endpoint;
    }

    public PointList getPoints(Integer size, Optional<String> offset) throws UnirestException {

        HttpRequest query = Unirest.get(endpoint + "/points")
                .queryString("size", size);

        if (offset.isPresent()) query.queryString("offset",offset.get());

        HttpResponse<PointList> res = query.asObject(PointList.class);

        if (res.getStatus()!= 200 && res.getStatus() != 202){
            LOG.error("Error getting points from librAIry: " + res.getStatusText());
            return new PointList();
        }

        return res.getBody();
    }

    public Point getPoint(String id) throws UnirestException {
        HttpResponse<Point> point = Unirest.get(endpoint + "/points/{id}").routeParam("id", id).asObject(Point.class);
        return point.getBody();
    }

    public NeighbourList getRelated(String id, Integer num, Boolean force) throws UnirestException {
        NeighboursRequest request = new NeighboursRequest(num, Collections.emptyList(), force);
        HttpResponse<NeighbourList> neighbours = Unirest.post(endpoint + "/points/{id}/neighbours").routeParam("id", id).body(request).asObject(NeighbourList.class);
        if (neighbours.getBody().getNeighbours().size() != num) LOG.warn("No max size in list!");
        return neighbours.getBody();
    }

    public NeighbourList getRelatedByVector(List<Double> shape, Integer num, Boolean force) throws UnirestException {
        SimilarRequest request = new SimilarRequest(shape, num, Collections.emptyList(), false);
        HttpResponse<NeighbourList> neighbours = Unirest.post(endpoint + "/neighbours").body(request).asObject(NeighbourList.class);
        if (neighbours.getBody().getNeighbours().size() != num) LOG.warn("No max size in list!");
        return neighbours.getBody();
    }


    public void add(Point point) throws UnirestException {
        HttpResponse<JsonNode> result = Unirest.post(endpoint + "/points").body(point).asJson();
    }

    public static void main(String[] args) throws IOException, SolrServerException, UnirestException {


        LibrairyClient client = new LibrairyClient("http://localhost:8080");


        List<String> result = client.getRelated("5227dcc6bfbe38d7288b6980", 10,false).getNeighbours().stream().map(rel -> rel.getId()).collect(Collectors.toList());

        for(String doc: result){
            System.out.println("Similar to: " + doc);
        }



    }
}
