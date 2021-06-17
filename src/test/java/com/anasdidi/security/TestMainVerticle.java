package com.anasdidi.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.mongo.MongoClient;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deployVerticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle()).subscribe(id -> {
      testContext.verify(() -> {
        Assertions.assertNotNull(id);
        testContext.completeNow();
      });
    });
  }

  @Test
  void testVerticleDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void testMongoClientGetCollectionsSuccess(Vertx vertx, VertxTestContext testContext) {
    MongoClient mongoClient = MongoClient.create(vertx, new JsonObject()//
        .put("connection_string", "mongodb://mongo:mongo@mongo:27017/security?authSource=admin"));

    mongoClient.rxGetCollections().subscribe(collectionList -> {
      testContext.verify(() -> {
        Assertions.assertTrue(!collectionList.isEmpty());
        testContext.completeNow();
      });
    }, error -> {
      testContext.failNow(error);
    });
  }
}
