package org.librairy.solr;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;
import org.librairy.service.space.rest.model.Point;
import org.librairy.solr.client.LibrairyClient;
import org.librairy.solr.tools.DocTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class SearchPointsInt {

    private static final Logger LOG = LoggerFactory.getLogger(SearchPointsInt.class);

    private HttpSolrClient server;


    @Before
    public void setup(){

        server = new HttpSolrClient.Builder("http://localhost:8983/solr/topTopics").build();
        //server = new HttpSolrClient.Builder("http://localhost:8983/solr/allTopics").build();
    }



    @Test
    public void byTopic() throws IOException, SolrServerException {

        String fieldName    = "about";
        String fieldValue   = "t0@m1";

        SolrQuery qterms = new SolrQuery();
        qterms.setQuery(fieldName+":"+fieldValue);
        qterms.setFilterQueries(fieldName+":"+fieldValue);
        qterms.setFields("id", "Terms:termfreq(\""+fieldName+"\",\""+fieldValue+"\")");
        qterms.addSort("termfreq(\""+fieldName+"\",\""+fieldValue+"\")", SolrQuery.ORDER.desc);
        QueryResponse response = server.query(qterms);
        SolrDocumentList results = response.getResults();
        long totalhits = results.getNumFound();
        System.out.println("Number of documents found (Document frequecy):" + totalhits + "\n");
        int i = 1;
        for (SolrDocument doc: results) {
            System.out.println("Output for Document " + i++ + ":");
            System.out.println("-----------------------------------------------");
            System.out.println("Document ID:" + doc.getFieldValue("id"));
            System.out.println("Number of Terms (Term frequency):" + doc.getFieldValue("Terms") + "\n\n");
        }
    }


    @Test
    public void byDocument() throws IOException, SolrServerException, UnirestException {

        SolrQuery query = new SolrQuery("id:yw_168nCYJs9o");
        query.set("mlt", true);
        query.set("mlt.fl", "about");
        query.set("mlt.mindf", 1);
        query.set("mlt.mintf", 1);
        query.set("fl", "id,name,score,about");

        QueryResponse response = server.query(query);


        NamedList<Object> moreLikeThis = (NamedList<Object>) response.getResponse().get("moreLikeThis");
        List<SolrDocument> docs =  (List<SolrDocument>)moreLikeThis.getVal(0);
        for (SolrDocument doc : docs)
                System.out.println(doc.get("score") + ", " + doc.get("about"));

        SolrDocumentList list = response.getResults();
        System.out.println(list.getNumFound() + "found");
        for (SolrDocument doc : list)
            System.out.println(doc.get("id") + "," + doc.get("about"));

    }







}
