package org.librairy.solr;

import org.librairy.solr.model.Algorithm;

import java.util.Arrays;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class Algorithms {

    public static List<Algorithm> list = Arrays.asList( new Algorithm[]{
            new Algorithm("tw-tfidf",   0.0,    1000.0),
            new Algorithm("ltw-tfidf",  0.001,  1000.0),
            new Algorithm("tw-bm25",    0.0,    1000.0),
            new Algorithm("ltw-bm25",   0.001,  1000.0)
    });

}
