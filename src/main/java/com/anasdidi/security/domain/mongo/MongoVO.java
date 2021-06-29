package com.anasdidi.security.domain.mongo;

import io.vertx.core.json.JsonObject;

class MongoVO {

  final String collection;
  final JsonObject document;
  final JsonObject query;
  final Long version;

  private MongoVO(String collection, JsonObject document, JsonObject query, Long version) {
    this.collection = collection;
    this.document = document;
    this.query = query;
    this.version = version;
  }

  static MongoVO fromJson(JsonObject json) {
    String collection = json.getString("collection");
    JsonObject document = json.getJsonObject("document");
    JsonObject query = json.getJsonObject("query");
    Long version = json.getLong("version");

    return new MongoVO(collection, document, query, version);
  }
}
