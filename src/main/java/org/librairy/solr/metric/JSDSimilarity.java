package org.librairy.solr.metric;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class JSDSimilarity extends Similarity {

    public JSDSimilarity() {
    }


    //  indexer calls

    /**
     * Computes the normalization value for a field, given the accumulated state of term processing for this field (see FieldInvertState)
     * @param state current processing state for this field
     * @return
     */
    @Override
    public long computeNorm(FieldInvertState state) {
        return 0;
    }

    // query time (single time)

    /**
     * Compute any collection-level weight
     * @param boost
     * @param collectionStatistics collection-level statistics, such as the number of tokens in the collection.
     * @param termStatisticses term-level statistics, such as the document frequency of a term across the collection.
     * @return
     */
    @Override
    public SimWeight computeWeight(float boost, CollectionStatistics collectionStatistics, TermStatistics... termStatisticses) {

        return null;
    }

    // query time (matching document): TermQuery, SpanQuery and PhraseQuery

    /**
     * Creates a new Similarity.SimScorer to score matching documents from a segment of the inverted index.
     * @param simWeight
     * @param leafReaderContext
     * @return
     * @throws IOException
     */
    @Override
    public SimScorer simScorer(SimWeight simWeight, LeafReaderContext leafReaderContext) throws IOException {
        return null;
    }
}
