package com.anasdidi.security;

import com.anasdidi.security.common.AppConfig;

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
  void testAppConfigSuccess(Vertx vertx, VertxTestContext testContext) {
    testContext.verify(() -> {
      AppConfig appConfig = AppConfig.instance();
      Assertions.assertNotNull(appConfig);

      Assertions.assertNotNull(appConfig.getAppPort());

      Assertions.assertNotNull(appConfig.getJwtSecret());
      Assertions.assertNotNull(appConfig.getJwtIssuer());
      Assertions.assertNotNull(appConfig.getJwtExpireInMinutes());

      Assertions.assertNotNull(appConfig.getMongoHost());
      Assertions.assertNotNull(appConfig.getMongoPort());
      Assertions.assertNotNull(appConfig.getMongoUsername());
      Assertions.assertNotNull(appConfig.getMongoPassword());
      Assertions.assertNotNull(appConfig.getMongoAuthSource());

      Assertions.assertNotNull(appConfig.getTestMongoHost());
      Assertions.assertNotNull(appConfig.getTestMongoPort());
      Assertions.assertNotNull(appConfig.getTestMongoUsename());
      Assertions.assertNotNull(appConfig.getTestMongoPassword());
      Assertions.assertNotNull(appConfig.getTestMongoAuthSource());

      testContext.completeNow();
    });
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
