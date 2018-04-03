package org.librairy.solr.metric;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.SimilarityFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class JSDSimilarityFactory extends SimilarityFactory {
    private boolean discountOverlaps;
    private float k1;
    private float b;

    public JSDSimilarityFactory() {
    }

    public void init(SolrParams params) {
        super.init(params);
        this.discountOverlaps = params.getBool("discountOverlaps", true);
        this.k1 = params.getFloat("k1", 1.2F);
        this.b = params.getFloat("b", 0.75F);
    }

    public Similarity getSimilarity() {
        BM25Similarity sim = new BM25Similarity(this.k1, this.b);
        sim.setDiscountOverlaps(this.discountOverlaps);
        return sim;
    }
}