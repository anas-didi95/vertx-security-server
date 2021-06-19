package com.anasdidi.security.common;

import org.junit.jupiter.api.Assertions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.client.HttpResponse;

public class TestUtils {

  public static void testResponseHeader(HttpResponse<Buffer> response, int statusCode) {
    Assertions.assertEquals(statusCode, response.statusCode());
    Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
  }

  public static MongoClient getMongoClient(Vertx vertx, String connectionString) {
    return MongoClient.create(vertx, new JsonObject()//
        .put("connection_string", connectionString));
  }
}
