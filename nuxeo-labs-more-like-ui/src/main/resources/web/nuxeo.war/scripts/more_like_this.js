/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 */
function doMoreLikeThisSearch(id,query) {

  var esClient= new jQuery.es.Client({
    hosts: {
            protocol: window.location.protocol,
            host: window.location.hostname,
            port: window.location.port.length != 0 ? window.location.port : 80,
            path: '/nuxeo/site/es',
            headers: {
                'Content-Type' : 'application/json'
            }
    }
  });

  var defaultQuery = {
    "bool": {
        "should": [{
            "more_like_this" : {
             "fields" : ["dc:title.fulltext"],
             "docs" : [{
               "_index" : "nuxeo",
               "_type" : "doc",
               "_id" : id}],
             "min_term_freq" : 1,
             "min_word_length" : 5,
             "min_doc_freq" : 3,
             "boost" : 3
           }},{
           "more_like_this" : {
             "docs" : [{
               "_index" : "nuxeo",
               "_type" : "doc",
               "_id" : id}],
             "min_term_freq" : 1,
             "max_query_terms" : 25,
           }}]
    }
  };

  /* replace id placeholder by actual doc id*/
  if (query) query = query.replace("\"id\"","\""+id+"\"");

  /* if no query is provided, use default one*/
  var actualQuery = query ? JSON.parse(query) : defaultQuery;

  esClient.search({
    index: 'nuxeo',
    body: {
     fields: ["_source"],
     size : 3,
     query : actualQuery
    }
  }).then(callbackSearch, function (err) {
      console.trace(err.message);
  });
};

function callbackSearch(resp) {
    resp.hits.hits.forEach(function(doc) {
        var source = doc["_source"];
        var id = doc["_id"];
        var root = jQuery('#similarDocs');

        var style =
            "background-image:url('"+
            window.location.protocol+'//'+
            window.location.host+'/nuxeo/nxthumb/default/'+
            id+"/blobholder:0/"+
            "');";
        var img = jQuery('<div>').addClass('thumbnailContainer').attr('style',style);

        var link = window.location.protocol+'//'+
            window.location.host+'/nuxeo/nxpath/'+
            source["ecm:repository"]+
            source["ecm:path"]+"@view_documents?tabIds=%3A";

        var href = jQuery('<a>').attr('href',link).text(source["dc:title"]);

        var mainDiv = jQuery('<div>').addClass('bubbleBox bubbleListing');

        img.appendTo(mainDiv);
        href.appendTo(mainDiv);
        mainDiv.appendTo(root);
    });
};
