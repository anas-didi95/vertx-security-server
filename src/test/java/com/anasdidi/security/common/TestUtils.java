package com.anasdidi.security.common;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.MultiMap;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.HttpResponse;
import io.vertx.rxjava3.ext.web.client.WebClient;

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

  public static MongoClient getMongoClient(Vertx vertx) {
    ApplicationConfig config = ApplicationConfig.instance();
    return MongoClient.create(vertx,
        new JsonObject().put("connection_string", config.getMongoConnectionString()));
  }

  public static JsonObject generateUserJson() {
    String suffix = ":" + System.currentTimeMillis();
    return new JsonObject().put("username", "username" + suffix)
        .put("password", "password" + suffix).put("fullName", "fullName" + suffix)
        .put("email", "email" + suffix).put("telegramId", "telegramId" + suffix);
  }

  public static HttpRequest<Buffer> doPostRequest(Vertx vertx, String requestURI) {
    return sendRequest(vertx, HttpMethod.POST, requestURI);
  }

  private static HttpRequest<Buffer> sendRequest(Vertx vertx, HttpMethod method,
      String requestURI) {
    WebClient webClient = WebClient.create(vertx);
    ApplicationConfig config = ApplicationConfig.instance();

    Map<String, String> map = new HashMap<>();
    map.put("Accept", "application/json");
    map.put("Content-Type", "application/json");
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().addAll(map);

    if (method == HttpMethod.POST) {
      return webClient.post(config.getAppPort(), config.getAppHost(), requestURI)
          .putHeaders(headers);
    }

    return null;
  }
}
