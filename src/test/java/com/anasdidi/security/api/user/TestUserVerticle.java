package com.anasdidi.security.api.user;

import java.time.Instant;
import java.util.UUID;

import com.anasdidi.security.MainVerticle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestUserVerticle {

  private JsonObject createdBody;
  private WebClient webClient;
  private MongoClient mongoClient;

  private JsonObject generateRequestBody() {
    return new JsonObject()//
        .put("username", System.currentTimeMillis() + "username")//
        .put("password", System.currentTimeMillis() + "password")//
        .put("fullName", System.currentTimeMillis() + "fullName")//
        .put("email", System.currentTimeMillis() + "email");
  }

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    ConfigRetriever config = ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

    config.rxGetConfig().subscribe(cfg -> {
      webClient = WebClient.create(vertx);

      mongoClient = MongoClient.createShared(vertx, new JsonObject()//
          .put("host", cfg.getString("TEST_MONGO_HOST"))//
          .put("port", cfg.getInteger("TEST_MONGO_PORT"))//
          .put("username", cfg.getString("TEST_MONGO_USERNAME"))//
          .put("password", cfg.getString("TEST_MONGO_PASSWORD"))//
          .put("authSource", cfg.getString("TEST_MONGO_AUTH_SOURCE"))//
          .put("db_name", "security"));

      String uuid = UUID.randomUUID().toString().replace("-", "");
      createdBody = new JsonObject()//
          .put("_id", uuid)//
          .put("username", uuid)//
          .put("password", uuid)//
          .put("fullName", uuid)//
          .put("email", uuid)//
          .put("version", 0);

      mongoClient.rxSave("users", createdBody).defaultIfEmpty(uuid).subscribe(docId -> {
        createdBody.put("id", docId);
        vertx.deployVerticle(new MainVerticle(true), testContext.succeeding(id -> testContext.completeNow()));
      }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testUserCreateSuccess(Vertx vertx, VertxTestContext testContext) {
    JsonObject requestBody = generateRequestBody();

    webClient.post(5000, "localhost", "/api/users").rxSendJsonObject(requestBody).subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);

        JsonObject status = responseBody.getJsonObject("status");
        Assertions.assertNotNull(status);
        Assertions.assertEquals(true, status.getBoolean("isSuccess"));
        Assertions.assertEquals("Record successfully created.", status.getString("message"));

        JsonObject data = responseBody.getJsonObject("data");
        Assertions.assertNotNull(data);
        Assertions.assertNotNull(data.getString("id"));

        testContext.completeNow();
      });
    }, e -> testContext.failNow(e));
  }

  @Test
  void testUserCreateValidationError(Vertx vertx, VertxTestContext testContext) {
    JsonObject requestBody = generateRequestBody();
    requestBody.put("fullName", "").put("email", "");

    webClient.post(5000, "localhost", "/api/users").rxSendJsonObject(requestBody).subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
  void testUserCreateServiceError(Vertx vertx, VertxTestContext testContext) {
    webClient.post(5000, "localhost", "/api/users").rxSendJsonObject(createdBody).subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
  }

  @Test
  void testUserCreateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    webClient.post(5000, "localhost", "/api/users").rxSend().subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
  void testUserUpdateSuccess(Vertx vertx, VertxTestContext testContext) {
    webClient.put(5000, "localhost", "/api/users/" + createdBody.getString("id")).rxSendJsonObject(createdBody)
        .subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Accept"));
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
            Assertions.assertEquals(createdBody.getString("id"), data.getString("id"));

            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
  }

  @Test
  void testUserUpdateValidationError(Vertx vertx, VertxTestContext testContext) {
    createdBody.put("fullName", "").put("email", "");

    webClient.put(5000, "localhost", "/api/users/" + createdBody.getString("id")).rxSendJsonObject(createdBody)
        .subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Accept"));
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
  void testUserUpdateNotFoundError(Vertx vertx, VertxTestContext testContext) {
    createdBody.put("version", -1);

    webClient.put(5000, "localhost", "/api/users/" + createdBody.getString("id")).rxSendJsonObject(createdBody)
        .subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Accept"));
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
  void testUserUpdateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    webClient.put(5000, "localhost", "/api/users/" + createdBody.getString("id")).rxSend().subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
  void testUserDeleteSuccess(Vertx vertx, VertxTestContext testContext) {
    webClient.delete(5000, "localhost", "/api/users/" + createdBody.getString("id")).rxSendJsonObject(createdBody)
        .subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Accept"));
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
            Assertions.assertEquals(createdBody.getString("id"), data.getString("id"));

            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
  }
}
