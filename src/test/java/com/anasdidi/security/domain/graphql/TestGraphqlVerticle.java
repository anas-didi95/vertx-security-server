package com.anasdidi.security.domain.graphql;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.TestConstants;
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

@ExtendWith(VertxExtension.class)
public class TestGraphqlVerticle {

  private String requestURI = ApplicationConstants.CONTEXT_PATH + GraphqlConstants.CONTEXT_PATH;

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
  void testGraphqlSuccess(Vertx vertx, VertxTestContext testContext) throws Exception {
    Checkpoint checkpoint = testContext.checkpoint(2);
    String testValue = "" + System.currentTimeMillis();
    JsonObject requestBody = new JsonObject()//
        .put("query", "query($value: String!) { ping(value: $value) { isSuccess testValue } }")//
        .put("variables", new JsonObject()//
            .put("value", testValue));

    TestUtils.doPostRequest(vertx, requestURI, TestConstants.ACCESS_TOKEN)
        .rxSendJsonObject(requestBody).subscribe(response -> {
          testContext.verify(() -> {
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
            checkpoint.flag();
          });

          testContext.verify(() -> {
            JsonObject responseBody = response.bodyAsJsonObject();
            Assertions.assertNotNull(responseBody);

            JsonObject data = responseBody.getJsonObject("data");
            Assertions.assertNotNull(data);

            JsonObject ping = data.getJsonObject("ping");
            Assertions.assertNotNull(ping);
            Assertions.assertEquals(true, ping.getBoolean("isSuccess"));
            Assertions.assertEquals(testValue, ping.getString("testValue"));

            checkpoint.flag();
          });
        }, error -> testContext.failNow(error));
  }

  @Test
  void testGraphqlAuthenticationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    String testValue = "" + System.currentTimeMillis();
    JsonObject requestBody = new JsonObject()//
        .put("query", "query($value: String!) { ping(value: $value) { isSuccess testValue } }")//
        .put("variables", new JsonObject()//
            .put("value", testValue));

    TestUtils.doPostRequest(vertx, requestURI, TestConstants.REFRESH_TOKEN)
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
  void testGraphqlAuthorizationError(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(3);
    String testValue = "" + System.currentTimeMillis();
    JsonObject requestBody = new JsonObject()//
        .put("query", "query($value: String!) { ping(value: $value) { isSuccess testValue } }")//
        .put("variables", new JsonObject()//
            .put("value", testValue));

    TestUtils.doPostRequest(vertx, requestURI, TestConstants.ACCESS_TOKEN_NO_PERMISSION)
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
}
