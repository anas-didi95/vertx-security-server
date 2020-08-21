package com.anasdidi.api.user;

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
  void testCanRouteToUser(Vertx vertx, VertxTestContext testContext) {
    webClient.get(5000, "localhost", "/api/users").rxSend().subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);

        String body = responseBody.getString("data");
        Assertions.assertEquals("Hello world", body);

        testContext.completeNow();
      });
    }, e -> testContext.failNow(e));
  }

}
