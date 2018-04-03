#!/usr/bin/env bash

# Solr Boost
# https://lucene.apache.org/solr/guide/6_6/the-dismax-query-parser.html#TheDisMaxQueryParser-Thebf_BoostFunctions_Parameter


# Solr Query UI
# http://localhost:8983/solr/<collection>/browse


echo "documents by topic distribution"
curl 'http://localhost:8983/solr/tw-tfidf/select?fl=id,name,about,score&q=about:t9@m1^1.0%20t10@m1^1.0%20t11@m1^8.0%20t14@m1^24.0%20t0@m1^59.0&sort=score%20desc'