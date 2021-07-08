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
import com.anasdidi.security.domain.mongo.MongoVerticle;
import com.anasdidi.security.domain.user.UserVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.rxjava3.config.ConfigRetriever;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;

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
      List<Single<String>> deployer =
          deployVerticles(router, new MongoVerticle(), new UserVerticle(), new AuthVerticle());

      Single.mergeDelayError(deployer).toList().subscribe(verticleList -> {
        logger.info("[start] Total deployed verticle: {}", verticleList.size());

        Router contextPath = Router.router(vertx);
        contextPath.mountSubRouter(ApplicationConstants.CONTEXT_PATH, router);
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

  private Router getRouter() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route().handler(
        routingContext -> routingContext.put("traceId", ApplicationUtils.getFormattedUUID())
            .put("startTime", System.currentTimeMillis()).next());

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
