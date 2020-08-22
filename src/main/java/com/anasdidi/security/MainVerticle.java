package com.anasdidi.security;

import com.anasdidi.security.api.user.UserVerticle;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject()//
        .put("host", "mongo")//
        .put("port", 27017)//
        .put("username", "mongo")//
        .put("password", "mongo")//
        .put("authSource", "admin")//
        .put("db_name", "security"));

    Router router = Router.router(vertx);

    vertx.deployVerticle(new UserVerticle(router, mongoClient));

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
