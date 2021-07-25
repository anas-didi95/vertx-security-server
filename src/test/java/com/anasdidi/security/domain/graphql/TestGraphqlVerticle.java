package com.anasdidi.security.domain.graphql;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConstants;
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
  // { "sub": "SYSTEM", "iss": "anasdidi.dev", "pms": ["user:write"], "typ": "TOKEN_ACCESS" } =
  // secret
  private final String accessToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJTWVNURU0iLCJpc3MiOiJhbmFzZGlkaS5kZXYiLCJwbXMiOlsidXNlcjp3cml0ZSJdLCJ0eXAiOiJUT0tFTl9BQ0NFU1MifQ.Vrehyb_erdUw_ziFUE15zg-Aiefp7fmpDWB9n69Ms3k";
  // { "sub": "SYSTEM", "iss": "anasdidi.dev", "typ": "TOKEN_REFRESH" } = secret
  private final String refreshToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJTWVNURU0iLCJpc3MiOiJhbmFzZGlkaS5kZXYiLCJ0eXAiOiJUT0tFTl9SRUZSRVNIIn0.Wie-HReiLjlUdwxIC0di2ACQFVOB_PmjPq52zOStRmY";

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

    TestUtils.doPostRequest(vertx, requestURI, accessToken).rxSendJsonObject(requestBody)
        .subscribe(response -> {
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

    TestUtils.doPostRequest(vertx, requestURI, refreshToken).rxSendJsonObject(requestBody)
        .subscribe(response -> {
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
}
