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
    Assertions.assertTrue(ApplicationConstants.HEADERS.entrySet().stream()
        .allMatch(entry -> entry.getValue().equals(response.getHeader(entry.getKey()))));
  }

  public static void testResponseBodyError(HttpResponse<Buffer> response, String code,
      String message) {
    JsonObject responseBody = response.bodyAsJsonObject();
    Assertions.assertNotNull(responseBody);
    Assertions.assertEquals(code, responseBody.getString("code"));
    Assertions.assertEquals(message, responseBody.getString("message"));
    Assertions.assertTrue(!responseBody.getJsonArray("errors").isEmpty());
  }

  public static MongoClient getMongoClient(Vertx vertx, String connectionString) {
    return MongoClient.create(vertx, new JsonObject()//
        .put("connection_string", connectionString));
  }
}
