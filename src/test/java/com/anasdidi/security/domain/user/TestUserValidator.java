package com.anasdidi.security.domain.user;

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
public class TestUserValidator {

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
  void testValidateCreate(Vertx vertx, VertxTestContext testContext) {
    List<String> actualList = new UserValidator().validateCreate(UserVO.fromJson(new JsonObject()));
    List<String> expectedList =
        Arrays.asList("Username is mandatory field!", "Password is mandatory field!",
            "Full Name is mandatory field!", "Email is mandatory field!");

    testContext.verify(() -> {
      Assertions.assertEquals(expectedList.size(), actualList.size());
      expectedList.stream().forEach(error -> {
        Assertions.assertTrue(actualList.contains(error), "Expected error not found! " + error);
      });
      testContext.completeNow();
    });
  }

  @Test
  void testValidateUpdate(Vertx vertx, VertxTestContext testContext) {
    List<String> actualList = new UserValidator().validateUpdate(UserVO.fromJson(new JsonObject()));
    List<String> expectedList = Arrays.asList("Full Name is mandatory field!",
        "Email is mandatory field!", "Version is mandatory field!");

    testContext.verify(() -> {
      Assertions.assertEquals(expectedList.size(), actualList.size());
      expectedList.stream().forEach(error -> {
        Assertions.assertTrue(actualList.contains(error), "Expected error not found! " + error);
      });
      testContext.completeNow();
    });
  }

  @Test
  void testValidateDelete(Vertx vertx, VertxTestContext testContext) {
    List<String> actualList = new UserValidator().validateDelete(UserVO.fromJson(new JsonObject()));
    List<String> expectedList = Arrays.asList("Version is mandatory field!");

    testContext.verify(() -> {
      Assertions.assertEquals(expectedList.size(), actualList.size());
      expectedList.stream().forEach(error -> {
        Assertions.assertTrue(actualList.contains(error), "Expected error not found! " + error);
      });
      testContext.completeNow();
    });
  }

  @Test
  void testValidateChangePassword(Vertx vertx, VertxTestContext testContext) {
    List<String> actualList =
        new UserValidator().validateChangePassword(UserVO.fromJson(new JsonObject()));
    List<String> expectedList = Arrays.asList("Version is mandatory field!",
        "Old Password is mandatory field!", "New Password is mandatory field!");

    testContext.verify(() -> {
      Assertions.assertEquals(expectedList.size(), actualList.size());
      expectedList.stream().forEach(error -> {
        Assertions.assertTrue(actualList.contains(error), "Expected error not found! " + error);
      });
      testContext.completeNow();
    });
  }
}
