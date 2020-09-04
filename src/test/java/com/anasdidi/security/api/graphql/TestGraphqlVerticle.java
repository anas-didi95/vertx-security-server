package com.anasdidi.security.api.graphql;

import com.anasdidi.security.MainVerticle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestGraphqlVerticle {

  private int port;
  private String host;
  private String requestURI = "/api/graphql";
  private WebClient webClient;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    ConfigRetriever config = ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

    config.rxGetConfig().subscribe(cfg -> {
      port = cfg.getInteger("APP_PORT");
      host = cfg.getString("APP_HOST", "localhost");
      webClient = WebClient.create(vertx);

      vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testGraphqlSuccess(Vertx vertx, VertxTestContext testContext) {
    JsonObject requestBody = new JsonObject()//
        .put("query", "query { getUser { id } }")//
        .put("variables", new JsonObject());

    webClient.post(port, host, requestURI).rxSendJsonObject(requestBody).subscribe(response -> {
      testContext.verify(() -> {
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("application/json", response.getHeader("Accept"));
        Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);

        testContext.completeNow();
      });
    }, e -> testContext.failNow(e));
  }
}
