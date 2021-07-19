package com.anasdidi.security.domain.auth;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
import com.anasdidi.security.common.ApplicationUtils;
import com.anasdidi.security.common.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.mongo.MongoClient;

@ExtendWith(VertxExtension.class)
public class TestAuthHandler {

  private final String baseURI = ApplicationConstants.CONTEXT_PATH + AuthConstants.CONTEXT_PATH;
  // { "sub": "SYSTEM", "iss": "anasdidi.dev", "pms": ["user:write"] } = secret
  private final String accessToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJTWVNURU0iLCJpc3MiOiJhbmFzZGlkaS5kZXYiLCJwbXMiOlsidXNlcjp3cml0ZSJdfQ.GxIlBwCt3dRWrNWg3xhLSmqHJtcVEHHTKu2A9D9_wug";
  private final String accessTokenNoClaims =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhbmFzZGlkaS5kZXYifQ.F5jwo_F1RkC5cSLKyKFTX2taKqRpCasfSQDMf13o5PA";
  private final String invalidAccessToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJTWVNURU0iLCJpc3MiOiJhbmFzZGlkaS5kZXYifQ.hxbVCLgVWkOTtGMj1OnfzGcDA_6pvaPczBQFebn2PPI";

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
    var removeUsers = mongoClient.rxRemoveDocuments(CollectionRecord.USER.name, new JsonObject());
    var removeTokens = mongoClient.rxRemoveDocuments(CollectionRecord.TOKEN.name, new JsonObject());

    Maybe.merge(removeUsers, removeTokens).subscribe((result) -> {
      testContext.verify(() -> {
        testContext.completeNow();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthLoginSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    String testPassword = "testAuthLoginSuccess:" + System.currentTimeMillis();
    JsonObject document = TestUtils.generateUserJson(testPassword);

    mongoClient.rxSave(CollectionRecord.USER.name, document).subscribe(id -> {
      JsonObject requestBody = new JsonObject().put("username", document.getString("username"))
          .put("password", testPassword);

      TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login"))
          .rxSendJsonObject(requestBody).subscribe(response -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response, 200);
              checkpoint.flag();
            });

            testContext.verify(() -> {
              JsonObject responseBody = response.bodyAsJsonObject();
              Assertions.assertNotNull(responseBody);
              Assertions.assertNotNull(responseBody.getString("accessToken"));
              Assertions.assertNotNull(responseBody.getString("refreshToken"));
              checkpoint.flag();
            });

            String accessToken = response.bodyAsJsonObject().getString("accessToken");
            TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "check"), accessToken)
                .rxSend().subscribe(response1 -> {
                  testContext.verify(() -> {
                    TestUtils.testResponseHeader(response1, 200);
                    checkpoint.flag();
                  });
                }, error -> testContext.failNow(error));
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthLoginRequestBodyEmptyError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login")).rxSend()
        .subscribe(response -> {
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
            Assertions.assertEquals("Required keys: username,password", error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthLoginValidationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);
    JsonObject requestBody = new JsonObject().put("key", "value");

    TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login"))
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
  void testAuthLoginRecordNotFoundError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    String testValue = "testAuthLoginRecordNotFoundError:" + System.currentTimeMillis();
    JsonObject requestBody = new JsonObject().put("username", testValue).put("password", testValue);

    TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login"))
        .rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 400);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            TestUtils.testResponseBodyError(response, "E201", "Invalid credentials!");
            checkpoint.flag();
          });

          testContext.verify(() -> {
            String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
            Assertions.assertEquals("Record not found with username: " + testValue, error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthLoginWrongPasswordError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    JsonObject user = TestUtils.generateUserJson("test");

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      JsonObject requestBody = new JsonObject().put("username", user.getString("username"))
          .put("password", "testAuthLoginWrongPasswordError:" + System.currentTimeMillis());

      return TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login"))
          .rxSendJsonObject(requestBody);
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 400);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        TestUtils.testResponseBodyError(response, "E201", "Invalid credentials!");
        checkpoint.flag();
      });

      testContext.verify(() -> {
        String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
        Assertions.assertEquals("Wrong password for username: " + user.getString("username"),
            error);
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthCheckSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    String password = "testAuthCheckSuccess";
    JsonObject user = TestUtils.generateUserJson(password);

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      user.put("id", id);
      JsonObject requestBody =
          new JsonObject().put("username", user.getString("username")).put("password", password);
      return TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login"))
          .rxSendJsonObject(requestBody);
    }).flatMapSingle(response -> {
      String accessToken = response.bodyAsJsonObject().getString("accessToken");
      return TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "check"), accessToken)
          .rxSend();
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 200);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(user.getString("id"), responseBody.getString("userId"));
        Assertions.assertEquals(user.getString("username"), responseBody.getString("username"));
        Assertions.assertEquals(user.getString("fullName"), responseBody.getString("fullName"));
        Assertions.assertEquals(user.getJsonArray("permissions").encode(),
            responseBody.getJsonArray("permissions").encode());
        checkpoint.flag();
      });
    }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthCheckAuthenticationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "check"), invalidAccessToken)
        .rxSend().subscribe(response -> {
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
  void testAuthCheckValidationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "check"), accessTokenNoClaims)
        .rxSend().subscribe(response -> {
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
  void testAuthCheckRecordNotFoundError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "check"), accessToken).rxSend()
        .subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 400);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            TestUtils.testResponseBodyError(response, "E202", "Incorrect credentials data!");
            checkpoint.flag();
          });

          testContext.verify(() -> {
            String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
            Assertions.assertEquals("Record not found with id: SYSTEM", error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthRefreshSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(4);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    String password = "testAuthRefreshSuccess:password";
    JsonObject user = TestUtils.generateUserJson(password);

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      user.put("id", id);
      JsonObject requestBody =
          new JsonObject().put("username", user.getString("username")).put("password", password);
      return TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login"))
          .rxSendJsonObject(requestBody);
    }).flatMapSingle(response -> {
      String refreshToken = response.bodyAsJsonObject().getString("refreshToken");
      return TestUtils
          .doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "refresh"), refreshToken).rxSend();
    }).subscribe(response -> {
      mongoClient
          .rxFindOne(CollectionRecord.TOKEN.name,
              new JsonObject().put("userId", user.getString("id")), new JsonObject())
          .subscribe(result -> {
            testContext.verify(() -> {
              Assertions.assertNotNull(result.getString("userId"));
              Assertions.assertNotNull(ApplicationUtils.getRecordDate(result, "issuedDate"));
              checkpoint.flag();
            });
          });

      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 200);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);
        Assertions.assertNotNull(responseBody.getString("accessToken"));
        Assertions.assertNotNull(responseBody.getString("refreshToken"));
        checkpoint.flag();
      });

      String accessToken = response.bodyAsJsonObject().getString("accessToken");
      TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "check"), accessToken).rxSend()
          .subscribe(response1 -> {
            testContext.verify(() -> {
              TestUtils.testResponseHeader(response1, 200);
              checkpoint.flag();
            });
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthRefreshAuthenticationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "refresh"), invalidAccessToken)
        .rxSend().subscribe(response -> {
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
  void testAuthRefreshValidationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "refresh"), accessTokenNoClaims)
        .rxSend().subscribe(response -> {
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
  void testAuthRefreshRecordNotFoundError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);

    TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "refresh"), accessToken).rxSend()
        .subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 400);
            checkpoint.flag();
          });

          testContext.verify(() -> {
            TestUtils.testResponseBodyError(response, "E203", "Refresh token failed!");
            checkpoint.flag();
          });

          testContext.verify(() -> {
            String error = response.bodyAsJsonObject().getJsonArray("errors").getString(0);
            Assertions.assertEquals("Record not found with id: SYSTEM", error);
            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testAuthLogoutSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    MongoClient mongoClient = TestUtils.getMongoClient(vertx);
    String password = "testAuthLogoutSuccess:password";
    JsonObject user = TestUtils.generateUserJson(password);

    mongoClient.rxSave(CollectionRecord.USER.name, user).flatMapSingle(id -> {
      user.put("id", id);
      JsonObject requestBody =
          new JsonObject().put("username", user.getString("username")).put("password", password);
      return TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login"))
          .rxSendJsonObject(requestBody);
    }).flatMapSingle(response -> {
      String accessToken = response.bodyAsJsonObject().getString("accessToken");
      return TestUtils.doGetRequest(vertx, TestUtils.getRequestURI(baseURI, "logout"), accessToken)
          .rxSend();
    }).subscribe(response -> {
      testContext.verify(() -> {
        TestUtils.testResponseHeader(response, 200);
        checkpoint.flag();
      });

      testContext.verify(() -> {
        JsonObject responseBody = response.bodyAsJsonObject();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(user.getString("id"), responseBody.getString("userId"));
        checkpoint.flag();
      });

      String userId = response.bodyAsJsonObject().getString("userId");
      mongoClient.rxCount(CollectionRecord.TOKEN.name, new JsonObject().put("userId", userId))
          .subscribe(count -> {
            testContext.verify(() -> {
              Assertions.assertEquals(0, count);
              checkpoint.flag();
            });
          }, error -> testContext.failNow(error));
    }, error -> testContext.failNow(error));
  }
}
