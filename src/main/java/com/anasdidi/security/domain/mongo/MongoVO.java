package com.anasdidi.security.domain.mongo;

import io.vertx.core.json.JsonObject;

class MongoVO {

  final String collection;
  final JsonObject document;

  private MongoVO(String collection, JsonObject document) {
    this.collection = collection;
    this.document = document;
  }

  static MongoVO fromJson(JsonObject json) {
    String collection = json.getString("collection");
    JsonObject document = json.getJsonObject("document");

    return new MongoVO(collection, document);
  }
}
