package com.anasdidi.security.domain.mongo;

import io.vertx.core.json.JsonObject;

class MongoVO {

  final String collection;
  final JsonObject document;
  final JsonObject query;

  private MongoVO(String collection, JsonObject document, JsonObject query) {
    this.collection = collection;
    this.document = document;
    this.query = query;
  }

  static MongoVO fromJson(JsonObject json) {
    String collection = json.getString("collection");
    JsonObject document = json.getJsonObject("document");
    JsonObject query = json.getJsonObject("query");

    return new MongoVO(collection, document, query);
  }
}
