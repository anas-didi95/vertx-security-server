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
    Assertions.assertNotNull(responseBody.getString("traceId"));
    Assertions.assertTrue(!responseBody.getJsonArray("errors").isEmpty());
  }

  public static MongoClient getMongoClient(Vertx vertx) {
    ApplicationConfig config = ApplicationConfig.instance();
    return MongoClient.create(vertx,
        new JsonObject().put("connection_string", config.getMongoConnectionString()));
  }

  public static String getRequestURI(String baseURI, String... paths) {
    String requestURI = baseURI;

    if (paths.length > 0) {
      requestURI += "/" + String.join("/", paths);
    }

    return requestURI;
  }

  public static JsonObject generateUserJson() {
    String suffix = ":" + System.currentTimeMillis();
    return new JsonObject().put("username", "username" + suffix)
        .put("password", "password" + suffix).put("fullName", "fullName" + suffix)
        .put("email", "email" + suffix).put("telegramId", "telegramId" + suffix).put("version", 0);
  }

  public static HttpRequest<Buffer> doPostRequest(Vertx vertx, String requestURI) {
    return sendRequest(vertx, HttpMethod.POST, requestURI);
  }

  public static HttpRequest<Buffer> doPutRequest(Vertx vertx, String requestURI) {
    return sendRequest(vertx, HttpMethod.PUT, requestURI);
  }

  public static HttpRequest<Buffer> doDeleteRequest(Vertx vertx, String requestURI) {
    return sendRequest(vertx, HttpMethod.DELETE, requestURI);
  }

  public static HttpRequest<Buffer> doGetRequest(Vertx vertx, String requestURI,
      String accessToken) {
    return sendRequest(vertx, HttpMethod.GET, requestURI, accessToken);
  }

  private static HttpRequest<Buffer> sendRequest(Vertx vertx, HttpMethod method,
      String requestURI) {
    return sendRequest(vertx, method, requestURI, null);
  }

  private static HttpRequest<Buffer> sendRequest(Vertx vertx, HttpMethod method, String requestURI,
      String accessToken) {
    WebClient webClient = WebClient.create(vertx);
    ApplicationConfig config = ApplicationConfig.instance();

    Map<String, String> map = new HashMap<>();
    map.put("Accept", "application/json");
    map.put("Content-Type", "application/json");
    if (accessToken != null) {
      map.put("Authorization", "Bearer " + accessToken);
    }
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().addAll(map);

    if (method == HttpMethod.POST) {
      return webClient.post(config.getAppPort(), config.getAppHost(), requestURI)
          .putHeaders(headers);
    } else if (method == HttpMethod.PUT) {
      return webClient.put(config.getAppPort(), config.getAppHost(), requestURI)
          .putHeaders(headers);
    } else if (method == HttpMethod.DELETE) {
      return webClient.delete(config.getAppPort(), config.getAppHost(), requestURI)
          .putHeaders(headers);
    } else if (method == HttpMethod.GET) {
      return webClient.get(config.getAppPort(), config.getAppHost(), requestURI)
          .putHeaders(headers);
    } else {
      System.err.println("[sendRequest] Method not implemented! " + method);
    }

    return null;
  }
}
