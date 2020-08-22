package com.anasdidi.security;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void testMongoConfigureSuccess(Vertx vertx, VertxTestContext testContext) {
    String collectionName = "TestCollection";
    MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject()//
        .put("host", "mongo")//
        .put("port", 27017)//
        .put("username", "mongo")//
        .put("password", "mongo")//
        .put("authSource", "admin")//
        .put("db_name", "security"));

    mongoClient.rxCreateCollection(collectionName).subscribe(() -> {
      mongoClient.rxDropCollection(collectionName).subscribe(() -> {
        testContext.completeNow();
      }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }
}
