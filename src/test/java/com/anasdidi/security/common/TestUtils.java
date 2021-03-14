package com.anasdidi.security.common;

import org.junit.jupiter.api.Assertions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;

public final class TestUtils {

  public static void checkCommonHeaders(HttpResponse<Buffer> response, int statusCode) {
    Assertions.assertEquals(statusCode, response.statusCode());
    Assertions.assertEquals("application/json", response.getHeader("Content-Type"));
    Assertions.assertEquals("no-store, no-cache", response.getHeader("Cache-Control"));
    Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    Assertions.assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
    Assertions.assertEquals("deny", response.getHeader("X-Frame-Options"));
  }

  public static JsonObject getResponseBody(HttpResponse<Buffer> response) {
    JsonObject responseBody = response.bodyAsJsonObject();
    Assertions.assertNotNull(responseBody);

    return responseBody;
  }
}
