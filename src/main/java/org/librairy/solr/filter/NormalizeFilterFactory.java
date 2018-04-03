package org.librairy.solr.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class NormalizeFilterFactory extends TokenFilterFactory implements MultiTermAwareComponent {

    private final int dimension;

    public NormalizeFilterFactory(Map<String, String> args) {
        super(args);
        this.dimension = this.getInt(args, "dim", 0);
        if(this.dimension <= 0) {
            throw new IllegalArgumentException("dim must be greater than 0 and passed: " + this.dimension);
        }
    }

    public NormalizeFilter create(TokenStream input) {
        return new NormalizeFilter(input, dimension);
    }

    public AbstractAnalysisFactory getMultiTermComponent() {
        return this;
    }
}
