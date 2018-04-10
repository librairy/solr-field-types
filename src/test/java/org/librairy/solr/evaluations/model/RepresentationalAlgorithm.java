package org.librairy.solr.evaluations.model;

import org.apache.lucene.search.similarities.Similarity;

import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public interface RepresentationalAlgorithm {

    String representationOf(List<Double> topicDistributions);

    List<Double> shapeFrom(String topicRepresentation);

    String id();

    Similarity similarity();
}
