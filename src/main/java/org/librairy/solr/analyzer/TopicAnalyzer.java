package org.librairy.solr.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.DelimitedTermFrequencyTokenFilter;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class TopicAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer tokenizer = new WhitespaceTokenizer();
        // TODO para usa DelimitedTermFrequencyTokenFilter mejor un mapa con los topicos activos que un array
        TokenFilter filters = new DelimitedTermFrequencyTokenFilter(tokenizer);
        return new TokenStreamComponents(tokenizer, filters);
    }
}
