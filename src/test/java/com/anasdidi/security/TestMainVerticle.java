package com.anasdidi.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  private ConfigRetriever configRetriever;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    this.configRetriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()//
        .addStore(new ConfigStoreOptions()//
            .setType("env")));

    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @Order(1)
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  @Order(2)
  void testConfigEnvironmentVariable(Vertx vertx, VertxTestContext testContext) {
    configRetriever.rxGetConfig().subscribe(cfg -> {
      testContext.verify(() -> {
        Assertions.assertNotNull(cfg.getString("MONGO_HOST"));
        Assertions.assertNotNull(cfg.getInteger("MONGO_PORT"));
        Assertions.assertNotNull(cfg.getString("MONGO_USERNAME"));
        Assertions.assertNotNull(cfg.getString("MONGO_PASSWORD"));
        Assertions.assertNotNull(cfg.getString("MONGO_AUTH_SOURCE"));

        Assertions.assertNotNull(cfg.getString("TEST_MONGO_HOST"));
        Assertions.assertNotNull(cfg.getInteger("TEST_MONGO_PORT"));
        Assertions.assertNotNull(cfg.getString("TEST_MONGO_USERNAME"));
        Assertions.assertNotNull(cfg.getString("TEST_MONGO_PASSWORD"));
        Assertions.assertNotNull(cfg.getString("TEST_MONGO_AUTH_SOURCE"));

        testContext.completeNow();
      });
    }, e -> testContext.failNow(e));
  }

  @Test
  @Order(3)
  void testMongoConfigureSuccess(Vertx vertx, VertxTestContext testContext) {
    String collectionName = "TestCollection";

    configRetriever.rxGetConfig().subscribe(cfg -> {
      MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject()//
          .put("host", cfg.getString("MONGO_HOST"))//
          .put("port", cfg.getInteger("MONGO_PORT"))//
          .put("username", cfg.getString("MONGO_USERNAME"))//
          .put("password", cfg.getString("MONGO_PASSWORD"))//
          .put("authSource", cfg.getString("MONGO_AUTH_SOURCE"))//
          .put("db_name", "security"));

      mongoClient.rxCreateCollection(collectionName).subscribe(() -> {
        mongoClient.rxDropCollection(collectionName).subscribe(() -> {
          testContext.completeNow();
        }, e -> testContext.failNow(e));
      }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

}
