package com.anasdidi.security.api.jwt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;

public class JwtVerticle extends AbstractVerticle {

  private final Logger logger = LogManager.getLogger(JwtController.class);
  private final Router mainRouter;
  private final JwtController jwtController;

  public JwtVerticle(Router mainRouter) {
    this.mainRouter = mainRouter;
    this.jwtController = new JwtController();
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);
    router.post("/validate").handler(jwtController::validate);

    mainRouter.mountSubRouter("/jwt", router);

    logger.info("[start] Deployed success");
    startPromise.complete();

  }
}
