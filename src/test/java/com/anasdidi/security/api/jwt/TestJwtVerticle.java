package com.anasdidi.security.api.jwt;

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
              Assertions.assertNotNull(data.getString("refreshToken"));

              testContext.completeNow();
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
          .rxSendJsonObject(user).subscribe(token -> {
            String accessToken =
                token.bodyAsJsonObject().getJsonObject("data").getString("accessToken");

            webClient.get(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/logout")
                .putHeader("Authorization", "Bearer " + accessToken).rxSend()
                .subscribe(response -> {
                  testContext.verify(() -> {
                    Assertions.assertEquals(200, response.statusCode());
                    Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
                    Assertions.assertEquals("no-store, no-cache",
                        response.getHeader("Cache-Control"));
                    Assertions.assertEquals("nosniff",
                        response.getHeader("X-Content-Type-Options"));
                    Assertions.assertEquals("1; mode=block",
                        response.getHeader("X-XSS-Protection"));
                    Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

                    JsonObject responseBody = response.bodyAsJsonObject();
                    Assertions.assertNotNull(responseBody);

                    // status
                    JsonObject status = responseBody.getJsonObject("status");
                    Assertions.assertNotNull(status);
                    Assertions.assertEquals(true, status.getBoolean("isSuccess"));
                    Assertions.assertEquals("User successfully logout.",
                        status.getString("message"));

                    // data
                    JsonObject data = responseBody.getJsonObject("data");
                    Assertions.assertNotNull(data);
                    Assertions.assertNotNull(data.getInstant("lastTokenDate"));

                    testContext.completeNow();
                  });
                }, e -> testContext.failNow(e));
          });
    }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtRefreshTokenSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    MongoClient mongoClient = getMongoClient(vertx);
    WebClient webClient = WebClient.create(vertx);
    JsonObject user = generateDocument();

    mongoClient.rxSave("users", user).subscribe(id -> {
      user.put("password", "password");

      webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login")
          .rxSendJsonObject(user).subscribe(token -> {
            JsonObject tokenBody = token.bodyAsJsonObject().getJsonObject("data");
            String accessToken = tokenBody.getString("accessToken");
            String refreshToken = tokenBody.getString("refreshToken");
            JsonObject requestBody = new JsonObject().put("refreshToken", refreshToken);

            Thread.sleep(2000);
            webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/refresh")
                .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(requestBody)
                .subscribe(response -> {
                  testContext.verify(() -> {
                    Assertions.assertEquals(200, response.statusCode());
                    Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
                    Assertions.assertEquals("no-store, no-cache",
                        response.getHeader("Cache-Control"));
                    Assertions.assertEquals("nosniff",
                        response.getHeader("X-Content-Type-Options"));
                    Assertions.assertEquals("1; mode=block",
                        response.getHeader("X-XSS-Protection"));
                    Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

                    JsonObject responseBody = response.bodyAsJsonObject();
                    Assertions.assertNotNull(responseBody);

                    // status
                    JsonObject status = responseBody.getJsonObject("status");
                    Assertions.assertNotNull(status);
                    Assertions.assertEquals(true, status.getBoolean("isSuccess"));
                    Assertions.assertEquals("Token refreshed.", status.getString("message"));

                    // data
                    JsonObject data = responseBody.getJsonObject("data");
                    Assertions.assertNotNull(data);
                    Assertions.assertNotNull(data.getString("accessToken"));
                    Assertions.assertNotNull(data.getString("refreshToken"));
                    Assertions.assertNotEquals(accessToken, data.getString("accessToken"));
                    Assertions.assertNotEquals(refreshToken, data.getString("refreshToken"));

                    testContext.completeNow();
                  });
                }, e -> testContext.failNow(e));
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtRefreshTokenValidationError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    MongoClient mongoClient = getMongoClient(vertx);
    WebClient webClient = WebClient.create(vertx);
    JsonObject user = generateDocument();

    mongoClient.rxSave("users", user).subscribe(id -> {
      user.put("password", "password");

      webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login")
          .rxSendJsonObject(user).subscribe(token -> {
            JsonObject tokenBody = token.bodyAsJsonObject().getJsonObject("data");
            String accessToken = tokenBody.getString("accessToken");
            JsonObject requestBody = new JsonObject().put("refreshToken", "");

            Thread.sleep(2000);
            webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/refresh")
                .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(requestBody)
                .subscribe(response -> {
                  testContext.verify(() -> {
                    Assertions.assertEquals(400, response.statusCode());
                    Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
                    Assertions.assertEquals("no-store, no-cache",
                        response.getHeader("Cache-Control"));
                    Assertions.assertEquals("nosniff",
                        response.getHeader("X-Content-Type-Options"));
                    Assertions.assertEquals("1; mode=block",
                        response.getHeader("X-XSS-Protection"));
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
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtRefreshTokenRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    MongoClient mongoClient = getMongoClient(vertx);
    WebClient webClient = WebClient.create(vertx);
    JsonObject user = generateDocument();

    mongoClient.rxSave("users", user).subscribe(id -> {
      user.put("password", "password");

      webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login")
          .rxSendJsonObject(user).subscribe(token -> {
            JsonObject tokenBody = token.bodyAsJsonObject().getJsonObject("data");
            String accessToken = tokenBody.getString("accessToken");

            Thread.sleep(2000);
            webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/refresh")
                .putHeader("Authorization", "Bearer " + accessToken).rxSend()
                .subscribe(response -> {
                  testContext.verify(() -> {
                    Assertions.assertEquals(400, response.statusCode());
                    Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
                    Assertions.assertEquals("no-store, no-cache",
                        response.getHeader("Cache-Control"));
                    Assertions.assertEquals("nosniff",
                        response.getHeader("X-Content-Type-Options"));
                    Assertions.assertEquals("1; mode=block",
                        response.getHeader("X-XSS-Protection"));
                    Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

                    JsonObject responseBody = response.bodyAsJsonObject();
                    Assertions.assertNotNull(responseBody);

                    // status
                    JsonObject status = responseBody.getJsonObject("status");
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
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtCheckSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    MongoClient mongoClient = getMongoClient(vertx);
    WebClient webClient = WebClient.create(vertx);
    JsonObject user = generateDocument();

    mongoClient.rxSave("users", user).subscribe(id -> {
      user.put("password", "password");
      webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/login")
          .rxSendJsonObject(user).subscribe(login -> {
            String accessToken =
                login.bodyAsJsonObject().getJsonObject("data").getString("accessToken");

            webClient.get(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/check")
                .putHeader("Authorization", "Bearer " + accessToken).rxSend()
                .subscribe(response -> {
                  testContext.verify(() -> {
                    Assertions.assertEquals(200, response.statusCode());
                    Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
                    Assertions.assertEquals("no-store, no-cache",
                        response.getHeader("Cache-Control"));
                    Assertions.assertEquals("nosniff",
                        response.getHeader("X-Content-Type-Options"));
                    Assertions.assertEquals("1; mode=block",
                        response.getHeader("X-XSS-Protection"));
                    Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

                    JsonObject responseBody = response.bodyAsJsonObject();
                    Assertions.assertNotNull(responseBody);

                    JsonObject status = responseBody.getJsonObject("status");
                    Assertions.assertNotNull(status);
                    Assertions.assertEquals(true, status.getBoolean("isSuccess"));
                    Assertions.assertEquals("Token decoded.", status.getString("message"));

                    JsonObject data = responseBody.getJsonObject("data");
                    Assertions.assertNotNull(data);
                    Assertions.assertNotNull(data.getString("userId"));
                    Assertions.assertNotNull(data.getString("username"));

                    testContext.completeNow();
                  });
                });
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }
}
