package com.anasdidi.security.api.user;

import com.anasdidi.security.MainVerticle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestUserVerticle {

  private WebClient webClient;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    webClient = WebClient.create(vertx);
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void testCanCreateUser(Vertx vertx, VertxTestContext testContext) {
    webClient.post(5000, "localhost", "/api/users").rxSend().subscribe(response -> {
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
        Assertions.assertEquals("id", data.getString("id"));

        testContext.completeNow();
      });
    }, e -> testContext.failNow(e));
  }
}
