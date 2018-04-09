package org.librairy.solr.factories;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class DocumentFactory {


    public static Document newDocTopic(String vectorString, String fieldName) {
        Document testDoc = new Document();
        FieldType fieldType = new FieldType(TextField.TYPE_STORED);//TYPE_NOT_STORED
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setStoreTermVectors(true);
        Field textField = new Field(fieldName, vectorString, fieldType);
        testDoc.add(textField);
        return testDoc;
    }

}
