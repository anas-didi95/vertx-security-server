package com.anasdidi.security.api.user;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;

public class UserVerticle extends AbstractVerticle {

  private final Router mainRouter;

  public UserVerticle(Router mainRouter) {
    this.mainRouter = mainRouter;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);
    router.get("/").handler(routingContext -> {
      routingContext.response()//
          .putHeader("Accept", "application/json")//
          .putHeader("Content-Type", "application/json")//
          .end(new JsonObject()//
              .put("data", "Hello world").encode());
    });

    mainRouter.mountSubRouter("/api/users", router);

    System.out.println("[UserVerticle:start] Deployed success");
    startPromise.complete();
  }
}
