package com.anasdidi.security;

import com.anasdidi.security.api.jwt.JwtVerticle;
import com.anasdidi.security.api.user.UserVerticle;
import com.anasdidi.security.common.CommonUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.healthchecks.Status;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.healthchecks.HealthCheckHandler;
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
      JsonObject mongoConfig = new JsonObject()
          .put("host", isTest ? cfg.getString("TEST_MONGO_HOST") : cfg.getString("MONGO_HOST"))//
          .put("port", isTest ? cfg.getInteger("TEST_MONGO_PORT") : cfg.getInteger("MONGO_PORT"))//
          .put("username", isTest ? cfg.getString("TEST_MONGO_USERNAME") : cfg.getString("MONGO_USERNAME"))//
          .put("password", isTest ? cfg.getString("TEST_MONGO_PASSWORD") : cfg.getString("MONGO_PASSWORD"))//
          .put("authSource", isTest ? cfg.getString("TEST_MONGO_AUTH_SOURCE") : cfg.getString("MONGO_AUTH_SOURCE"))//
          .put("db_name", "security");
      MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfig);//

      Router router = Router.router(vertx);
      router.route().handler(BodyHandler.create());
      router.route().handler(this::generateRequestId);

      @SuppressWarnings("deprecation")
      JWTAuth jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()//
          .setJWTOptions(new JWTOptions()//
              .setExpiresInMinutes(cfg.getInteger("JWT_EXPIRE_IN_MINUTES"))//
              .setIssuer(cfg.getString("JWT_ISSUER")))//
          .addPubSecKey(new PubSecKeyOptions()//
              .setAlgorithm("HS256")//
              .setPublicKey(cfg.getString("JWT_SECRET"))//
              .setSymmetric(true)));

      vertx.deployVerticle(new JwtVerticle(router, vertx.eventBus(), jwtAuth, cfg));
      vertx.deployVerticle(new UserVerticle(router, mongoClient, jwtAuth));

      HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
      setupHealthCheck(healthCheckHandler, mongoClient, mongoConfig);
      router.get("/ping").handler(healthCheckHandler);

      int port = cfg.getInteger("APP_PORT");
      String host = cfg.getString("APP_HOST", "localhost");
      vertx.createHttpServer().requestHandler(router).listen(port, host, http -> {
        if (http.succeeded()) {
          logger.info("HTTP server started on {}:{}", host, port);
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

  void setupHealthCheck(HealthCheckHandler healthCheckHandler, MongoClient mongoClient, JsonObject mongoConfig) {
    healthCheckHandler.register("check-mongo-connection", promise -> {
      JsonObject data = new JsonObject()//
          .put("host", mongoConfig.getString("host"))//
          .put("port", mongoConfig.getInteger("port"))//
          .put("db_name", mongoConfig.getString("db_name"));
      mongoClient.rxGetCollections().subscribe(resultList -> {
        if (!resultList.isEmpty()) {
          promise.complete(Status.OK(data));
        } else {
          promise.complete(Status.KO(data//
              .put("error", "Collection list is empty!")));
        }
      }, e -> {
        promise.complete(Status.KO(data//
            .put("error", e.getMessage())));
      });
    });
  }
}
