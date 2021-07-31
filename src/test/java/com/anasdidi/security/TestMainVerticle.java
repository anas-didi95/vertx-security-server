package com.anasdidi.security;

import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.mongo.MongoClient;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  private final String baseURI = ApplicationConstants.CONTEXT_PATH;

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
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);

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
    Assertions.assertNotNull(config.getJwtSecret());
    Assertions.assertNotNull(config.getJwtIssuer());
    Assertions.assertNotNull(config.getJwtPermissionsKey());
    Assertions.assertNotNull(config.getJwtAccessTokenExpireInMinutes());
    Assertions.assertNotNull(config.getJwtRefreshTokenExpireInMinutes());
    Assertions.assertNotNull(config.getGraphiqlEnable());
    Assertions.assertNotNull(config.getCorsOrigins());
    testContext.completeNow();
  }

  @Test
  void testHealthEndpointSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint();

    TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "health")).rxSend()
        .subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(200, response.statusCode());
            checkpoint.flag();
          });

          testContext.verify(() -> {
            JsonObject responseBody = response.bodyAsJsonObject();
            Assertions.assertNotNull(responseBody);
            Assertions.assertEquals("UP", responseBody.getString("outcome"));
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }
}
