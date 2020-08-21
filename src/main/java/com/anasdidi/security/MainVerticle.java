package com.anasdidi.security;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);
    router.get("/api/users").handler(routingContext -> {
      routingContext.response()//
          .putHeader("Accept", "application/json")//
          .putHeader("Content-Type", "application/json")//
          .end(new JsonObject()//
              .put("data", "Hello world").encode());
    });

    vertx.createHttpServer().requestHandler(router).listen(5000, "localhost", http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 5000");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
