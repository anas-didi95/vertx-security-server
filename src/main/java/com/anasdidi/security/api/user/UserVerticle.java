package com.anasdidi.security.api.user;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

public class UserVerticle extends AbstractVerticle {

  private final Router mainRouter;

  public UserVerticle(Router mainRouter) {
    this.mainRouter = mainRouter;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);
    router.post("/").handler(this::create);

    mainRouter.mountSubRouter("/api/users", router);

    System.out.println("[UserVerticle:start] Deployed success");
    startPromise.complete();
  }

  void create(RoutingContext routingContext) {
    routingContext.response()//
        .putHeader("Accept", "application/json")//
        .putHeader("Content-Type", "application/json")//
        .setStatusCode(201)//
        .end(new JsonObject()//
            .put("status", new JsonObject()//
                .put("isSuccess", true)//
                .put("message", "Record successfully created."))//
            .put("data", new JsonObject()//
                .put("id", "id"))//
            .encode());
  }
}
