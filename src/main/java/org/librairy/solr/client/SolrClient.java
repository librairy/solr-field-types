package org.librairy.solr.client;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.librairy.solr.tools.DocTopic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class SolrClient {

    private final HttpSolrClient server;

    public SolrClient(String endpoint) {
        server = new HttpSolrClient.Builder(endpoint).build();
    }


    public List<String> byTopic(String topic, Integer num) throws IOException, SolrServerException {

        String fieldName    = "about";
        String fieldValue   = topic;

        return byFieldValue(fieldName, fieldValue, num);
    }


    public List<String> byVector(List<Double> vector, double threshold, double ratio, Integer num) throws IOException, SolrServerException, UnirestException {


        String fieldName    = "about";
        String fieldValue   = DocTopic.fromVector(threshold, ratio, vector,"m1");

        return byFieldValue(fieldName, fieldValue,num);
    }


    private List<String> byFieldValue(String fieldName, String fieldValue, Integer num) throws IOException, SolrServerException {
        SolrQuery qterms = new SolrQuery();
        qterms.setQuery(fieldName+":"+fieldValue);
        qterms.setFilterQueries(fieldName+":"+fieldValue);
        qterms.setFields("id", "Terms:termfreq(\""+fieldName+"\",\""+fieldValue+"\")");
        qterms.addSort("termfreq(\""+fieldName+"\",\""+fieldValue+"\")", SolrQuery.ORDER.desc);
        qterms.setRows(num);
        QueryResponse response = server.query(qterms);
        SolrDocumentList results = response.getResults();
        return results.stream().map(r -> (String) r.getFieldValue("id")).limit(num).collect(Collectors.toList());
    }

    public List<String> getByTermFrequency(String fieldName, String fieldValue, Integer num) throws IOException, SolrServerException {
        SolrQuery qterms = new SolrQuery();
        qterms.setQuery("*:*");
        qterms.setFilterQueries(fieldName+":"+fieldValue);
        qterms.setFields("id", "Terms:termfreq(\""+fieldName+"\",\""+fieldValue+"\")");
        qterms.addSort("termfreq(\""+fieldName+"\",\""+fieldValue+"\")", SolrQuery.ORDER.desc);
        qterms.setRows(num);
        QueryResponse response = server.query(qterms);
        SolrDocumentList results = response.getResults();
        return results.stream().map(r -> (String) r.getFieldValue("id")).limit(num).collect(Collectors.toList());
    }

    public List<String> getByRelevance(String fieldName, Map<String,Double> relevance, Integer num) throws IOException, SolrServerException {
        SolrQuery qterms = new SolrQuery();

        String values = relevance.entrySet().stream().map(entry -> entry.getKey() + "^" + entry.getValue()).collect(Collectors.joining(" "));
        qterms.setQuery(fieldName+":"+values);
        qterms.setFields("id", "score");
        qterms.setRows(num);
        qterms.addSort("score", SolrQuery.ORDER.desc);
        QueryResponse response = server.query(qterms);
        SolrDocumentList results = response.getResults();
        if (results.size() < num) System.out.println("Result size: " + results.size());
        return results.stream().map(r -> (String) r.getFieldValue("id")).limit(num).collect(Collectors.toList());
    }


    public List<String> getMoreLikeThis(String id, Integer num) throws IOException, SolrServerException {
        SolrQuery qterms = new SolrQuery();
        qterms.setQuery("id:"+id);
        qterms.addSort("score", SolrQuery.ORDER.desc);
        qterms.setFields("id", "score");

        qterms.setMoreLikeThis(true);
        qterms.setMoreLikeThisCount(num);
        qterms.setMoreLikeThisFields("about");
        qterms.setMoreLikeThisMinDocFreq(1);
        qterms.setMoreLikeThisMinTermFreq(1);
        qterms.setMoreLikeThisBoost(true);


        QueryResponse response = server.query(qterms);
        NamedList<SolrDocumentList> list = response.getMoreLikeThis();
        SolrDocumentList results = list.get(id);

        return results.stream().map(r -> (String) r.getFieldValue("id")).limit(num).collect(Collectors.toList());

    }

    public static void main(String[] args) throws IOException, SolrServerException {


        SolrClient client = new SolrClient("http://localhost:8983/solr/tw-tfidf");


        List<String> result = client.getMoreLikeThis("5227dcc6bfbe38d7288b6980", 10);

        for(String doc: result){
            System.out.println("Similar to: " + doc);
        }


//        System.out.println("-----------------------------------");
//
//        List<Double> shape = Arrays.asList(new Double[]{0.5808636792883174, 0.0012924507488554738, 0.012987877933694545, 0.007797219954706585, 0.019073483008997246, 0.01563497192300208, 0.035953485498165544, 0.005851498534328535, 0.0003636651389412795, 0.0010815685721197858, 0.00000251634960138929, 0.07253282784565515, 0.01013426990551522, 0.0012029830592417468, 0.23522750223885497});
//
//        Map<String, Double> docTopic = DocTopic.relevance(0.0, 100.0, shape, "m1");
//
//        List<String> result2 = client.getByRelevance("about", docTopic, 10);
//
//        for(String doc: result2){
//            System.out.println("Similar to: " + doc);
//        }

    }

}
