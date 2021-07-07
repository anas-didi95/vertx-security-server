package com.anasdidi.security.common;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;

public abstract class BaseVerticle extends AbstractVerticle {

  public abstract String getContextPath();

  public abstract Router getRouter();

  protected abstract void setHandler(Router router, EventBus eventBus,
      JWTAuthHandler jwtAuthHandler);

  protected final MongoClient getMongoClient() {
    ApplicationConfig config = ApplicationConfig.instance();
    return MongoClient.create(vertx,
        new JsonObject().put("connection_string", config.getMongoConnectionString()));
  }

  protected final JWTAuth getAuthProvider() {
    return JWTAuth.create(vertx, new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("secret")));
  }

  protected final JWTAuthHandler getAuthHandler() {
    return JWTAuthHandler.create(getAuthProvider());
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    setHandler(getRouter(), vertx.eventBus(), getAuthHandler());
  }
}
