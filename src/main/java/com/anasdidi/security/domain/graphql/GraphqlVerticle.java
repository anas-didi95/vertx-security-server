package com.anasdidi.security.domain.graphql;

import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;
import io.vertx.rxjava3.ext.web.handler.graphql.GraphQLHandler;

public class GraphqlVerticle extends BaseVerticle {

  private static final Logger logger = LogManager.getLogger(GraphqlVerticle.class);
  private final GraphqlHandler graphqlHandler;

  public GraphqlVerticle() {
    this.graphqlHandler = new GraphqlHandler();
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    super.start(startFuture);

    logger.info("[start] Verticle started");
    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return GraphqlConstants.CONTEXT_PATH;
  }

  @Override
  protected String getPermission() {
    return null;
  }

  @Override
  protected void setHandler(Router router, EventBus eventBus, JWTAuthHandler jwtAuthHandler,
      Handler<RoutingContext> jwtAuthzHandler) {
    router.route().handler(jwtAuthHandler).failureHandler(graphqlHandler::sendResponseFailure);
    router.route().handler(jwtAuthzHandler).failureHandler(graphqlHandler::sendResponseFailure);
    router.post("/").handler(GraphQLHandler.create(GraphqlUtils.createGraphQL(vertx)));
  }
}
