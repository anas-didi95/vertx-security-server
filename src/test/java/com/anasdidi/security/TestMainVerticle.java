package com.anasdidi.security;

import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    }, error -> testContext.failNow(error));
  }

  @Test
  void testVerticleDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void testMongoClientGetCollectionsSuccess(Vertx vertx, VertxTestContext testContext) {
    ApplicationConfig config = ApplicationConfig.instance();
    MongoClient mongoClient = TestUtils.getMongoClient(vertx, config.getMongoConnectionString());

    mongoClient.rxGetCollections().subscribe(collectionList -> {
      testContext.verify(() -> {
        testContext.completeNow();
      });
    }, error -> {
      testContext.failNow(error);
    });
  }

  @Test
  void testApplicationConfigHasValue(Vertx vertx, VertxTestContext testContext) {
    ApplicationConfig config = ApplicationConfig.instance();
    Assertions.assertNotNull(config);
    Assertions.assertNotNull(config.getAppHost());
    Assertions.assertNotNull(config.getAppPort());
    Assertions.assertNotNull(config.getMongoConnectionString());
    testContext.completeNow();
  }
}
