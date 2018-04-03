package org.librairy.solr.filter;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class NormalizeFilter extends TokenFilter {
    private final CharTermAttribute termAttribute               = (CharTermAttribute)this.addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAttribute    = (PositionIncrementAttribute)this.addAttribute(PositionIncrementAttribute.class);
    private final CharArraySet previous                         = new CharArraySet(8, false);

    private final Integer dim;

    public NormalizeFilter(TokenStream in, Integer dim) {
        super(in);
        this.dim = dim;
    }

    public boolean incrementToken() throws IOException {
        while(true) {
            if(this.input.incrementToken()) {
                char[] term         = this.termAttribute.buffer();
                int length          = this.termAttribute.length();
                int posIncrement    = this.posIncAttribute.getPositionIncrement();
                if(posIncrement > 0) {
                    this.previous.clear();
                }

                boolean duplicate = posIncrement == 0 && this.previous.contains(term, 0, length);
                char[] saved = new char[length];
                System.arraycopy(term, 0, saved, 0, length);
                this.previous.add(saved);
                if(duplicate) {
                    continue;
                }

                return true;
            }

            return false;
        }
    }
}
