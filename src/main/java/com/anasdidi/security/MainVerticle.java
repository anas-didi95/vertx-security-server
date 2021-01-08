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

  private Logger logger = LogManager.getLogger(MainVerticle.class);
  private final boolean isTest;

  public MainVerticle() {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        Log4j2LogDelegateFactory.class.getName());
    this.isTest = false;
  }

  public MainVerticle(boolean isTest) {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        Log4j2LogDelegateFactory.class.getName());
    this.isTest = isTest;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    final String TAG = "start";
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

    configRetriever.rxGetConfig().subscribe(config -> {
      AppConfig appConfig = AppConfig.create(config.put("IS_TEST", isTest));
      logger.info("[{}] appConfig\n{}", TAG, appConfig.toString());

      MongoClient mongoClient = MongoClient.createShared(vertx, appConfig.getMongoConfig());//

      JWTAuth jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()//
          .setJWTOptions(new JWTOptions()//
              .setExpiresInMinutes(appConfig.getJwtExpireInMinutes())//
              .setIssuer(appConfig.getJwtIssuer()))
          .addPubSecKey(new PubSecKeyOptions()//
              .setAlgorithm("HS256")//
              .setBuffer(appConfig.getJwtSecret())));

      Router router = Router.router(vertx);
      router.route().handler(setupCorsHandler());
      router.route().handler(BodyHandler.create());
      router.route().handler(this::generateRequestId);
      router.get("/ping").handler(setupHealthCheck(mongoClient));

      vertx.deployVerticle(new JwtVerticle(router, vertx.eventBus(), jwtAuth, mongoClient));
      vertx.deployVerticle(new UserVerticle(router, mongoClient, jwtAuth, vertx.eventBus()));
      vertx.deployVerticle(new GraphqlVerticle(router, vertx.eventBus(), jwtAuth));

      int port = appConfig.getAppPort();
      String host = appConfig.getAppHost();
      Router contextPath = Router.router(vertx);
      contextPath.mountSubRouter(CommonConstants.CONTEXT_PATH, router);
      vertx.createHttpServer().requestHandler(contextPath).listen(port, host, http -> {
        if (http.succeeded()) {
          logger.info("[{}] HTTP server started on {}:{}", TAG, host, port);
          startPromise.complete();
        } else {
          startPromise.fail(http.cause());
        }
      });
    }, e -> startPromise.fail(e));
  }

  void generateRequestId(RoutingContext routingContext) {
    routingContext.put("requestId", CommonUtils.generateUUID());
    routingContext.put("startTime", System.currentTimeMillis());
    routingContext.next();
  }

  HealthCheckHandler setupHealthCheck(MongoClient mongoClient) {
    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

    healthCheckHandler.register("check-mongo-connection", promise -> {
      mongoClient.rxGetCollections().subscribe(resultList -> {
        if (!resultList.isEmpty()) {
          promise.complete(Status.OK(new JsonObject().put("totalCollection", resultList.size())));
        } else {
          promise.complete(Status.KO(new JsonObject().put("error", "Collection list is empty!")));
        }
      }, e -> {
        promise.complete(Status.KO(new JsonObject().put("error", e.getMessage())));
      });
    });

    return healthCheckHandler;
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
