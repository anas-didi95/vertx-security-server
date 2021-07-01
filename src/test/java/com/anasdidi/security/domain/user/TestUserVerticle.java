package com.anasdidi.security.domain.user;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.TestUtils;
import org.junit.jupiter.api.AfterAll;
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

  @AfterAll
  static void postTesting(Vertx vertx, VertxTestContext testContext) throws Exception {
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);

    mongoClient.rxRemoveDocuments(CollectionRecord.USER.name, new JsonObject())
        .subscribe(result -> {
          testContext.verify(() -> {
            testContext.completeNow();
          });
        }, e -> testContext.failNow(e));
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
                  Assertions.assertEquals(0, result.getLong("version"));
                  checkpoint.flag();
                }, error -> testContext.failNow(error));
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testUserCreateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    TestUtils.doPostRequest(vertx, UserConstants.CONTEXT_PATH).rxSend().subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 400);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E001", "Request body is empty!");
        checkpoint.flag();
      });

      testContext.verify(() -> {
        String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
        Assertions.assertEquals("Required keys: username,password,fullName,email,telegramId",
            error);
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
              TestUtils.testResponseBodyError(response, "E101", "Create user failed!");
              checkpoint.flag();
            });
          });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).subscribe(id -> {
      requestBody.put("fullName", "testUserUpdateSuccess1");
      requestBody.put("email", "testUserUpdateSuccess2");
      requestBody.put("telegramId", "testUserUpdateSuccess3");

      TestUtils.doPutRequest(vertx, TestUtils.getRequestURI(UserConstants.CONTEXT_PATH, id))
          .rxSendJsonObject(requestBody).subscribe(response -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response, 200);
              checkpoint.flag();
            });

            testContext.verify(() -> {
              JsonObject responseBody = response.bodyAsJsonObject();
              Assertions.assertNotNull(responseBody);
              Assertions.assertEquals(id, responseBody.getString("id"));
              checkpoint.flag();
            });

            JsonObject query = new JsonObject().put("_id", id);
            JsonObject fields = new JsonObject();
            mongoClient.rxFindOne(CollectionRecord.USER.name, query, fields).toSingle()
                .subscribe(result -> {
                  testContext.verify(() -> {
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
                    Assertions.assertEquals(requestBody.getLong("version") + 1,
                        result.getLong("version"));
                    checkpoint.flag();
                  });
                }, error -> testContext.failNow(error));
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).subscribe(id -> {
      TestUtils.doPutRequest(vertx, TestUtils.getRequestURI(UserConstants.CONTEXT_PATH, id))
          .rxSend().subscribe(response -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response, 400);
              checkpoint.flag();
            });

            testContext.verify(() -> {
              TestUtils.testResponseBodyError(response, "E001", "Request body is empty!");
              checkpoint.flag();
            });

            testContext.verify(() -> {
              String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
              Assertions.assertEquals("Required keys: fullName,email,telegramId,version", error);
              checkpoint.flag();
            });
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateValidationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(1);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).subscribe(id -> {
      requestBody.clear().put("a", "a");

      TestUtils.doPutRequest(vertx, TestUtils.getRequestURI(UserConstants.CONTEXT_PATH, id))
          .rxSendJsonObject(requestBody).subscribe(response -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response, 400);
              checkpoint.flag();
            });

            testContext.verify(() -> {
              TestUtils.testResponseBodyError(response, "E002", "Validation error!");
              checkpoint.flag();
            });
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateRecordNotFoundError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    JsonObject requestBody = TestUtils.generateUserJson().put("version", 0);
    String userId = "" + System.currentTimeMillis();

    TestUtils.doPutRequest(vertx, TestUtils.getRequestURI(UserConstants.CONTEXT_PATH, userId))
        .rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 400);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            TestUtils.testResponseBodyError(response, "E102", "Update user failed!");
            checkpoint.flag();
          });

          testContext.verify(() -> {
            String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
            Assertions.assertEquals("Record not found with id: " + userId, error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateVersionMismatch(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).subscribe(id -> {
      long version = -1;
      requestBody.put("fullName", "testUserUpdateVersionMismatch1");
      requestBody.put("email", "testUserUpdateVersionMismatch2");
      requestBody.put("telegramId", "testUserUpdateVersionMismatch3");
      requestBody.put("version", version);

      TestUtils.doPutRequest(vertx, TestUtils.getRequestURI(UserConstants.CONTEXT_PATH, id))
          .rxSendJsonObject(requestBody).subscribe(response -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response, 400);
              checkpoint.flag();
            });

            testContext.verify(() -> {
              TestUtils.testResponseBodyError(response, "E102", "Update user failed!");
              checkpoint.flag();
            });

            testContext.verify(() -> {
              String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
              Assertions.assertEquals(
                  "Current record has version mismatch with requested value: " + version, error);
              checkpoint.flag();
            });
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserDeleteSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject userJson = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, userJson).subscribe(id -> {
      JsonObject requestBody = new JsonObject().put("version", userJson.getLong("version"));

      TestUtils.doDeleteRequest(vertx, TestUtils.getRequestURI(UserConstants.CONTEXT_PATH, id))
          .rxSendJsonObject(requestBody).subscribe(response -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response, 200);
              checkpoint.flag();
            });

            testContext.verify(() -> {
              JsonObject responseBody = response.bodyAsJsonObject();
              Assertions.assertNotNull(responseBody);
              Assertions.assertEquals(id, responseBody.getString("id"));
              checkpoint.flag();
            });

            JsonObject query = new JsonObject().put("_id", id);
            JsonObject fields = new JsonObject();
            mongoClient.rxFindOne(CollectionRecord.USER.name, query, fields).subscribe(
                result -> testContext.failNow("Record not deleted!"),
                error -> testContext.failNow(error), () -> checkpoint.flag());
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }


  @Test
  void testUserDeleteRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject userJson = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, userJson).subscribe(id -> {
      TestUtils.doDeleteRequest(vertx, TestUtils.getRequestURI(UserConstants.CONTEXT_PATH, id))
          .rxSend().subscribe(response -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response, 400);
              checkpoint.flag();
            });

            testContext.verify(() -> {
              TestUtils.testResponseBodyError(response, "E001", "Request body is empty!");
              checkpoint.flag();
            });

            testContext.verify(() -> {
              String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
              Assertions.assertEquals("Required keys: version", error);
              checkpoint.flag();
            });
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }
}
