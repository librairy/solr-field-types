#!/usr/bin/env bash

# Solr Wiki
# https://wiki.apache.org/solr/MoreLikeThis


# Solr Query UI
# http://localhost:8983/solr/<collection>/browse


echo "mlt documents by topic"
curl 'http://localhost:8983/solr/tw-tfidf/select?fl=id,name,score&mlt.count=10&mlt.fl=about&mlt=true&q=id:5227dcc6bfbe38d7288b6980&sort=score%20desc'