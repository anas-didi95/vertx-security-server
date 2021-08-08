package com.anasdidi.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.anasdidi.security.common.ApplicationConfig;
import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.ApplicationUtils;
import com.anasdidi.security.common.BaseVerticle;
import com.anasdidi.security.domain.auth.AuthVerticle;
import com.anasdidi.security.domain.graphql.GraphiqlVerticle;
import com.anasdidi.security.domain.graphql.GraphqlVerticle;
import com.anasdidi.security.domain.mongo.MongoVerticle;
import com.anasdidi.security.domain.user.UserVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.ext.healthchecks.Status;
import io.vertx.rxjava3.config.ConfigRetriever;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.shareddata.LocalMap;
import io.vertx.rxjava3.ext.healthchecks.HealthCheckHandler;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;
import io.vertx.rxjava3.ext.web.handler.CorsHandler;

public class MainVerticle extends AbstractVerticle {

  private final static Logger logger = LogManager.getLogger(MainVerticle.class);

  public MainVerticle() {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        Log4j2LogDelegateFactory.class.getName());
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    getConfigRetriever().rxGetConfig().subscribe(json -> {
      ApplicationConfig config = ApplicationConfig.create(json);
      logger.info("[start] Load {}", config);

      Router router = getRouter();
      List<Single<String>> deployer = deployVerticles(router, new MongoVerticle(),
          new UserVerticle(), new AuthVerticle(), new GraphqlVerticle());

      if (config.getGraphiqlEnable()) {
        deployer.add(deployVerticle(router, new GraphiqlVerticle()));
      }

      Single.mergeDelayError(deployer).toList().subscribe(verticleList -> {
        logger.info("[start] Total deployed verticle: {}", verticleList.size());

        LocalMap<Object, Object> localMap = vertx.sharedData().getLocalMap("serverStatus");
        localMap.put("verticles", verticleList.size());
        localMap.put("startTime", System.currentTimeMillis());

        Router contextPath = Router.router(vertx);
        contextPath.mountSubRouter(ApplicationConstants.CONTEXT_PATH, router);
        contextPath.mountSubRouter(ApplicationConstants.CONTEXT_PATH, getHealthRouter());
        logger.info("[start] Set context path: {}", ApplicationConstants.CONTEXT_PATH);

        vertx.createHttpServer().requestHandler(contextPath)
            .listen(config.getAppPort(), config.getAppHost()).subscribe(server -> {
              logger.info("[start] HTTP server started on {}:{}", config.getAppHost(),
                  server.actualPort());
              startFuture.complete();
            }, error -> startFuture.fail(error));
      }, error -> startFuture.fail(error));
    });
  }

  private ConfigRetriever getConfigRetriever() {
    List<ConfigStoreOptions> storeList = new ArrayList<>();
    storeList.add(new ConfigStoreOptions().setType("env")
        .setConfig(new JsonObject().put("keys", ApplicationConfig.getKeyList())));

    return ConfigRetriever.create(vertx, new ConfigRetrieverOptions().setStores(storeList));
  }

  private CorsHandler getCorsHandler() {
    ApplicationConfig config = ApplicationConfig.instance();
    CorsHandler handler = CorsHandler.create();

    Arrays.asList(config.getCorsOrigins().split(",")).forEach(handler::addOrigin);
    Arrays.asList("Accept", "Content-Type", "Authorization").forEach(handler::allowedHeader);
    Arrays.asList(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
        .forEach(handler::allowedMethod);

    return handler;
  }

  private Router getRouter() {
    Router router = Router.router(vertx);
    router.route().handler(getCorsHandler());
    router.route().handler(BodyHandler.create());
    router.route().handler(
        routingContext -> routingContext.put("traceId", ApplicationUtils.getFormattedUUID())
            .put("startTime", System.currentTimeMillis()).next());

    return router;
  }

  private Router getHealthRouter() {
    Router router = Router.router(vertx);
    HealthCheckHandler handler = HealthCheckHandler.create(vertx);

    handler.register("server-status", promise -> {
      LocalMap<Object, Object> localMap = vertx.sharedData().getLocalMap("serverStatus");
      int verticles = (int) localMap.get("verticles");
      long uptime = System.currentTimeMillis() - (long) localMap.get("startTime");
      String uptimeTemplate = "%dd %dh %dm %ds %dms";

      long minsec = uptime % 1000;
      uptime /= 1000;
      long sec = uptime % 60;
      uptime /= 60;
      long min = uptime % 60;
      uptime /= 60;
      long hour = uptime % 24;
      uptime /= 24;

      JsonObject responseBody = new JsonObject().put("verticles", verticles).put("uptime",
          String.format(uptimeTemplate, uptime, hour, min, sec, minsec));
      promise.complete(Status.OK(responseBody));
    });

    router.get("/health").handler(handler);
    return router;
  }

  private List<Single<String>> deployVerticles(Router router, BaseVerticle... verticles) {
    return Arrays.stream(verticles).map(verticle -> deployVerticle(router, verticle))
        .collect(Collectors.toList());
  }

  private Single<String> deployVerticle(Router router, BaseVerticle verticle) {
    return vertx.rxDeployVerticle(verticle).doOnSuccess(id -> {
      logger.info("[deployVerticle] {} OK: {}", verticle.getClass().getName(), id);

      if (verticle.hasRouter()) {
        router.mountSubRouter(verticle.getContextPath(), verticle.getRouter());
        logger.info("[deployVerticle] {} Mount router: {}", verticle.getClass().getName(),
            verticle.getContextPath());
      }
    }).doOnError(error -> logger.error("[deployVerticle {} FAILED! {}",
        verticle.getClass().getName(), error));
  }
}
