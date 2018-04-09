#!/usr/bin/env bash

COLLECTION=$1
SIM=$2

solr-7.2.1/bin/solr delete -c $COLLECTION

echo "Creating collection: $COLLECTION"
solr-7.2.1/bin/solr create -c $COLLECTION

echo "Disable Data Driven schema functionality"
curl http://localhost:8983/solr/$COLLECTION/config -d '{"set-user-property": {"update.autoCreateFields":"false"}}'

echo "Creating type definitions for topics"
# https://lucene.apache.org/solr/7_2_1//solr-core/org/apache/solr/schema/SimilarityFactory.html
curl -X POST -H 'Content-type:application/json' --data-binary '{"add-field-type" : {"name":"topics_classic","class":"solr.TextField","positionIncrementGap":"100","analyzer" : {"tokenizer":{"class":"solr.WhitespaceTokenizerFactory" },"filters":[{"class":"solr.LowerCaseFilterFactory" }]},"similarity":{"class":"solr.ClassicSimilarityFactory"}}}' http://localhost:8983/solr/$COLLECTION/schema
curl -X POST -H 'Content-type:application/json' --data-binary '{"add-field-type" : {"name":"topics_bm25","class":"solr.TextField","positionIncrementGap":"100","analyzer" : {"tokenizer":{"class":"solr.WhitespaceTokenizerFactory" },"filters":[{"class":"solr.LowerCaseFilterFactory" }]},"similarity":{"class":"solr.BM25SimilarityFactory"}}}' http://localhost:8983/solr/$COLLECTION/schema

echo "Defining field ID"
curl -X POST -H 'Content-type:application/json' --data-binary '{"replace-field":{"name":"id","type":"string","stored":true }}' http://localhost:8983/solr/$COLLECTION/schema

echo "Adding field NAME"
curl -X POST -H 'Content-type:application/json' --data-binary '{"add-field":{"name":"name","type":"text_en","stored":true }}' http://localhost:8983/solr/$COLLECTION/schema
echo "Adding field TYPE"
curl -X POST -H 'Content-type:application/json' --data-binary '{"add-field":{"name":"type","type":"text_en","stored":true }}' http://localhost:8983/solr/$COLLECTION/schema
echo "Adding field ABOUT with similarity: $SIM"
curl -X POST -H 'Content-type:application/json' --data-binary '{"add-field":{"name":"about","type":"topics_'$SIM'","indexed":true, "omitNorms":true, "omitPositions":true, "stored":true, "termVectors":true }}' http://localhost:8983/solr/$COLLECTION/schema



curl -X GET -H 'Content-type:application/json' http://localhost:8983/solr/$COLLECTION/schema/fields
