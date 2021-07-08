package com.anasdidi.security.domain.auth;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.TestUtils;
import com.anasdidi.security.common.ApplicationConstants.CollectionRecord;
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
public class TestAuthHandler {

  private final String baseURI = ApplicationConstants.CONTEXT_PATH + AuthConstants.CONTEXT_PATH;

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
}
