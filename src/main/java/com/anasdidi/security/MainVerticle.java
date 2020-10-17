package com.anasdidi.security;

import java.util.HashSet;
import java.util.Set;

import com.anasdidi.security.api.graphql.GraphqlVerticle;
import com.anasdidi.security.api.jwt.JwtVerticle;
import com.anasdidi.security.api.user.UserVerticle;
import com.anasdidi.security.common.AppConfig;
import com.anasdidi.security.common.CommonConstants;
import com.anasdidi.security.common.CommonUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
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
import io.vertx.reactivex.ext.web.handler.CorsHandler;

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
    String tag = "start";
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

    configRetriever.rxGetConfig().subscribe(config -> {
      AppConfig appConfig = AppConfig.create(config);
      logger.info("[{}] appConfig\n{}", tag, appConfig.toString());

      JsonObject mongoConfig = new JsonObject()
          .put("host", isTest ? appConfig.getTestMongoHost() : appConfig.getMongoHost())//
          .put("port", isTest ? appConfig.getTestMongoPort() : appConfig.getMongoPort())//
          .put("username", isTest ? appConfig.getTestMongoUsename() : appConfig.getMongoUsername())//
          .put("password", isTest ? appConfig.getTestMongoPassword() : appConfig.getMongoPassword())//
          .put("authSource", isTest ? appConfig.getTestMongoAuthSource() : appConfig.getMongoAuthSource())//
          .put("db_name", "security");
      MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfig);//

      @SuppressWarnings("deprecation")
      JWTAuth jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()//
          .setJWTOptions(new JWTOptions()//
              .setExpiresInMinutes(appConfig.getJwtExpireInMinutes())//
              .setIssuer(appConfig.getJwtIssuer()))
          .addPubSecKey(new PubSecKeyOptions()//
              .setAlgorithm("HS256")//
              .setPublicKey(appConfig.getJwtSecret())//
              .setSymmetric(true)));

      HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
      setupHealthCheck(healthCheckHandler, mongoClient, mongoConfig);

      Router router = Router.router(vertx);
      router.route().handler(setupCorsHandler());
      router.route().handler(BodyHandler.create());
      router.route().handler(this::generateRequestId);
      router.get("/ping").handler(healthCheckHandler);

      vertx.deployVerticle(new JwtVerticle(router, vertx.eventBus(), jwtAuth, mongoClient));
      vertx.deployVerticle(new UserVerticle(router, mongoClient, jwtAuth, vertx.eventBus()));
      vertx.deployVerticle(new GraphqlVerticle(router, vertx.eventBus(), jwtAuth));

      int port = appConfig.getAppPort();
      String host = appConfig.getAppHost();
      Router contextPath = Router.router(vertx).mountSubRouter(CommonConstants.CONTEXT_PATH, router);
      vertx.createHttpServer().requestHandler(contextPath).listen(port, host, http -> {
        if (http.succeeded()) {
          logger.info("[{}] HTTP server started on {}:{}", tag, host, port);
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

  CorsHandler setupCorsHandler() {
    Set<String> headerNames = new HashSet<>();
    headerNames.add("Accept");
    headerNames.add("Content-Type");
    headerNames.add("Authorization");

    Set<HttpMethod> methods = new HashSet<>();
    methods.add(HttpMethod.GET);
    methods.add(HttpMethod.POST);
    methods.add(HttpMethod.PUT);
    methods.add(HttpMethod.DELETE);

    return CorsHandler.create("*")//
        .allowedHeaders(headerNames)//
        .allowedMethods(methods);
  }
}
