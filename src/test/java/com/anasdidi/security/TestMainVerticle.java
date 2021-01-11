package com.anasdidi.security;

import com.anasdidi.security.common.AppConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(true),
        testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void testAppConfigSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();

    testContext.verify(() -> {
      Assertions.assertNotNull(appConfig);
      Assertions.assertNotNull(appConfig.getAppPort());
      Assertions.assertNotNull(appConfig.getAppHost());
      Assertions.assertNotNull(appConfig.getJwtSecret());
      Assertions.assertNotNull(appConfig.getJwtIssuer());
      Assertions.assertNotNull(appConfig.getJwtExpireInMinutes());
      Assertions.assertNotNull(appConfig.getJwtPermissionKey());
      Assertions.assertNotNull(appConfig.getRefreshTokenExpireInMinutes());
      Assertions.assertNotNull(appConfig.getMongoConfig());
      Assertions.assertNotNull(appConfig.getGraphiqlIsEnable());

      testContext.completeNow();
    });
  }

  @Test
  void testMongoConfigureSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    MongoClient mongoClient = MongoClient.createShared(vertx, appConfig.getMongoConfig());

    mongoClient.rxGetCollections().subscribe(resultList -> {
      testContext.verify(() -> {
        Assertions.assertNotNull(resultList);

        testContext.completeNow();
      });
    }, e -> testContext.failNow(e));
  }
}
