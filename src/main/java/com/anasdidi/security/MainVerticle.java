package com.anasdidi.security;

import com.anasdidi.security.api.user.UserVerticle;
import com.anasdidi.security.common.CommonUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

  private final boolean isTest;
  private Logger logger = LogManager.getLogger(MainVerticle.class);

  public MainVerticle(boolean isTest) {
    System.setProperty("vertx.logger-delegate-factory-class-name", Log4j2LogDelegateFactory.class.getName());
    this.isTest = isTest;
  }

  public MainVerticle() {
    System.setProperty("vertx.logger-delegate-factory-class-name", Log4j2LogDelegateFactory.class.getName());
    this.isTest = false;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

    configRetriever.rxGetConfig().subscribe(cfg -> {
      MongoClient mongoClient = MongoClient.createShared(vertx, new JsonObject()//
          .put("host", isTest ? cfg.getString("TEST_MONGO_HOST") : cfg.getString("MONGO_HOST"))//
          .put("port", isTest ? cfg.getInteger("TEST_MONGO_PORT") : cfg.getInteger("MONGO_PORT"))//
          .put("username", isTest ? cfg.getString("TEST_MONGO_USERNAME") : cfg.getString("MONGO_USERNAME"))//
          .put("password", isTest ? cfg.getString("TEST_MONGO_PASSWORD") : cfg.getString("MONGO_PASSWORD"))//
          .put("authSource", isTest ? cfg.getString("TEST_MONGO_AUTH_SOURCE") : cfg.getString("MONGO_AUTH_SOURCE"))//
          .put("db_name", "security"));

      Router router = Router.router(vertx);
      router.route().handler(BodyHandler.create());
      router.route().handler(this::generateRequestId);

      vertx.deployVerticle(new UserVerticle(router, mongoClient));

      vertx.createHttpServer().requestHandler(router).listen(5000, "localhost", http -> {
        if (http.succeeded()) {
          logger.info("HTTP server started on port 5000");
          startPromise.complete();
        } else {
          startPromise.fail(http.cause());
        }
      });
    }, e -> startPromise.fail(e));
  }

  void generateRequestId(RoutingContext routingContext) {
    routingContext.put("requestId", CommonUtils.generateId());
    routingContext.put("startTime", System.currentTimeMillis());
    routingContext.next();
  }
}
