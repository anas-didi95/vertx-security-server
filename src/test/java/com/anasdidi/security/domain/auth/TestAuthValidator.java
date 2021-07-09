package com.anasdidi.security.domain.auth;

import java.util.Arrays;
import java.util.List;
import com.anasdidi.security.MainVerticle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;

@ExtendWith(VertxExtension.class)
public class TestAuthValidator {

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
  void testValidateLogin(Vertx vertx, VertxTestContext testContext) {
    List<String> actualList = new AuthValidator().validateLogin(AuthVO.fromJson(new JsonObject()));
    List<String> expectedList =
        Arrays.asList("Username is mandatory field!", "Password is mandatory field!");

    testContext.verify(() -> {
      Assertions.assertEquals(expectedList.size(), actualList.size());
      expectedList.stream().forEach(error -> {
        Assertions.assertTrue(actualList.contains(error), "Expected error not found! " + error);
      });
      testContext.completeNow();
    });
  }
}
