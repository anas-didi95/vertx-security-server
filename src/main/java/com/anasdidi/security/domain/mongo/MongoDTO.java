package com.anasdidi.security.domain.mongo;

import io.vertx.core.json.JsonObject;

class MongoDTO {

  final String collection;
  final JsonObject document;

  private MongoDTO(String collection, JsonObject document) {
    this.collection = collection;
    this.document = document;
  }

  static MongoDTO fromJson(JsonObject json) {
    String collection = json.getString("collection");
    JsonObject document = json.getJsonObject("document");

    return new MongoDTO(collection, document);
  }
}
