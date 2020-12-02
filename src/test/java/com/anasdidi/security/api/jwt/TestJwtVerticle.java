package com.anasdidi.security.api.jwt;

import java.util.List;
import java.util.stream.Collectors;
import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.AppConfig;
import com.anasdidi.security.common.CommonConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestJwtVerticle {

  private String requestURI = CommonConstants.CONTEXT_PATH + JwtConstants.REQUEST_URI;

  private JsonObject generateDocument() {
    return new JsonObject()//
        .put("username", System.currentTimeMillis() + "username")//
        .put("password", BCrypt.hashpw("password", BCrypt.gensalt()))//
        .put("telegramId", System.currentTimeMillis() + "telegramId");
  }

  private static MongoClient getMongoClient(Vertx vertx) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    return MongoClient.createShared(vertx, appConfig.getMongoConfig());
  }

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(true),
        testContext.succeeding(id -> testContext.completeNow()));
  }

  @AfterAll
  static void postTesting(Vertx vertx, VertxTestContext testContext) throws Exception {
    MongoClient mongoClient = getMongoClient(vertx);
    Single<MongoClientDeleteResult> delUsers =
        mongoClient.rxRemoveDocuments("users", new JsonObject()).toSingle();
    Single<MongoClientDeleteResult> delJwts =
        mongoClient.rxRemoveDocuments(JwtConstants.COLLECTION_NAME, new JsonObject()).toSingle();

    Single.zip(delUsers, delJwts, (r1, r2) -> true).subscribe(result -> {
      testContext.verify(() -> {
        Assertions.assertEquals(true, result);
        testContext.completeNow();
      });
    }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtLoginSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    MongoClient mongoClient = getMongoClient(vertx);
    WebClient webClient = WebClient.create(vertx);
    JsonObject user = generateDocument();

    mongoClient.rxSave("users", user).subscribe(id -> {
      user.put("password", "password");
      webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login")
          .rxSendJsonObject(user).subscribe(response -> {
            testContext.verify(() -> {
              Assertions.assertEquals(200, response.statusCode());
              Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
              Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
              Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
              Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
              Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

              JsonObject responseBody = response.bodyAsJsonObject();
              Assertions.assertNotNull(responseBody);

              // status
              JsonObject status = responseBody.getJsonObject("status");
              Assertions.assertEquals(true, status.getBoolean("isSuccess"));
              Assertions.assertEquals("User successfully validated.", status.getString("message"));

              // data
              JsonObject data = responseBody.getJsonObject("data");
              Assertions.assertNotNull(data);
              Assertions.assertNotNull(data.getString("accessToken"));

              // cookie
              List<String> cookies = response.cookies();
              Assertions.assertEquals(true, !cookies.isEmpty());

              webClient.get(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/check")
                  .putHeader("Authorization", "Bearer " + data.getString("accessToken")).rxSend()
                  .subscribe(ping -> {
                    testContext.verify(() -> {
                      Assertions.assertEquals(200, ping.statusCode());
                      Assertions.assertNotNull(ping.bodyAsJsonObject());
                      testContext.completeNow();
                    });
                  }, e -> testContext.failNow(e));
            });
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtLoginValidationError(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    JsonObject user = new JsonObject();
    user.put("username", "");

    webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login")
        .rxSendJsonObject(user).subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(400, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
            Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
            Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
            Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
            Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

            JsonObject responseBody = response.bodyAsJsonObject();
            Assertions.assertNotNull(responseBody);

            // status
            JsonObject status = responseBody.getJsonObject("status");
            Assertions.assertEquals(false, status.getBoolean("isSuccess"));
            Assertions.assertEquals("Validation error!", status.getString("message"));

            // data
            JsonObject data = responseBody.getJsonObject("data");
            Assertions.assertNotNull(data);
            Assertions.assertNotNull(data.getString("requestId"));
            Assertions.assertNotNull(data.getInstant("instant"));
            Assertions.assertNotNull(data.getJsonArray("errorList"));
            Assertions.assertTrue(!data.getJsonArray("errorList").isEmpty());

            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtLoginInvalidCredentialError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    JsonObject user = generateDocument();

    webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login")
        .rxSendJsonObject(user).subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(400, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
            Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
            Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
            Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
            Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

            JsonObject responseBody = response.bodyAsJsonObject();
            Assertions.assertNotNull(responseBody);

            // status
            JsonObject status = responseBody.getJsonObject("status");
            Assertions.assertNotNull(status);
            Assertions.assertEquals(false, status.getBoolean("isSuccess"));
            Assertions.assertEquals("Invalid credential!", status.getString("message"));

            // data
            JsonObject data = responseBody.getJsonObject("data");
            Assertions.assertNotNull(data);
            Assertions.assertNotNull(data.getString("requestId"));
            Assertions.assertNotNull(data.getInstant("instant"));
            Assertions.assertNotNull(data.getJsonArray("errorList"));
            Assertions.assertTrue(!data.getJsonArray("errorList").isEmpty());

            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtLoginRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);

    webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login").rxSend()
        .subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(400, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
            Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
            Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
            Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
            Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

            JsonObject responseBody = response.bodyAsJsonObject();
            Assertions.assertNotNull(responseBody);

            // status
            JsonObject status = responseBody.getJsonObject("status");
            Assertions.assertNotNull(status);
            Assertions.assertEquals(false, status.getBoolean("isSuccess"));
            Assertions.assertEquals("Request failed!", status.getString("message"));

            // data
            JsonObject data = responseBody.getJsonObject("data");
            Assertions.assertNotNull(data);
            Assertions.assertNotNull(data.getString("requestId"));
            Assertions.assertNotNull(data.getInstant("instant"));
            Assertions.assertNotNull(data.getJsonArray("errorList"));
            Assertions.assertTrue(!data.getJsonArray("errorList").isEmpty());

            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtLogoutSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    MongoClient mongoClient = getMongoClient(vertx);
    WebClient webClient = WebClient.create(vertx);
    JsonObject user = generateDocument();

    mongoClient.rxSave("users", user).subscribe(id -> {
      user.put("password", "password");
      webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login")
          .rxSendJsonObject(user).subscribe(response -> {
            testContext.verify(() -> {
              Assertions.assertEquals(200, response.statusCode());
              Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
              Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
              Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
              Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
              Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

              JsonObject responseBody = response.bodyAsJsonObject();
              Assertions.assertNotNull(responseBody);

              // status
              JsonObject status = responseBody.getJsonObject("status");
              Assertions.assertEquals(true, status.getBoolean("isSuccess"));
              Assertions.assertEquals("User successfully validated.", status.getString("message"));

              // data
              JsonObject data = responseBody.getJsonObject("data");
              Assertions.assertNotNull(data);
              Assertions.assertNotNull(data.getString("accessToken"));

              webClient.get(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/logout")
                  .putHeader("Authorization", "Bearer " + data.getString("accessToken")).rxSend()
                  .subscribe(resLogout -> {
                    testContext.verify(() -> {
                      Assertions.assertEquals(200, response.statusCode());
                      Assertions.assertEquals("application/json",
                          response.getHeader("Content-Type"));
                      Assertions.assertEquals("no-store, no-cache",
                          response.getHeader("Cache-Control"));
                      Assertions.assertEquals("nosniff",
                          response.getHeader("X-Content-Type-Options"));
                      Assertions.assertEquals("1; mode=block",
                          response.getHeader("X-XSS-Protection"));
                      Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

                      JsonObject resBodyLogout = resLogout.bodyAsJsonObject();
                      Assertions.assertNotNull(resBodyLogout);

                      // status
                      JsonObject statusLogout = resBodyLogout.getJsonObject("status");
                      Assertions.assertNotNull(statusLogout);
                      Assertions.assertEquals(true, statusLogout.getBoolean("isSuccess"));
                      Assertions.assertEquals("User successfully logout.",
                          statusLogout.getString("message"));

                      // data
                      JsonObject dataLogout = resBodyLogout.getJsonObject("data");
                      Assertions.assertNotNull(dataLogout);
                      Assertions.assertNotNull(dataLogout.getString("requestId"));
                      Assertions.assertNotNull(dataLogout.getString("tokenIssuedDate"));

                      List<String> cookies = resLogout.cookies().stream()
                          .filter(s -> s.contains("refreshToken")).collect(Collectors.toList());
                      Assertions.assertEquals(true, cookies.isEmpty());

                      testContext.completeNow();
                    });
                  }, e -> testContext.failNow(e));
            });
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }
}
