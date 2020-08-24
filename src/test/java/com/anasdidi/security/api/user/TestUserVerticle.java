package com.anasdidi.security.api.user;

import com.anasdidi.security.MainVerticle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestUserVerticle {

  private JsonObject requestBody;
  private WebClient webClient;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    requestBody = new JsonObject()//
        .put("username", System.currentTimeMillis() + "username")//
        .put("password", System.currentTimeMillis() + "password")//
        .put("fullName", System.currentTimeMillis() + "fullName")//
        .put("email", System.currentTimeMillis() + "email");

    webClient = WebClient.create(vertx);
    vertx.deployVerticle(new MainVerticle(true), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void testCreateUserSuccess(Vertx vertx, VertxTestContext testContext) {
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
  void testCreateUserValidationError(Vertx vertx, VertxTestContext testContext) {
    requestBody.put("fullName", "").put("email", "");

    webClient.post(5000, "localhost", "/api/users").rxSendJsonObject(requestBody).subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content"));

        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);

        JsonObject status = responseBody.getJsonObject("status");
        Assertions.assertNotNull(status);
        Assertions.assertEquals(false, status.getBoolean("isSuccess"));
        Assertions.assertEquals("Validation error!", status.getString("message"));

        JsonArray errors = responseBody.getJsonArray("errors");
        Assertions.assertNotNull(errors);
        Assertions.assertTrue(!errors.isEmpty());

        testContext.completeNow();
      });
    }, e -> testContext.failNow(e));
  }
}
