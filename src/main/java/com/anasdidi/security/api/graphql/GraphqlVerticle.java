package com.anasdidi.security.api.graphql;

import com.anasdidi.security.common.AppConfig;
import com.anasdidi.security.common.CommonConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

public class GraphqlVerticle extends AbstractVerticle {

  private final Logger logger = LogManager.getLogger(GraphqlVerticle.class);
  private final Router mainRouter;
  private final JWTAuth jwtAuth;
  private final GraphqlDataFetcher dataFetcher;

  public GraphqlVerticle(Router mainRouter, EventBus eventBus, JWTAuth jwtAuth) {
    this.mainRouter = mainRouter;
    this.jwtAuth = jwtAuth;
    this.dataFetcher = new GraphqlDataFetcher(eventBus);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    AppConfig appConfig = AppConfig.instance();
    Router router = Router.router(vertx);
    router.route().handler(JWTAuthHandler.create(jwtAuth));
    router.post("/").handler(GraphQLHandler.create(createGraphQL()));
    mainRouter.mountSubRouter(GraphqlConstants.REQUEST_URI, router);

    if (appConfig.getGraphiqlIsEnable()) {
      Router router1 = Router.router(vertx);
      router1.post(GraphqlConstants.REQUEST_URI).handler(GraphQLHandler.create(createGraphQL()));
      router1.get("/*").handler(GraphiQLHandler.create(new GraphiQLHandlerOptions()//
          .setGraphQLUri(CommonConstants.CONTEXT_PATH + GraphqlConstants.REQUEST_URI_GRAPHIQL
              + GraphqlConstants.REQUEST_URI)
          .setEnabled(appConfig.getGraphiqlIsEnable())));
      mainRouter.mountSubRouter("/graphiql", router1);
    }

    logger.info("[start] Deployed success");
    startPromise.complete();
  }

  private GraphQL createGraphQL() {
    String schema = vertx.fileSystem().readFileBlocking("schema.graphql").toString();
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()//
        .type("Query", builder -> builder//
            .dataFetcher("ping", VertxDataFetcher.create(dataFetcher::ping))
            .dataFetcher("getUserList", VertxDataFetcher.create(dataFetcher::getUserList))//
            .dataFetcher("getUserById", VertxDataFetcher.create(dataFetcher::getUserById))//
            .dataFetcher("getUserByUsername",
                VertxDataFetcher.create(dataFetcher::getUserByUsername)))//
        .type("User", builder -> builder//
            .dataFetcher("lastModifiedBy", VertxDataFetcher.create(dataFetcher::getLastModifiedBy)))
        .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema =
        schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    return GraphQL.newGraphQL(graphQLSchema).build();
  }
}
