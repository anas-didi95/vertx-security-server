package com.anasdidi.security.api.graphql;

import java.util.UUID;

import com.anasdidi.security.MainVerticle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestGraphqlVerticle {

  private int port;
  private String host;
  private String requestURI = "/security/graphql";
  private WebClient webClient;
  private MongoClient mongoClient;
  private JsonObject user;
  private int waitMillis = 200;
  private String accessToken;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    ConfigRetriever config = ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

    config.rxGetConfig().subscribe(cfg -> {
      port = cfg.getInteger("APP_PORT");
      host = cfg.getString("APP_HOST", "localhost");
      webClient = WebClient.create(vertx);

      mongoClient = MongoClient.createShared(vertx, new JsonObject()//
          .put("host", cfg.getString("TEST_MONGO_HOST"))//
          .put("port", cfg.getInteger("TEST_MONGO_PORT"))//
          .put("username", cfg.getString("TEST_MONGO_USERNAME"))//
          .put("password", cfg.getString("TEST_MONGO_PASSWORD"))//
          .put("authSource", cfg.getString("TEST_MONGO_AUTH_SOURCE"))//
          .put("db_name", "security"));

      String uuid = UUID.randomUUID().toString().replace("-", "");
      user = new JsonObject()//
          .put("_id", uuid)//
          .put("username", uuid)//
          .put("password", uuid)//
          .put("fullName", uuid)//
          .put("email", uuid)//
          .put("version", 0);

      @SuppressWarnings("deprecation")
      JWTAuth jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()//
          .setJWTOptions(new JWTOptions()//
              .setExpiresInMinutes(cfg.getInteger("JWT_EXPIRE_IN_MINUTES"))//
              .setIssuer(cfg.getString("JWT_ISSUER")))//
          .addPubSecKey(new PubSecKeyOptions()//
              .setAlgorithm("HS256")//
              .setPublicKey(cfg.getString("JWT_SECRET"))//
              .setSymmetric(true)));
      accessToken = jwtAuth.generateToken(new JsonObject(), new JWTOptions()//
          .setIssuer(cfg.getString("JWT_ISSUER"))//
          .setExpiresInMinutes(cfg.getInteger("JWT_EXPIRE_IN_MINUTES")));

      mongoClient.rxSave("users", user).defaultIfEmpty(uuid).subscribe(docId -> {
        user.put("id", docId);
        vertx.deployVerticle(new MainVerticle(true), testContext.succeeding(id -> testContext.completeNow()));
      }, e -> testContext.failNow(e));
    }, e -> testContext.failNow(e));
  }

  @Test
  void testGraphqlSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    JsonObject requestBody = new JsonObject()//
        .put("query", "query { getUserList { id } }")//
        .put("variables", new JsonObject());

    Thread.sleep(waitMillis);
    webClient.post(port, host, requestURI).putHeader("Authorization", "Bearer " + accessToken)
        .rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));

            JsonObject responseBody = response.bodyAsJsonObject();
            Assertions.assertNotNull(responseBody);

            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
  }
}
