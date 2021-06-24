package com.anasdidi.security.domain.user;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
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
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    TestUtils.doPostRequest(vertx, UserConstants.CONTEXT_PATH).rxSendJsonObject(requestBody)
        .subscribe(response -> {
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

          testContext.verify(() -> {
            String id = response.bodyAsJsonObject().getString("id");
            JsonObject query = new JsonObject().put("_id", id);
            JsonObject fields = new JsonObject();
            mongoClient.findOne(CollectionRecord.USER.name, query, fields).toSingle()
                .subscribe(result -> {
                  Assertions.assertEquals(requestBody.getString("username"),
                      result.getString("username"));
                  Assertions.assertEquals(requestBody.getString("password"),
                      result.getString("password"));
                  Assertions.assertEquals(requestBody.getString("fullName"),
                      result.getString("fullName"));
                  Assertions.assertEquals(requestBody.getString("email"),
                      result.getString("email"));
                  Assertions.assertEquals(requestBody.getString("telegramId"),
                      result.getString("telegramId"));
                  checkpoint.flag();
                }, error -> testContext.failNow(error));
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testUserCreateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    TestUtils.doPostRequest(vertx, UserConstants.CONTEXT_PATH).rxSend().subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 400);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E001", "Request body is empty!");
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserCreateValidationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);
    JsonObject requestBody = new JsonObject().put("a", "a");

    TestUtils.doPostRequest(vertx, UserConstants.CONTEXT_PATH).rxSendJsonObject(requestBody)
        .subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 400);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            TestUtils.testResponseBodyError(response, "E002", "Validation error!");
            checkpoint.flag();
          });
        });
  }

  @Test
  void testUserCreateUserServiceError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).subscribe(id -> {
      TestUtils.doPostRequest(vertx, UserConstants.CONTEXT_PATH).rxSendJsonObject(requestBody)
          .subscribe(response -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response, 400);
              checkpoint.flag();
            });

            testContext.verify(() -> {
              TestUtils.testResponseBodyError(response, "E100", "Create user failed!");
              checkpoint.flag();
            });
          });
    }, error -> testContext.failNow(error));
  }
}
