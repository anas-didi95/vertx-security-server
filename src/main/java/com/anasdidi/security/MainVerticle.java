package com.anasdidi.security;

import io.vertx.core.Promise;
import io.vertx.rxjava3.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.createHttpServer().requestHandler(req -> {
      req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x!");
    }).listen(5000).subscribe(server -> {
      System.out.println("HTTP server started on port 5000");
      startPromise.complete();
    }, error -> {
      startPromise.fail(error);
    });
  }
}
