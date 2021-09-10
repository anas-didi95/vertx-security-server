package com.anasdidi.security.domain.user;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.ApplicationUtils;
import com.anasdidi.security.common.TestConstants;
import com.anasdidi.security.common.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.mongo.MongoClient;

@ExtendWith(VertxExtension.class)
public class TestUserVerticle {

  private final String baseURI = ApplicationConstants.CONTEXT_PATH + UserConstants.CONTEXT_PATH;

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

    TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI), TestConstants.ACCESS_TOKEN)
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
          mongoClient.findOne(CollectionRecord.USER.name, query, fields).toSingle()
              .subscribe(result -> {
                testContext.verify(() -> {
                  Assertions.assertEquals(requestBody.getString("username"),
                      result.getString("username"));
                  Assertions.assertTrue(BCrypt.checkpw(requestBody.getString("password"),
                      result.getString("password")));
                  Assertions.assertEquals(requestBody.getString("fullName"),
                      result.getString("fullName"));
                  Assertions.assertEquals(requestBody.getString("email"),
                      result.getString("email"));
                  Assertions.assertEquals(requestBody.getString("telegramId"),
                      result.getString("telegramId"));
                  Assertions.assertTrue(requestBody.getJsonArray("permissions").encode()
                      .equals(result.getJsonArray("permissions").encode()));
                  Assertions.assertEquals(0, result.getLong("version"));
                  Assertions
                      .assertNotNull(ApplicationUtils.getRecordDate(result, "lastModifiedDate"));
                  Assertions.assertEquals("SYSTEM", result.getString("lastModifiedBy"));
                  checkpoint.flag();
                });
              }, error -> testContext.failNow(error));
        }, error -> testContext.failNow(error));
  }

  @Test
  void testUserCreateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI), TestConstants.ACCESS_TOKEN)
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

    TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI), TestConstants.ACCESS_TOKEN)
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
  }

  @Test
  void testUserCreateUserServiceError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).flatMapSingle(id -> {
      return TestUtils
          .doPostRequest(vertx, TestUtils.getRequestURI(baseURI), TestConstants.ACCESS_TOKEN)
          .rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 400);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E101", "Create user failed!");
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserCreateAuthenticationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    JsonObject requestBody = TestUtils.generateUserJson();

    TestUtils
        .doPostRequest(vertx, TestUtils.getRequestURI(baseURI),
            TestConstants.ACCESS_TOKEN_INVALID_SIGNATURE)
        .rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 401);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            TestUtils.testResponseBodyError(response, "E003", "Unauthorized!");
            checkpoint.flag();
          });

          testContext.verify(() -> {
            String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
            Assertions.assertEquals("Lacks valid authentication credentials for resource", error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testUserCreateAuthorizationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    JsonObject requestBody = TestUtils.generateUserJson();

    TestUtils
        .doPostRequest(vertx, TestUtils.getRequestURI(baseURI),
            TestConstants.ACCESS_TOKEN_NO_PERMISSION)
        .rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 403);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            TestUtils.testResponseBodyError(response, "E004", "Forbidden!");
            checkpoint.flag();
          });

          testContext.verify(() -> {
            String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
            Assertions.assertEquals("Insufficient permissions for resource", error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).flatMapSingle(id -> {
      requestBody.put("fullName", "testUserUpdateSuccess1");
      requestBody.put("email", "testUserUpdateSuccess2");
      requestBody.put("telegramId", "testUserUpdateSuccess3");
      requestBody.put("permissions",
          new JsonArray().add("updatePermission1").add("updatePermission2"));

      return TestUtils
          .doPutRequest(vertx, TestUtils.getRequestURI(baseURI, id), TestConstants.ACCESS_TOKEN)
          .rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 200);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);
        Assertions.assertNotNull(responseBody.getString("id"));
        checkpoint.flag();
      });

      String userId = response.bodyAsJsonObject().getString("id");
      JsonObject query = new JsonObject().put("_id", userId);
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
              Assertions.assertEquals(requestBody.getString("email"), result.getString("email"));
              Assertions.assertEquals(requestBody.getString("telegramId"),
                  result.getString("telegramId"));
              Assertions.assertTrue(requestBody.getJsonArray("permissions").encode()
                  .equals(result.getJsonArray("permissions").encode()));
              Assertions.assertEquals(requestBody.getLong("version") + 1,
                  result.getLong("version"));
              Assertions.assertNotNull(ApplicationUtils.getRecordDate(result, "lastModifiedDate"));
              Assertions.assertEquals("SYSTEM", result.getString("lastModifiedBy"));
              checkpoint.flag();
            });
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).flatMapSingle(id -> {
      return TestUtils
          .doPutRequest(vertx, TestUtils.getRequestURI(baseURI, id), TestConstants.ACCESS_TOKEN)
          .rxSend();
    }).subscribe(response -> {
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
  }

  @Test
  void testUserUpdateValidationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(1);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).flatMapSingle(id -> {
      requestBody.clear().put("a", "a");

      return TestUtils
          .doPutRequest(vertx, TestUtils.getRequestURI(baseURI, id), TestConstants.ACCESS_TOKEN)
          .rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 400);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E002", "Validation error!");
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateRecordNotFoundError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    JsonObject requestBody = TestUtils.generateUserJson().put("version", 0);
    String userId = "" + System.currentTimeMillis();

    TestUtils
        .doPutRequest(vertx, TestUtils.getRequestURI(baseURI, userId), TestConstants.ACCESS_TOKEN)
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
            JsonObject query = new JsonObject().put("_id", userId);
            Assertions.assertEquals("Record not found with query: " + query.encode(), error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateVersionMismatchError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject requestBody = TestUtils.generateUserJson();
    long version = -1;

    mongoClient.rxSave(CollectionRecord.USER.name, requestBody).flatMapSingle(id -> {
      requestBody.put("fullName", "testUserUpdateVersionMismatch1");
      requestBody.put("email", "testUserUpdateVersionMismatch2");
      requestBody.put("telegramId", "testUserUpdateVersionMismatch3");
      requestBody.put("version", version);

      return TestUtils
          .doPutRequest(vertx, TestUtils.getRequestURI(baseURI, id), TestConstants.ACCESS_TOKEN)
          .rxSendJsonObject(requestBody);
    }).subscribe(response -> {
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
  }

  @Test
  void testUserUpdateAuthenticationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint();
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject user = TestUtils.generateUserJson("password");

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      return TestUtils.doPutRequest(vertx, TestUtils.getRequestURI(baseURI, id),
          TestConstants.ACCESS_TOKEN_INVALID_SIGNATURE).rxSendJsonObject(user);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 401);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E003", "Unauthorized!");
        checkpoint.flag();
      });

      testContext.verify(() -> {
        String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
        Assertions.assertEquals("Lacks valid authentication credentials for resource", error);
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserUpdateAuthorzationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint();
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject user = TestUtils.generateUserJson("password");

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      return TestUtils.doPutRequest(vertx, TestUtils.getRequestURI(baseURI, id),
          TestConstants.ACCESS_TOKEN_NO_PERMISSION).rxSendJsonObject(user);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 403);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E004", "Forbidden!");
        checkpoint.flag();
      });

      testContext.verify(() -> {
        String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
        Assertions.assertEquals("Insufficient permissions for resource", error);
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserDeleteSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject userJson = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, userJson).flatMapSingle(id -> {
      JsonObject requestBody = new JsonObject().put("version", userJson.getLong("version"));

      return TestUtils
          .doDeleteRequest(vertx, TestUtils.getRequestURI(baseURI, id), TestConstants.ACCESS_TOKEN)
          .rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 200);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);
        Assertions.assertNotNull(responseBody.getString("id"));
        checkpoint.flag();
      });

      String userId = response.bodyAsJsonObject().getString("id");
      JsonObject query = new JsonObject().put("_id", userId);
      JsonObject fields = new JsonObject();
      mongoClient.rxFindOne(CollectionRecord.USER.name, query, fields).subscribe(
          result -> testContext.failNow("Record not deleted!"), error -> testContext.failNow(error),
          () -> checkpoint.flag());
    }, error -> testContext.failNow(error));
  }


  @Test
  void testUserDeleteRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject userJson = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, userJson).flatMapSingle(id -> {
      return TestUtils
          .doDeleteRequest(vertx, TestUtils.getRequestURI(baseURI, id), TestConstants.ACCESS_TOKEN)
          .rxSend();
    }).subscribe(response -> {
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
  }

  @Test
  void testUserDeleteValidationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject userJson = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, userJson).flatMapSingle(id -> {
      JsonObject requestBody = new JsonObject().put("key", "value");

      return TestUtils
          .doDeleteRequest(vertx, TestUtils.getRequestURI(baseURI, id), TestConstants.ACCESS_TOKEN)
          .rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 400);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E002", "Validation error!");
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserDeleteRecordNotFoundError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    String userId = "" + System.currentTimeMillis();
    JsonObject requestBody = new JsonObject().put("version", 0);

    TestUtils.doDeleteRequest(vertx, TestUtils.getRequestURI(baseURI, userId),
        TestConstants.ACCESS_TOKEN).rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 400);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            TestUtils.testResponseBodyError(response, "E103", "Delete user failed!");
            checkpoint.flag();
          });

          testContext.verify(() -> {
            String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
            JsonObject query = new JsonObject().put("_id", userId);
            Assertions.assertEquals("Record not found with query: " + query.encode(), error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testUserDeleteVersionMismatchError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject userJson = TestUtils.generateUserJson();
    long version = -1;

    mongoClient.rxSave(CollectionRecord.USER.name, userJson).flatMapSingle(id -> {
      JsonObject requestBody = new JsonObject().put("version", version);

      return TestUtils
          .doDeleteRequest(vertx, TestUtils.getRequestURI(baseURI, id), TestConstants.ACCESS_TOKEN)
          .rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 400);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E103", "Delete user failed!");
        checkpoint.flag();
      });

      testContext.verify(() -> {
        String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
        Assertions.assertEquals(
            "Current record has version mismatch with requested value: " + version, error);
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserDeleteAuthenticationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject user = TestUtils.generateUserJson("password");

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      return TestUtils.doDeleteRequest(vertx, TestUtils.getRequestURI(baseURI, id),
          TestConstants.ACCESS_TOKEN_INVALID_SIGNATURE).rxSendJsonObject(user);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 401);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E003", "Unauthorized!");
        checkpoint.flag();
      });

      testContext.verify(() -> {
        String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
        Assertions.assertEquals("Lacks valid authentication credentials for resource", error);
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserDeleteAuthorizationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject user = TestUtils.generateUserJson("password");

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      return TestUtils.doDeleteRequest(vertx, TestUtils.getRequestURI(baseURI, id),
          TestConstants.ACCESS_TOKEN_NO_PERMISSION).rxSendJsonObject(user);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 403);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E004", "Forbidden!");
        checkpoint.flag();
      });

      testContext.verify(() -> {
        String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
        Assertions.assertEquals("Insufficient permissions for resource", error);
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserChangePasswordSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    String oldPassword = "oldPassword:" + System.currentTimeMillis();
    String newPassword = "newPassword:" + System.currentTimeMillis();
    JsonObject user = TestUtils.generateUserJson(oldPassword);

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      JsonObject requestBody = new JsonObject().put("version", user.getLong("version"))
          .put("oldPassword", oldPassword).put("newPassword", newPassword);
      return TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, id, "change-password"),
          TestConstants.ACCESS_TOKEN).rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 200);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);
        Assertions.assertNotNull(responseBody.getString("id"));
        checkpoint.flag();
      });

      String userId = response.bodyAsJsonObject().getString("id");
      JsonObject query = new JsonObject().put("_id", userId);
      JsonObject fields = new JsonObject();
      mongoClient.rxFindOne(CollectionRecord.USER.name, query, fields).subscribe(result -> {
        testContext.verify(() -> {
          Assertions.assertEquals(result.getString("username"), user.getString("username"));
          Assertions.assertEquals(result.getLong("version"), user.getLong("version") + 1);
          Assertions.assertNotEquals(user.getString("password"), result.getString("password"));
          Assertions.assertTrue(BCrypt.checkpw(newPassword, result.getString("password")));
          checkpoint.flag();
        });
      }, error -> testContext.failNow(error), () -> testContext.failNow("User not found!"));
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserChangePasswordRequestBodyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject user = TestUtils.generateUserJson();

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      JsonObject requestBody = new JsonObject();
      return TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, id, "change-password"),
          TestConstants.ACCESS_TOKEN).rxSendJsonObject(requestBody);
    }).subscribe(response -> {
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
        Assertions.assertEquals("Required keys: version,oldPassword,newPassword", error);
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testUserChangePasswordValidationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    String oldPassword = "oldPassword:" + System.currentTimeMillis();
    JsonObject user = TestUtils.generateUserJson(oldPassword);

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      JsonObject requestBody = new JsonObject().put("key", "value");
      return TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, id, "change-password"),
          TestConstants.ACCESS_TOKEN).rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 400);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E002", "Validation error!");
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }
}
