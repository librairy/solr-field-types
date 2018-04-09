#!/usr/bin/env bash

# Solr Admin UI
# http://localhost:8983/solr


# Solr Query UI
# http://localhost:8983/solr/topTopics/browse
# http://localhost:8983/solr/allTopics/browse
#
#  Rank Queries
#  https://opensourceconnections.com/blog/2015/10/16/bm25-the-next-generation-of-lucene-relevation/


echo "sort documents by topic"
curl 'http://localhost:8983/solr/tw-tfidf/select?fl=id,name,termfreq(about,t1@m1),termfreq(about,t2@m1)&fq=about:t1@m1%20and%20about:t2@m1&q=*:*&sort=termfreq(about,t1@m1)%20desc,termfreq(about,t2@m1)%20desc'


echo "sort documents by topics"


echo "similar documents"
# MoreLikeThis https://lucene.apache.org/solr/guide/6_6/morelikethis.html