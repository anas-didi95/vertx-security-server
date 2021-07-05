package com.anasdidi.security.domain.auth;

import com.anasdidi.security.MainVerticle;
import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;

@ExtendWith(VertxExtension.class)
public class TestAuthHandler {

  private final String baseURI = ApplicationConstants.CONTEXT_PATH + "/auth";

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
    Checkpoint checkpoint = testContext.checkpoint(1);

    TestUtils.doPostRequest(vertx, TestUtils.getRequestURI(baseURI, "login")).rxSend()
        .subscribe(response -> {
          testContext.verify(() -> {
            TestUtils.testResponseHeader(response, 200);
            checkpoint.flag();
          });

        }, error -> testContext.failNow(error));
  }
}
