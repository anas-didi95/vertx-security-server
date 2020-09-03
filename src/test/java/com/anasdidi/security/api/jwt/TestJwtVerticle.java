package com.anasdidi.security.api.jwt;

import java.util.UUID;

import com.anasdidi.security.MainVerticle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestJwtVerticle {

  private int port;
  private String host;
  private String requestURI = "/api/jwt";
  private WebClient webClient;
  private JsonObject user;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()//
        .addStore(new ConfigStoreOptions()//
            .setType("env")));

    configRetriever.rxGetConfig().subscribe(cfg -> {
      port = cfg.getInteger("APP_PORT", 5000);
      host = cfg.getString("APP_HOST", "localhost");
      webClient = WebClient.create(vertx);
      MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject()//
          .put("host", cfg.getString("TEST_MONGO_HOST"))//
          .put("port", cfg.getInteger("TEST_MONGO_PORT"))//
          .put("username", cfg.getString("TEST_MONGO_USERNAME"))//
          .put("password", cfg.getString("TEST_MONGO_PASSWORD"))//
          .put("authSource", cfg.getString("TEST_MONGO_AUTH_SOURCE"))//
          .put("db_name", "security"));

      String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
      user = new JsonObject()//
          .put("username", uuid)//
          .put("password", BCrypt.hashpw(uuid, BCrypt.gensalt()));

      mongoClient.rxSave("users", user).subscribe(docId -> {
        user.put("id", docId).put("password", uuid);
        vertx.deployVerticle(new MainVerticle(true), testContext.succeeding(id -> testContext.completeNow()));
      }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));

  }

  @Test
  void testJwtLoginSuccess(Vertx vertx, VertxTestContext testContext) {
    webClient.post(port, host, requestURI + "/login").rxSendJsonObject(user).subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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

        webClient.get(port, host, requestURI + "/check")
            .putHeader("Authorization", "Bearer " + data.getString("accessToken")).rxSend().subscribe(ping -> {
              testContext.verify(() -> {
                Assertions.assertEquals(200, ping.statusCode());
                Assertions.assertNotNull(ping.bodyAsJsonObject());
                testContext.completeNow();
              });
            }, e -> testContext.failNow(e));
      });
    }, e -> testContext.failNow(e));
  }

  @Test
  void testJwtLoginValidationError(Vertx vertx, VertxTestContext testContext) {
    user.put("username", "");

    webClient.post(port, host, requestURI + "/login").rxSendJsonObject(user).subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
  void testJwtLoginInvalidCredentialError(Vertx vertx, VertxTestContext testContext) {
    user.put("username", "" + System.currentTimeMillis());

    webClient.post(port, host, requestURI + "/login").rxSendJsonObject(user).subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

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
  void testJwtLoginRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    webClient.post(port, host, requestURI + "/login").rxSend().subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(400, response.statusCode());
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
}
