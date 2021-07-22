package com.anasdidi.security.domain.graphql;

import com.anasdidi.security.common.ApplicationConstants;
import com.anasdidi.security.common.BaseVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;
import io.vertx.rxjava3.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.rxjava3.ext.web.handler.graphql.GraphiQLHandler;

public class GraphiqlVerticle extends BaseVerticle {

  private static final Logger logger = LogManager.getLogger(GraphiqlVerticle.class);

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    super.start(startFuture);

    logger.info("[start] Verticle started");
    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return GraphqlConstants.CONTEXT_PATH_GRAPHIQL;
  }

  @Override
  protected String getPermission() {
    return null;
  }

  @Override
  protected void setHandler(Router router, EventBus eventBus, JWTAuthHandler jwtAuthHandler,
      Handler<RoutingContext> jwtAuthzHandler) {
    router.post("/graphql").handler(GraphQLHandler.create(GraphqlUtils.createGraphQL(vertx)));
    router.get("/*").handler(GraphiQLHandler.create(new GraphiQLHandlerOptions().setEnabled(true)
        .setGraphQLUri(ApplicationConstants.CONTEXT_PATH + "/graphiql/graphql")));
  }
}
