package com.anasdidi.security.common;

import org.junit.jupiter.api.Assertions;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpResponse;

public class TestUtils {

  public static void testResponseHeader(HttpResponse<Buffer> response, int statusCode) {
    Assertions.assertEquals(statusCode, response.statusCode());
    Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
  }
}
