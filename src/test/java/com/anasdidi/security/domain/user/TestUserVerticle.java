package com.anasdidi.security.domain.user;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConfig;
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
import io.vertx.rxjava3.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
public class TestUserVerticle {

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
  void testUserCreateSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    ApplicationConfig config = ApplicationConfig.instance();
    WebClient webClient = WebClient.create(vertx);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx, config.getMongoConnectionString());

    String suffix = ":" + System.currentTimeMillis();
    JsonObject requestBody = new JsonObject().put("username", "username" + suffix);

    webClient.post(config.getAppPort(), config.getAppHost(), "/user")
        .putHeader("Accept", "application/json").putHeader("Content-Type", "application/json")
        .rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 201);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            JsonObject responseBody = response.bodyAsJsonObject();
            Assertions.assertNotNull(responseBody);
            Assertions.assertNotNull(responseBody.getString("id"));
            checkpoint.flag();
          });

          String id = response.bodyAsJsonObject().getString("id");
          JsonObject query = new JsonObject().put("_id", id);
          JsonObject fields = new JsonObject();
          mongoClient.findOne("users", query, fields).toSingle().subscribe(result -> {
            testContext.verify(() -> {
              Assertions.assertEquals(requestBody.getString("username"),
                  result.getString("username"));
              checkpoint.flag();
            });
          }, error -> testContext.failNow(error));
        }, error -> testContext.failNow(error));
  }
}
