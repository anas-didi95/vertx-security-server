package com.anasdidi.security.api.user;

import java.time.Instant;
import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.AppConfig;
import com.anasdidi.security.common.CommonConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestUserVerticle {

  private String requestURI = CommonConstants.CONTEXT_PATH + UserConstants.REQUEST_URI;

  // payload = { "iss": "anasdidi.dev", "permissions": ["user:write", "user:read"] },
  // secret = secret
  private String accessToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhbmFzZGlkaS5kZXYiLCJwZXJtaXNzaW9ucyI6WyJ1c2VyOndyaXRlIiwidXNlcjpyZWFkIl19.W5U_uOGHqK8bqNtLTNZjNAxHdA-G68pclLUFE8KNbqw";

  // payload = { "iss": "anasdidi.dev" },
  // secret = secret
  private String accessTokenWithoutPermission =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhbmFzZGlkaS5kZXYifQ.F5jwo_F1RkC5cSLKyKFTX2taKqRpCasfSQDMf13o5PA";

  private JsonObject generateDocument() {
    return new JsonObject()//
        .put("username", System.currentTimeMillis() + "username")//
        .put("password", System.currentTimeMillis() + "password")//
        .put("fullName", System.currentTimeMillis() + "fullName")//
        .put("email", System.currentTimeMillis() + "email")//
        .put("version", 0)//
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

    mongoClient.rxRemoveDocuments(UserConstants.COLLECTION_NAME, new JsonObject())
        .subscribe(result -> {
          testContext.verify(() -> {
            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
  }

  @Test
  void testUserCreateSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    MongoClient mongoClient = getMongoClient(vertx);
    WebClient webClient = WebClient.create(vertx);
    JsonObject requestBody = generateDocument();

    mongoClient.rxCount(UserConstants.COLLECTION_NAME, new JsonObject()).subscribe(prevCount -> {
      webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI)
          .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(requestBody)
          .subscribe(response -> {
            testContext.verify(() -> {
              Assertions.assertEquals(201, response.statusCode());
              Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
              Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
              Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
              Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
              Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

              JsonObject responseBody = response.bodyAsJsonObject();
              Assertions.assertNotNull(responseBody);

              JsonObject status = responseBody.getJsonObject("status");
              Assertions.assertNotNull(status);
              Assertions.assertEquals(true, status.getBoolean("isSuccess"));
              Assertions.assertEquals("Record successfully created.", status.getString("message"));

              JsonObject data = responseBody.getJsonObject("data");
              Assertions.assertNotNull(data);
              Assertions.assertNotNull(data.getString("id"));

              mongoClient.rxCount(UserConstants.COLLECTION_NAME, new JsonObject())
                  .subscribe(currCount -> {
                    testContext.verify(() -> {
                      // Assertions.assertNotEquals(prevCount, currCount);

                      testContext.completeNow();
                    });
                  }, e -> testContext.failNow(e));
            });
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testUserCreateValidationError(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    JsonObject requestBody = generateDocument();
    requestBody.put("fullName", "").put("email", "");

    webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI)
        .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(requestBody)
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
            Assertions.assertEquals("Validation error!", status.getString("message"));

            // data
            JsonObject data = responseBody.getJsonObject("data");
            Assertions.assertNotNull(data);

            String requestId = data.getString("requestId");
            Assertions.assertNotNull(requestId);

            JsonArray errorList = data.getJsonArray("errorList");
            Assertions.assertNotNull(errorList);
            Assertions.assertTrue(!errorList.isEmpty());

            Instant instant = data.getInstant("instant");
            Assertions.assertNotNull(instant);

            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
  }

  @Test
  void testUserCreateServiceError(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    MongoClient mongoClient = getMongoClient(vertx);
    JsonObject createdBody = generateDocument();

    // If error, start the server in development to create the index first.
    mongoClient.rxSave(UserConstants.COLLECTION_NAME, createdBody).subscribe(id -> {
      webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI)
          .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(createdBody)
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
              Assertions.assertEquals("User creation failed!", status.getString("message"));

              // data
              JsonObject data = responseBody.getJsonObject("data");
              Assertions.assertNotNull(data);

              String requestId = data.getString("requestId");
              Assertions.assertNotNull(requestId);

              JsonArray errorList = data.getJsonArray("errorList");
              Assertions.assertTrue(!errorList.isEmpty());

              Instant instant = data.getInstant("instant");
              Assertions.assertNotNull(instant);

              testContext.completeNow();
            });
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testUserCreateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);

    webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI)
        .putHeader("Authorization", "Bearer " + accessToken).rxSend().subscribe(response -> {
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
  void testUserCreateAuthorizationError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    JsonObject requestBody = generateDocument();

    webClient.post(appConfig.getAppPort(), appConfig.getAppHost(), requestURI)
        .putHeader("Authorization", "Bearer " + accessTokenWithoutPermission)
        .rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(403, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
            Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
            Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
            Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
            Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

            JsonObject responseBody = response.bodyAsJsonObject();
            Assertions.assertNotNull(responseBody);

            JsonObject status = responseBody.getJsonObject("status");
            Assertions.assertNotNull(status);
            Assertions.assertEquals(false, status.getBoolean("isSuccess"));
            Assertions.assertEquals("You are not authorized for this request!",
                status.getString("message"));

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
  void testUserUpdateSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    MongoClient mongoClient = getMongoClient(vertx);
    JsonObject createdBody = generateDocument();

    mongoClient.rxSave(UserConstants.COLLECTION_NAME, createdBody).subscribe(id -> {
      webClient.put(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/" + id)
          .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(createdBody)
          .subscribe(response -> {
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
              Assertions.assertNotNull(status);
              Assertions.assertEquals(true, status.getBoolean("isSuccess"));
              Assertions.assertEquals("Record successfully updated.", status.getString("message"));

              // data
              JsonObject data = responseBody.getJsonObject("data");
              Assertions.assertNotNull(data);
              Assertions.assertEquals(id, data.getString("id"));

              testContext.completeNow();
            });
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testUserUpdateValidationError(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    JsonObject createdBody = generateDocument().put("fullName", "");

    webClient.put(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/validIdHere")
        .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(createdBody)
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
  void testUserUpdateNotFoundError(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    JsonObject createdBody = generateDocument();

    webClient.put(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/randomIdHere")
        .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(createdBody)
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
            Assertions.assertEquals("User update failed!", status.getString("message"));

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
  void testUserUpdateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);

    webClient.put(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/validIdHere")
        .putHeader("Authorization", "Bearer " + accessToken).rxSend().subscribe(response -> {
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
  void testUserUpdateAuthorizationError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    MongoClient mongoClient = getMongoClient(vertx);
    JsonObject createdBody = generateDocument();

    mongoClient.rxSave(UserConstants.COLLECTION_NAME, createdBody).subscribe(id -> {
      webClient.put(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/" + id)
          .putHeader("Authorization", "Bearer " + accessTokenWithoutPermission)
          .rxSendJsonObject(createdBody).subscribe(response -> {
            testContext.verify(() -> {
              Assertions.assertEquals(403, response.statusCode());
              Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
              Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
              Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
              Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
              Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));

              JsonObject responseBody = response.bodyAsJsonObject();
              Assertions.assertNotNull(responseBody);

              JsonObject status = responseBody.getJsonObject("status");
              Assertions.assertNotNull(status);
              Assertions.assertEquals(false, status.getBoolean("isSuccess"));
              Assertions.assertEquals("You are not authorized for this request!",
                  status.getString("message"));

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
  }

  @Test
  void testUserDeleteSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    MongoClient mongoClient = getMongoClient(vertx);
    JsonObject createdBody = generateDocument();

    mongoClient.rxSave(UserConstants.COLLECTION_NAME, createdBody).subscribe(id -> {
      webClient.delete(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/" + id)
          .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(createdBody)
          .subscribe(response -> {
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
              Assertions.assertNotNull(status);
              Assertions.assertEquals(true, status.getBoolean("isSuccess"));
              Assertions.assertEquals("User successfully deleted.", status.getString("message"));

              // data
              JsonObject data = responseBody.getJsonObject("data");
              Assertions.assertNotNull(data);
              Assertions.assertEquals(id, data.getString("id"));

              testContext.completeNow();
            });
          }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testUserDeleteValidationError(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    JsonObject requestBody = new JsonObject().put("dummy", "");

    webClient.delete(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/validIdHere")
        .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(requestBody)
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
  void testUserDeleteNotFoundError(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    JsonObject createdBody = new JsonObject();
    createdBody.put("version", -1);

    webClient.delete(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/validIdHere")
        .putHeader("Authorization", "Bearer " + accessToken).rxSendJsonObject(createdBody)
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
            Assertions.assertEquals("User delete failed!", status.getString("message"));

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
  void testUserDeleteRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    AppConfig appConfig = AppConfig.instance();
    WebClient webClient = WebClient.create(vertx);

    webClient.delete(appConfig.getAppPort(), appConfig.getAppHost(), requestURI + "/validIdHere")
        .putHeader("Authorization", "Bearer " + accessToken).rxSend().subscribe(response -> {
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
}
